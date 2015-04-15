package com.microsoft.authenticate;

/**
 * Indicates that an exception occurred during the Auth process.
 */
public class AuthException extends Exception {
    private final String error;
    private final String errorUri;


    AuthException(String errorMessage) {
        super(errorMessage);
        this.error = "";
        this.errorUri = "";
    }

    AuthException(String errorMessage, Throwable throwable) {
        super(errorMessage, throwable);
        this.error = "";
        this.errorUri = "";
    }

    AuthException(String error, String errorDescription, String errorUri) {
        super(errorDescription);

        this.error = error;
        this.errorUri = errorUri;
    }

    AuthException(String error, String errorDescription, String errorUri, Throwable cause) {
        super(errorDescription, cause);

        this.error = error;
        this.errorUri = errorUri;
    }

    /**
     * @return Returns the authentication error.
     */
    public String getError() {
        return this.error;
    }

    /**
     * @return Returns the error URI.
     */
    public String getErrorUri() {
        return this.errorUri;
    }
}