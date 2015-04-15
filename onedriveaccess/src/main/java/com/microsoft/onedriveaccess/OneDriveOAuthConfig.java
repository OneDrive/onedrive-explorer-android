package com.microsoft.onedriveaccess;

import android.net.Uri;

import com.microsoft.authenticate.OAuthConfig;

/**
 * OAuth configuration for the OneDrive service
 */
public class OneDriveOAuthConfig implements OAuthConfig {

    /**
     * The domain to authenticate against
     */
    public static final String HTTPS_LOGIN_LIVE_COM = "https://login.live.com/";

    /**
     * Singleton instance
     */
    private static OneDriveOAuthConfig sInstance;

    /**
     * The authorization uri
     */
    private Uri mOAuthAuthorizeUri;

    /**
     * The desktop uri
     */
    private Uri mOAuthDesktopUri;

    /**
     * The logout uri
     */
    private Uri mOAuthLogoutUri;

    /**
     * The auth token uri
     */
    private Uri mOAuthTokenUri;

    /**
     * Default Constructor
     */
    public OneDriveOAuthConfig() {
        mOAuthAuthorizeUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "oauth20_authorize.srf");
        mOAuthDesktopUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "oauth20_desktop.srf");
        mOAuthLogoutUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "oauth20_logout.srf");
        mOAuthTokenUri = Uri.parse(HTTPS_LOGIN_LIVE_COM + "oauth20_token.srf");
    }

    public static OneDriveOAuthConfig getInstance() {
        if (OneDriveOAuthConfig.sInstance == null) {
            OneDriveOAuthConfig.sInstance = new OneDriveOAuthConfig();
        }
        return OneDriveOAuthConfig.sInstance;
    }

    @Override
    public Uri getOAuthAuthorizeUri() {
        return mOAuthAuthorizeUri;
    }

    @Override
    public Uri getOAuthDesktopUri() {
        return mOAuthDesktopUri;
    }

    @Override
    public Uri getOAuthLogoutUri() {
        return mOAuthLogoutUri;
    }

    @Override
    public Uri getOAuthTokenUri() {
        return mOAuthTokenUri;
    }
}
