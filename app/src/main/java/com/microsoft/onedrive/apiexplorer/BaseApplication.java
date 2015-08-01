package com.microsoft.onedrive.apiexplorer;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.LruCache;
import android.widget.Toast;

import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.ODConnection;
import com.microsoft.services.msa.LiveAuthClient;
import com.microsoft.services.msa.LiveAuthException;
import com.microsoft.services.msa.LiveAuthListener;
import com.microsoft.services.msa.LiveConnectSession;
import com.microsoft.services.msa.LiveStatus;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Arrays;

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
    private LiveAuthClient mAuthClient;

    /**
     * The current auth session
     */
    private LiveConnectSession mAuthSession;

    /**
     * The system connectivity manager
     */
    private ConnectivityManager mConnectivityManager;

    /**
     * What to do when the application starts
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mAuthClient = new LiveAuthClient(this, CLIENT_ID, Arrays.asList("onedrive.readwrite", "onedrive.appfolder"));
        mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Gets the authentication client
     * @return The auth client
     */
    public synchronized LiveAuthClient getAuthClient() {
        return mAuthClient;
    }

    /**
     * Gets the current auth session
     * @return session The session to set
     */
    synchronized LiveConnectSession getAuthSession() {
        return mAuthSession;
    }

    /**
     * Navigates the user to the wifi settings if there is a connection problem
     */
    synchronized boolean goToWifiSettingsIfDisconnected() {
        final NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            Toast.makeText(this, "Unable to access the internet, please visit connection settings", Toast.LENGTH_LONG).show();
            final Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return false;
    }
    /**
     * Sets the current auth session
     * @param session The session to set
     */
    synchronized void setAuthSession(final LiveConnectSession session) {
        mAuthSession = session;
    }

    /**
     * Clears out the auth token from the application store
     */
    void signOut() {
        mAuthClient.logout(new LiveAuthListener() {
            @Override
            public void onAuthComplete(final LiveStatus status, final LiveConnectSession session, final Object userState) {
                final Intent intent = new Intent(getBaseContext(), SignIn.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onAuthError(final LiveAuthException exception, final Object userState) {
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
        if (mAuthSession == null) {
            throw new RuntimeException("Not authenicated yet!");
        }

        if (mODConnection == null) {
            final ODConnection connection = new ODConnection(mAuthSession);
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
