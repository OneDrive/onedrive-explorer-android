package com.microsoft.onedrive.apiexplorer;

import android.app.Application;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.jackson.JacksonFactory;
import com.microsoft.onedrivesdk.IOneDriveService;
import com.microsoft.onedrivesdk.ODConnection;
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
    private static final List<String> SCOPES = Arrays.asList("wl.signin", "wl.offline_access", "onedrive.readwrite");

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
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

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
//        signOut();
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
     * Get an instance of the OneDrive service
     * @return
     */
    IOneDriveService getOneDriveService() {
        final ODConnection connection = new ODConnection(getCredentials());
        connection.setVerboseLogcatOutput(true);
        return connection.getService();
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

    public ImageLoader getImageLoader() {
        if (mImageLoader == null) {

            mImageLoader = new ImageLoader(getRequestQueue(), new ImageLoader.ImageCache() {
                private final LruCache<String, Bitmap> mCache = new LruCache<>(300);

                public void putBitmap(String url, Bitmap bitmap) {
                    mCache.put(url, bitmap);
                }

                public Bitmap getBitmap(String url) {
                    return mCache.get(url);
                }
            });
        }
        return mImageLoader;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
        }
        // Make sure the credentials have been updated!
        return mRequestQueue;
    }
}
