package com.microsoft.onedriveaccess;

/**
 * An interface for credentials objects that interact with the OneDrive service
 */
public interface IODCredential {

    /**
     * Gets the access token for these credentials
     * @return The token
     */
    String getAccessToken();

}