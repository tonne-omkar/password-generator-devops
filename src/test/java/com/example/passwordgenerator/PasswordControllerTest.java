package com.example.passwordgenerator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Starts the whole application and calls it over (mock) HTTP, the way a client would. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability // tests switch metric exporters off by default; /actuator/prometheus needs one
class PasswordControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void generateUsesDefaultLength16() throws Exception {
        mvc.perform(get("/api/password/generate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.password", hasLength(16)))
                .andExpect(jsonPath("$.length").value(16))
                .andExpect(jsonPath("$.entropyBits").value(103.4))
                .andExpect(jsonPath("$.strength").value("very strong"));
    }

    @Test
    void generateHonoursOptions() throws Exception {
        mvc.perform(get("/api/password/generate")
                        .param("length", "10")
                        .param("upper", "false")
                        .param("lower", "false")
                        .param("symbols", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password", matchesPattern("[0-9]{10}")))
                .andExpect(jsonPath("$.entropyBits").value(33.2))
                .andExpect(jsonPath("$.strength").value("weak"));
    }

    @Test
    void lengthOutsideLimitsIs400() throws Exception {
        mvc.perform(get("/api/password/generate").param("length", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password length must be between 8 and 64"));
    }

    @Test
    void nonNumericLengthIs400() throws Exception {
        mvc.perform(get("/api/password/generate").param("length", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for 'length'"));
    }

    @Test
    void noCharacterTypeIs400() throws Exception {
        mvc.perform(get("/api/password/generate")
                        .param("upper", "false").param("lower", "false")
                        .param("digits", "false").param("symbols", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Select at least one character type"));
    }

    @Test
    void strengthChecksPostedPassword() throws Exception {
        mvc.perform(post("/api/password/strength")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"qwerty\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("very weak"))
                .andExpect(jsonPath("$.warnings", hasSize(3)));
    }

    @Test
    void strengthWithBlankPasswordIs400() throws Exception {
        mvc.perform(post("/api/password/strength")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must not be empty"));
    }

    @Test
    void strengthWithBrokenJsonIs400() throws Exception {
        mvc.perform(post("/api/password/strength")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointStillWorks() throws Exception {
        mvc.perform(get("/api/password/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Password Generator API"));
    }

    @Test
    void actuatorProbesAreUp() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void prometheusMetricsAreExposed() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }
}
