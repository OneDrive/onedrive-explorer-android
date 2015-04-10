package com.microsoft.onedrive.apiexplorer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;
import android.webkit.CookieManager;

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
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.ODConnection;
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
     * The number of thumbnails to cache
     */
    public static final int MAX_IMAGE_CACHE_SIZE = 300;

    /**
     * The user id for credentials store
     */
    static final String USER_ID = "userId";

    /**
     * The client id, get one for your application at https://account.live.com/developers/applications
     */
    private static final String CLIENT_ID = "000000004C146A60";

    /**
     * The scopes used for this app
     */
    private static final List<String> SCOPES = Arrays.asList("wl.signin", "onedrive.readwrite");

    /**
     * The post-fix for the credentials secure storage
     */
    public static final String CREDENTIALS = ".credentials";

    /**
     * The credentials store instance
     */
    private SharedPreferencesCredentialStore mCredentialStore;

    /**
     * The authorization flow for OAuth
     */
    private AuthorizationFlow mAuthorizationFlow;

    /**
     * The request queue
     */
    private RequestQueue mRequestQueue;

    /**
     * The image loader
     */
    private ImageLoader mImageLoader;

    /**
     * The OneDrive Service instance
     */
    private IOneDriveService mODConnection;

    /**
     * What to do when the application starts
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mCredentialStore = new SharedPreferencesCredentialStore(
                this,
                this.getPackageName() + CREDENTIALS,
                new JacksonFactory());
    }

    /**
     * Gets the users cached credentials
     *
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
     *
     * @param fragmentManager the fragement manager to host this UX
     * @return The manager
     */
    OAuthManager getOAuthManager(final FragmentManager fragmentManager) {
        return new OAuthManager(
                getAuthorizationFlow(),
                getAuthorizationFlowUIHandler(fragmentManager));
    }

    /**
     * Get the authorization flow
     *
     * @return the flow
     */
    private synchronized AuthorizationFlow getAuthorizationFlow() {
        if (mAuthorizationFlow == null) {
            final String liveAuthorizationEndpoint = getString(R.string.base_auth_endpoint)
                    + getString(R.string.url_path_authorize);
            final String liveTokenEndpoint = getString(R.string.base_auth_endpoint)
                    + getString(R.string.url_path_token);

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
     * Clears out the auth token from the application store
     */
    void signOut() {
        try {
            final Credential credential = new GoogleCredential();
            mCredentialStore.delete(USER_ID, credential);
        } catch (final IOException ignored) {
            // Ignored
            Log.d(getClass().getSimpleName(), ignored.toString());
        }

        mAuthorizationFlow = null;
    }

    /**
     * Get an instance of the OneDrive service
     *
     * @return The OneDrive Service
     */
    synchronized IOneDriveService getOneDriveService() {
        if (mODConnection == null) {
            final ODConnection connection = new ODConnection(getCredentials());
            connection.setVerboseLogcatOutput(true);
            mODConnection = connection.getService();
        }
        return mODConnection;
    }

    /**
     * Create the UX for handling OAuth sign in
     *
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
            public boolean disableWebViewCache() {
                return false;
            }

            @Override
            public boolean removePreviousCookie() {
                return true;
            }

            @Override
            public String getRedirectUri() throws IOException {
                return liveDesktopRedirectEndpoint;
            }
        };
    }

    /**
     * Gets the image loader for this application
     *
     * @return the image loader
     */
    public synchronized ImageLoader getImageLoader() {
        if (mImageLoader == null) {

            mImageLoader = new ImageLoader(getRequestQueue(), new ImageLoader.ImageCache() {
                private final LruCache<String, Bitmap> mCache = new LruCache<>(MAX_IMAGE_CACHE_SIZE);

                public void putBitmap(final String url, final Bitmap bitmap) {
                    mCache.put(url, bitmap);
                }

                public Bitmap getBitmap(final String url) {
                    return mCache.get(url);
                }
            });
        }
        return mImageLoader;
    }

    /**
     * Gets the request queue for this application
     *
     * @return The request queue
     */
    public synchronized RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
        }
        // Make sure the credentials have been updated!
        return mRequestQueue;
    }
}
