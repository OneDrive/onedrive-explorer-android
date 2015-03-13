package com.microsoft.onedrive.apiexplorer;

import android.app.Application;
import android.app.FragmentManager;
import android.util.Log;

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

/**
 * Base application
 */
public class BaseApplication extends Application {

    /**
     * The client id
     */
    private static final String CLIENT_ID = "0000000044131935";

    /**
     * The scopes used for this app
     */
    private static final List<String> SCOPES = Arrays.asList("wl.signin", "wl.offline_access", "onedrive.readonly");

    /**
     * The user id for credentials store
     */
    private static final String USER_ID = "userId";

    /**
     * The credentials store instance
     */
    private SharedPreferencesCredentialStore mCredentialStore;

    /**
     * The authorization flow for OAuth
     */
    private AuthorizationFlow mAuthorizationFlow;

    /**
     * What to do when the application starts
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mCredentialStore = new SharedPreferencesCredentialStore(
                this,
                this.getPackageName() + ".credentials",
                new JacksonFactory());

        // Until we can ensure the token is not expired, clear the user state on startup
        signOut();
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

    /**
     * Get an OAuth fragmenet manager
     * @param fragmentManager the fragement manager to host this UX
     * @return The manager
     */
    OAuthManager getOAuthManager(final FragmentManager fragmentManager) {
        return new OAuthManager(
                getAuthorizationFlow(),
                getAuthorizationFlowUIHandler(fragmentManager));
    }

    /**
     * Sign the user out
     */
    void signOut() {
        try {
            final Credential credential = new GoogleCredential();
            mCredentialStore.delete(USER_ID, credential);
        } catch (final IOException ignored) {
            // Ignored
            Log.d(getClass().getSimpleName(), ignored.toString());
        }
    }

    /**
     * Get the authorization flow
     * @return the flow
     */
    AuthorizationFlow getAuthorizationFlow() {
        final String liveAuthorizationEndpoint = getString(R.string.base_auth_endpoint)
                + getString(R.string.url_path_authorize);
        final String liveTokenEndpoint = getString(R.string.base_auth_endpoint)
                + getString(R.string.url_path_token);

        if (mAuthorizationFlow == null) {
            AuthorizationFlow.Builder authorizationFlowBuilder = new AuthorizationFlow.Builder(
                    BearerToken.queryParameterAccessMethod(),
                    AndroidHttp.newCompatibleTransport(),
                    new JacksonFactory(),
                    new GenericUrl(liveTokenEndpoint),
                    new ClientParametersAuthentication(CLIENT_ID, null),
                    CLIENT_ID,
                    liveAuthorizationEndpoint);

            mAuthorizationFlow = authorizationFlowBuilder
                    .setCredentialStore(mCredentialStore)
                    .setScopes(SCOPES)
                    .build();
        }
        return mAuthorizationFlow;
    }

    /**
     * Create the UX for handling OAuth sign in
     * @param fragmentManager the fragement manager to host this UX
     * @return The controller for the fragment
     */
    private DialogFragmentController getAuthorizationFlowUIHandler(final FragmentManager fragmentManager) {
        final String liveDesktopRedirectEndpoint = getString(R.string.base_auth_endpoint)
                + getString(R.string.url_path_desktop);

        return new DialogFragmentController(fragmentManager) {
            @Override
            public boolean isJavascriptEnabledForWebView() {
                return true;
            }
            @Override
            public String getRedirectUri() throws IOException {
                return liveDesktopRedirectEndpoint;
            }
        };
    }
}
