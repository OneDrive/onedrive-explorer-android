package com.microsoft.authenticate;

/**
 * Handles callback methods for LiveAuthClient init, login, and logout methods.
 * Returns the * status of the operation when onAuthComplete is called. If there was an error
 * during the operation, onAuthError is called with the exception that was thrown.
 */
public interface AuthListener {
    /**
     * Invoked when the operation completes successfully.
     *
     * @param status The {@link AuthStatus} for an operation. If successful, the status is
     *               CONNECTED. If unsuccessful, NOT_CONNECTED or UNKNOWN are returned.
     * @param session The {@link AuthSession} from the {@link AuthClient}.
     * @param userState An arbitrary object that is used to determine the caller of the method.
     */
    public void onAuthComplete(AuthStatus status, AuthSession session, Object userState);

    /**
     * Invoked when the method call fails.
     *
     * @param exception The {@link AuthException} error.
     * @param userState An arbitrary object that is used to determine the caller of the method.
     */
    public void onAuthError(AuthException exception, Object userState);
}
