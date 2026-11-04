package com.example.passwordgenerator;

/** The same limits as application.properties, for unit tests that do not start Spring. */
final class TestProperties {

    static final PasswordProperties DEFAULTS = new PasswordProperties(8, 64);

    private TestProperties() {
    }
}
