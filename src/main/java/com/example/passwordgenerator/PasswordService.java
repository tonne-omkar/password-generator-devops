package com.example.passwordgenerator;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class PasswordService {

    private static final String LOWERCASE =
            "abcdefghijklmnopqrstuvwxyz";

    private static final String UPPERCASE =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String NUMBERS =
            "0123456789";

    private static final String SYMBOLS =
            "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private static final String ALL_CHARACTERS =
            LOWERCASE + UPPERCASE + NUMBERS + SYMBOLS;

    private final SecureRandom random = new SecureRandom();

    public String generatePassword(int length) {

        if (length < 8 || length > 64) {
            throw new IllegalArgumentException(
                    "Password length must be between 8 and 64"
            );
        }

        StringBuilder password = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALL_CHARACTERS.length());
            password.append(ALL_CHARACTERS.charAt(index));
        }

        return password.toString();
    }
}
