package com.example.passwordgenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrengthServiceTest {

    private final StrengthService strengthService = new StrengthService();

    @Test
    void entropyIsLengthTimesLog2OfPool() {
        // 16 characters from 64 possibilities = 16 * 6 bits
        assertEquals(96.0, StrengthService.entropy(16, 64), 1e-9);
        assertEquals(0.0, StrengthService.entropy(10, 1), 1e-9);
    }

    @Test
    void commonPasswordIsVeryWeak() {
        StrengthService.Strength s = strengthService.evaluate("Password");

        assertEquals(0, s.score());
        assertEquals("very weak", s.label());
        assertTrue(s.warnings().contains("This is one of the most common passwords"));
    }

    @Test
    void longMixedPasswordIsVeryStrong() {
        StrengthService.Strength s = strengthService.evaluate("q7#Vt2!mZp9@Lw4$");

        assertEquals(4, s.score());
        assertEquals("very strong", s.label());
        assertTrue(s.warnings().isEmpty(), s.warnings().toString());
    }

    @Test
    void shortLettersOnlyGetsBothWarnings() {
        StrengthService.Strength s = strengthService.evaluate("sunshine");

        assertTrue(s.warnings().contains("Use at least 12 characters"));
        assertTrue(s.warnings().contains("Add digits or symbols"));
    }

    @Test
    void tripleRepeatIsPenalised() {
        StrengthService.Strength repeated = strengthService.evaluate("abcd1111efgh");
        StrengthService.Strength plain = strengthService.evaluate("abcd1234efgh");

        assertTrue(repeated.entropyBits() < plain.entropyBits());
        assertTrue(repeated.warnings().contains("Avoid repeating the same character three times"));
    }

    @Test
    void emptyPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> strengthService.evaluate(""));
        assertThrows(IllegalArgumentException.class, () -> strengthService.evaluate(null));
    }

    @ParameterizedTest
    @CsvSource({"0,0,very weak", "27.9,0,very weak", "28,1,weak", "36,2,fair", "60,3,strong", "80,4,very strong"})
    void scoreBoundaries(double bits, int score, String label) {
        assertEquals(score, StrengthService.score(bits));
        assertEquals(label, StrengthService.label(score));
    }

    @Test
    void poolSizeDetectsCharacterTypes() {
        assertEquals(26, StrengthService.poolSize("abc"));
        assertEquals(52, StrengthService.poolSize("aB"));
        assertEquals(62, StrengthService.poolSize("aB3"));
        assertEquals(62 + PasswordService.SYMBOLS.length(), StrengthService.poolSize("aB3!"));
    }
}
