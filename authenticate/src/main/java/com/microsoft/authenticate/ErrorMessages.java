package com.microsoft.authenticate;

/**
 * ErrorMessages is a non-instantiable class that contains all the String constants
 * used in for errors and exceptions.
 */
final class ErrorMessages {
    public static final String CLIENT_ERROR =
            "An error occurred on the client during the operation.";
    public static final String LOGIN_IN_PROGRESS =
            "Another login operation is already in progress.";
    public static final String NON_INSTANTIABLE_CLASS = "Non-instantiable class";
    public static final String SERVER_ERROR =
            "An error occurred while communicating with the server during the operation. "
            + "Please try again later.";
    public static final String SIGNIN_CANCEL = "The user cancelled the login operation.";

    private ErrorMessages() {
        throw new AssertionError(NON_INSTANTIABLE_CLASS);
    }
}
