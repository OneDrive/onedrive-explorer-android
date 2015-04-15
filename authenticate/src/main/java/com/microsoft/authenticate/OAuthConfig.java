package com.microsoft.authenticate;

import android.net.Uri;

/**
 * Config is a singleton class that contains the values used throughout the SDK.
 */
public interface OAuthConfig {

    Uri getOAuthAuthorizeUri();

    Uri getOAuthDesktopUri();

    Uri getOAuthLogoutUri();

    Uri getOAuthTokenUri();
}
