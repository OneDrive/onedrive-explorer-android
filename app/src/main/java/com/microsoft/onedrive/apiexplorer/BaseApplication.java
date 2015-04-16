package com.microsoft.onedrive.apiexplorer;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.Toast;

import com.microsoft.authenticate.AuthClient;
import com.microsoft.authenticate.AuthException;
import com.microsoft.authenticate.AuthListener;
import com.microsoft.authenticate.AuthSession;
import com.microsoft.authenticate.AuthStatus;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.ODConnection;
import com.microsoft.onedriveaccess.OneDriveOAuthConfig;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Base application
 */
public class BaseApplication extends Application {

    /**
     * The number of thumbnails to cache
     */
    private static final int MAX_IMAGE_CACHE_SIZE = 300;

    /**
     * Thumbnail cache
     */
    private LruCache<String, Bitmap> mImageCache;

    /**
     * The http client to use with any ad-hoc requests
     */
    private HttpClient mHttpClient;

    /**
     * The client id, get one for your application at https://account.live.com/developers/applications
     */
    private static final String CLIENT_ID = "000000004C146A60";

    /**
     * The OneDrive Service instance
     */
    private IOneDriveService mODConnection;

    /**
     * The authorization client
     */
    private AuthClient mAuthClient;

    /**
     * What to do when the application starts
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mAuthClient = new AuthClient(this, OneDriveOAuthConfig.getInstance(), CLIENT_ID);
    }

    /**
     * Gets the authentication client
     * @return The auth client
     */
    public AuthClient getAuthClient() {
        return mAuthClient;
    }

    /**
     * Clears out the auth token from the application store
     */
    void signOut() {
        mAuthClient.logout(new AuthListener() {
            @Override
            public void onAuthComplete(final AuthStatus status, final AuthSession session, final Object userState) {
                final Intent intent = new Intent(getBaseContext(), SignIn.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onAuthError(final AuthException exception, final Object userState) {
                Toast.makeText(getBaseContext(), "Logout error " + exception, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Get an instance of the OneDrive service
     *
     * @return The OneDrive Service
     */
    synchronized IOneDriveService getOneDriveService() {
        if (mODConnection == null) {
            final ODConnection connection = new ODConnection(mAuthClient);
            connection.setVerboseLogcatOutput(true);
            mODConnection = connection.getService();
        }
        return mODConnection;
    }

    /**
     * Gets the image cache for this application
     *
     * @return the image loader
     */
    public synchronized LruCache<String, Bitmap> getImageCache() {
        if (mImageCache == null) {
            mImageCache = new LruCache<>(BaseApplication.MAX_IMAGE_CACHE_SIZE);
        }
        return mImageCache;
    }

    /**
     * Gets the http client for this application
     *
     * @return the http client
     */
    public synchronized HttpClient getHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
        }
        return mHttpClient;
    }
}
