package com.example.passwordgenerator;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService(TestProperties.DEFAULTS);

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

    @ParameterizedTest
    @ValueSource(ints = {8, 12, 20, 64})
    void acceptsEveryLengthInsideTheLimits(int length) {
        assertEquals(length, passwordService.generatePassword(length).length());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 7, 65, 1000})
    void rejectsLengthsOutsideTheLimits(int length) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> passwordService.generatePassword(length));
        assertEquals("Password length must be between 8 and 64", e.getMessage());
    }

    /** Repeated because the guarantee must hold for every random draw, not just one. */
    @RepeatedTest(50)
    void containsAtLeastOneCharacterOfEverySelectedType() {
        String password = passwordService.generatePassword(8);

        assertTrue(password.chars().anyMatch(Character::isLowerCase), password);
        assertTrue(password.chars().anyMatch(Character::isUpperCase), password);
        assertTrue(password.chars().anyMatch(Character::isDigit), password);
        assertTrue(password.chars().anyMatch(c -> PasswordService.SYMBOLS.indexOf(c) >= 0), password);
    }

    @Test
    void digitsOnlyProducesOnlyDigits() {
        String pin = passwordService.generate(new PasswordOptions(12, false, false, true, false, false));

        assertTrue(pin.chars().allMatch(Character::isDigit), pin);
    }

    @Test
    void lettersOnlyHasNoDigitsOrSymbols() {
        String password = passwordService.generate(new PasswordOptions(40, true, true, false, false, false));

        assertTrue(password.chars().allMatch(Character::isLetter), password);
    }

    @RepeatedTest(20)
    void excludeAmbiguousNeverUsesLookAlikeCharacters() {
        String password = passwordService.generate(new PasswordOptions(64, true, true, true, true, true));

        for (char c : PasswordService.AMBIGUOUS.toCharArray()) {
            assertFalse(password.indexOf(c) >= 0, "found '" + c + "' in " + password);
        }
    }

    @Test
    void noCharacterTypeSelectedIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> passwordService.generate(new PasswordOptions(16, false, false, false, false, false)));
        assertEquals("Select at least one character type", e.getMessage());
    }

    @Test
    void poolSizeCountsSelectedCharacters() {
        assertEquals(26 + 26 + 10 + PasswordService.SYMBOLS.length(),
                passwordService.poolSize(PasswordOptions.allOf(16)));
        assertEquals(10, passwordService.poolSize(new PasswordOptions(16, false, false, true, false, false)));
        // 0, 1 removed from digits
        assertEquals(8, passwordService.poolSize(new PasswordOptions(16, false, false, true, false, true)));
    }

    @Test
    void passwordsAreNotRepeated() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(passwordService.generatePassword(16));
        }

        assertEquals(100, seen.size(), "two identical 16-char passwords");
    }
}
