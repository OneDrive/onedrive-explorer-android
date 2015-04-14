package com.microsoft.authenticate;

/**
 * An observer of an OAuth Request. It will be notified of an Exception or of a Response.
 */
interface OAuthRequestObserver {
    /**
     * Callback used on an exception.
     *
     * @param exception
     */
    public void onException(AuthException exception);

    /**
     * Callback used on a response.
     *
     * @param response
     */
    public void onResponse(OAuthResponse response);
}
