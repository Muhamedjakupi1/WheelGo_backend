package com.wheelGo.validation;

public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
    public static final String MESSAGE =
            "Password must be at least 8 chars, include 1 uppercase and 1 number";

    private PasswordPolicy() {
    }

    public static boolean isValid(String value) {
        return value != null && value.matches(REGEX);
    }
}
