package com.example.passwordgenerator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordService passwordService;
    private final StrengthService strengthService;

    public PasswordController(PasswordService passwordService, StrengthService strengthService) {
        this.passwordService = passwordService;
        this.strengthService = strengthService;
    }

    public record StrengthRequest(@NotBlank String password) {
    }

    /** GET /api/password/generate?length=16&upper=true&lower=true&digits=true&symbols=true&excludeAmbiguous=false */
    @GetMapping("/generate")
    public Map<String, Object> generatePassword(
            @RequestParam(defaultValue = "16") int length,
            @RequestParam(defaultValue = "true") boolean upper,
            @RequestParam(defaultValue = "true") boolean lower,
            @RequestParam(defaultValue = "true") boolean digits,
            @RequestParam(defaultValue = "true") boolean symbols,
            @RequestParam(defaultValue = "false") boolean excludeAmbiguous) {

        PasswordOptions options = new PasswordOptions(length, upper, lower, digits, symbols, excludeAmbiguous);
        String password = passwordService.generate(options);
        double entropy = Math.round(StrengthService.entropy(length, passwordService.poolSize(options)) * 10) / 10.0;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("password", password);
        body.put("length", password.length());
        body.put("entropyBits", entropy);
        body.put("strength", StrengthService.label(StrengthService.score(entropy)));
        return body;
    }

    /** POST /api/password/strength with body {"password": "..."} */
    @PostMapping("/strength")
    public StrengthService.Strength strength(@Valid @RequestBody StrengthRequest request) {
        return strengthService.evaluate(request.password());
    }
}
