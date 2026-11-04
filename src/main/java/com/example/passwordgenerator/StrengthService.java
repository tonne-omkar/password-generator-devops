package com.example.passwordgenerator;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Estimates password strength from entropy: length x log2(size of the character pool),
 * with penalties for patterns that make a password easy to guess.
 */
@Service
public class StrengthService {

    private static final Set<String> COMMON = Set.of(
            "password", "123456", "12345678", "qwerty", "abc123", "111111",
            "letmein", "welcome", "admin", "iloveyou", "monkey", "dragon",
            "football", "password1", "123456789", "qwerty123", "1q2w3e4r");

    public record Strength(double entropyBits, int score, String label, List<String> warnings) {
    }

    public Strength evaluate(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }

        List<String> warnings = new ArrayList<>();
        double entropy = entropy(password.length(), poolSize(password));

        if (COMMON.contains(password.toLowerCase(Locale.ROOT))) {
            warnings.add("This is one of the most common passwords");
            entropy = Math.min(entropy, 10);
        }
        if (password.length() < 12) {
            warnings.add("Use at least 12 characters");
        }
        if (password.matches(".*(.)\\1\\1.*")) {
            warnings.add("Avoid repeating the same character three times");
            entropy *= 0.8;
        }
        if (password.chars().allMatch(Character::isLetter)) {
            warnings.add("Add digits or symbols");
        }

        double rounded = Math.round(entropy * 10) / 10.0;
        int score = score(rounded);
        return new Strength(rounded, score, label(score), warnings);
    }

    /** Entropy in bits for a password of this length drawn uniformly from a pool. */
    public static double entropy(int length, int poolSize) {
        if (poolSize <= 1) {
            return 0;
        }
        return length * (Math.log(poolSize) / Math.log(2));
    }

    static int poolSize(String password) {
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean other = false;
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else {
                other = true;
            }
        }
        return (lower ? 26 : 0) + (upper ? 26 : 0) + (digit ? 10 : 0)
                + (other ? PasswordService.SYMBOLS.length() : 0);
    }

    static int score(double entropyBits) {
        if (entropyBits < 28) return 0;
        if (entropyBits < 36) return 1;
        if (entropyBits < 60) return 2;
        if (entropyBits < 80) return 3;
        return 4;
    }

    static String label(int score) {
        return switch (score) {
            case 0 -> "very weak";
            case 1 -> "weak";
            case 2 -> "fair";
            case 3 -> "strong";
            default -> "very strong";
        };
    }
}
