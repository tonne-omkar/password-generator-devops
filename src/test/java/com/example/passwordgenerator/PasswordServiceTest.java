package com.example.passwordgenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordServiceTest {

    private final PasswordService passwordService =
            new PasswordService();

    @Test
    void passwordShouldHaveRequestedLength() {
        String password = passwordService.generatePassword(16);

        assertEquals(16, password.length());
    }

    @Test
    void passwordShouldHaveRequestedLength32() {
        String password = passwordService.generatePassword(32);

        assertEquals(32, password.length());
    }

    @Test
    void invalidLengthShouldThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> passwordService.generatePassword(7)
        );
    }
}
