package com.example.passwordgenerator;

/** Which character classes a generated password may use. */
public record PasswordOptions(
        int length,
        boolean upper,
        boolean lower,
        boolean digits,
        boolean symbols,
        boolean excludeAmbiguous) {

    /** Every class enabled, nothing excluded. */
    public static PasswordOptions allOf(int length) {
        return new PasswordOptions(length, true, true, true, true, false);
    }
}
