package com.microsoft.onedrive.apiexplorer;

import android.app.Application;
import android.app.FragmentManager;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.jackson.JacksonFactory;
import com.wuman.android.auth.AuthorizationFlow;
import com.wuman.android.auth.DialogFragmentController;
import com.wuman.android.auth.OAuthManager;
import com.wuman.android.auth.oauth2.store.SharedPreferencesCredentialStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class BaseApplication extends Application {

    private static final String CLIENT_ID = "0000000044131935";
    private static final List<String> SCOPES = Arrays.asList("wl.signin", "wl.offline_access", "onedrive.readonly");

    private static final String USER_ID = "userId";

    private SharedPreferencesCredentialStore mCredentialStore;
    private AuthorizationFlow mAuthorizationFlow;

    @Override
    public void onCreate() {
        super.onCreate();
        mCredentialStore = new SharedPreferencesCredentialStore(this, this.getPackageName() + ".credentials", new JacksonFactory());
    }

    /**
     * Gets the users cached credentials
     * @return <b>Null</b> if no credentials were found, otherwise the populated credentials object
     */
    public ODCredentials getCredentials() {
        try {
            final ODCredentials credential = new ODCredentials();
            if (!mCredentialStore.load(USER_ID, credential)) {
                return null;
            }
            return credential;
        } catch (final Exception e) {
            return null;
        }
    }

    OAuthManager getOAuthManager(final FragmentManager fragmentManager) {
        return new OAuthManager(
                getAuthorizationFlow(),
                getAuthorizationFlowUIHandler(fragmentManager));
    }

    void signOut() {
        try {
            final Credential credential = new GoogleCredential();
            mCredentialStore.delete(USER_ID, credential);
        } catch (final IOException ignored) {
            // Ignored
        }
    }

    AuthorizationFlow getAuthorizationFlow() {
        final String LIVE_AUTHORIZATION_ENDPOINT = getString(R.string.base_auth_endpoint) + getString(R.string.url_path_authorize);
        final String LIVE_TOKEN_ENDPOINT = getString(R.string.base_auth_endpoint) + getString(R.string.url_path_token);

        if (mAuthorizationFlow == null) {
            AuthorizationFlow.Builder authorizationFlowBuilder = new AuthorizationFlow.Builder(
                    BearerToken.queryParameterAccessMethod(),
                    AndroidHttp.newCompatibleTransport(),
                    new JacksonFactory(),
                    new GenericUrl(LIVE_TOKEN_ENDPOINT),
                    new ClientParametersAuthentication(CLIENT_ID, null),
                    CLIENT_ID,
                    LIVE_AUTHORIZATION_ENDPOINT);

            mAuthorizationFlow = authorizationFlowBuilder
                    .setCredentialStore(mCredentialStore)
                    .setScopes(SCOPES)
                    .build();
        }
        return mAuthorizationFlow;
    }

    private DialogFragmentController getAuthorizationFlowUIHandler(final FragmentManager fragmentManager) {
        final String LIVE_DESKTOP_REDIRECT_ENDPOINT = getString(R.string.base_auth_endpoint) + getString(R.string.url_path_desktop);

        return new DialogFragmentController(fragmentManager) {
            @Override
            public boolean isJavascriptEnabledForWebView() {
                return true;
            }
            @Override
            public String getRedirectUri() throws IOException {
                return LIVE_DESKTOP_REDIRECT_ENDPOINT;
            }
        };
    }
}
