package com.microsoft.authenticate;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.text.TextUtils;

import com.microsoft.authenticate.OAuth.GrantType;

/**
 * RefreshAccessTokenRequest performs a refresh access token request. Most of the work
 * is done by the parent class, TokenRequest. This class adds in the required body parameters via
 * TokenRequest's hook method, constructBody().
 */
class RefreshAccessTokenRequest extends TokenRequest {

    /** REQUIRED. Value MUST be set to "refresh_token". */
    private final GrantType grantType = GrantType.REFRESH_TOKEN;

    /**  REQUIRED. The refresh token issued to the client. */
    private final String refreshToken;

    private final String scope;

    public RefreshAccessTokenRequest(HttpClient client,
                                     OAuthConfig oAuthConfig,
                                     String clientId,
                                     String refreshToken,
                                     String scope) {
        super(client, oAuthConfig, clientId);

        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    @Override
    protected void constructBody(List<NameValuePair> body) {
        body.add(new BasicNameValuePair(OAuth.REFRESH_TOKEN, this.refreshToken));
        body.add(new BasicNameValuePair(OAuth.SCOPE, this.scope));
        body.add(new BasicNameValuePair(OAuth.GRANT_TYPE, this.grantType.toString()));
    }
}
