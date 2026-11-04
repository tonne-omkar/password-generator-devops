package com.example.passwordgenerator;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordService {

    static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final String NUMBERS = "0123456789";
    static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    /** Characters that are easy to misread: O/0/o, I/l/1, |. */
    static final String AMBIGUOUS = "O0oIl1|";

    private final SecureRandom random = new SecureRandom();
    private final PasswordProperties properties;

    public PasswordService(PasswordProperties properties) {
        this.properties = properties;
    }

    /** Kept from the first version of the API: every character class, no exclusions. */
    public String generatePassword(int length) {
        return generate(PasswordOptions.allOf(length));
    }

    /**
     * Generates a password that contains at least one character from every selected class,
     * then shuffles it so those guaranteed characters are not always at the start.
     */
    public String generate(PasswordOptions options) {
        validateLength(options.length());

        List<String> classes = selectedClasses(options);
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("Select at least one character type");
        }

        String pool = String.join("", classes);
        char[] password = new char[options.length()];

        for (int i = 0; i < classes.size(); i++) {
            password[i] = pick(classes.get(i));
        }
        for (int i = classes.size(); i < password.length; i++) {
            password[i] = pick(pool);
        }
        shuffle(password);
        return new String(password);
    }

    /** Number of distinct characters the selected options can produce. */
    public int poolSize(PasswordOptions options) {
        return selectedClasses(options).stream().mapToInt(String::length).sum();
    }

    private void validateLength(int length) {
        if (length < properties.minLength() || length > properties.maxLength()) {
            throw new IllegalArgumentException(
                    "Password length must be between " + properties.minLength()
                            + " and " + properties.maxLength());
        }
    }

    private List<String> selectedClasses(PasswordOptions options) {
        List<String> classes = new ArrayList<>(4);
        if (options.lower()) classes.add(LOWERCASE);
        if (options.upper()) classes.add(UPPERCASE);
        if (options.digits()) classes.add(NUMBERS);
        if (options.symbols()) classes.add(SYMBOLS);
        if (options.excludeAmbiguous()) {
            classes.replaceAll(PasswordService::withoutAmbiguous);
        }
        return classes;
    }

    private static String withoutAmbiguous(String characters) {
        StringBuilder kept = new StringBuilder(characters.length());
        for (char c : characters.toCharArray()) {
            if (AMBIGUOUS.indexOf(c) < 0) {
                kept.append(c);
            }
        }
        return kept.toString();
    }

    private char pick(String characters) {
        return characters.charAt(random.nextInt(characters.length()));
    }

    /** Fisher-Yates shuffle with SecureRandom. */
    private void shuffle(char[] chars) {
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
    }
}
