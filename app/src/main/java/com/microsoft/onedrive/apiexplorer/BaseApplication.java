package com.microsoft.onedrive.apiexplorer;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.microsoft.authenticate.AuthClient;
import com.microsoft.authenticate.AuthException;
import com.microsoft.authenticate.AuthListener;
import com.microsoft.authenticate.AuthSession;
import com.microsoft.authenticate.AuthStatus;
import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.ODConnection;

/**
 * Base application
 */
public class BaseApplication extends Application {

    /**
     * The number of thumbnails to cache
     */
    public static final int MAX_IMAGE_CACHE_SIZE = 300;

    /**
     * The client id, get one for your application at https://account.live.com/developers/applications
     */
    private static final String CLIENT_ID = "000000004C146A60";

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
     * The authorization client
     */
    private AuthClient mAuthClient;

    /**
     * What to do when the application starts
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mAuthClient = new AuthClient(this, CLIENT_ID);
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
            public void onAuthComplete(AuthStatus status, AuthSession session, Object userState) {
                final Intent intent = new Intent(getBaseContext(), SignIn.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onAuthError(AuthException exception, Object userState) {
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
