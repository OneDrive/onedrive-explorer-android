package com.microsoft.onedrive.apiexplorer;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A default callback that logs errors
 * @param <T> The type returned by this callback
 */
public class DefaultCallback<T> implements Callback<T> {

    /**
     * If there was an auth failure when talking to the service
     */
    private static final int AUTHENTICATION_FAILURE = 401;

    /**
     * The exception text for not implemented runtime exceptions
     */
    public static final String SUCCESS_MUST_BE_IMPLEMENTED = "Success must be implemented";

    /**
     * The context used for displaying toast notifications
     */
    private final Context mContext;

    /**
     * Default constructor
     * @param context The context used for displaying toast notifications
     */
    public DefaultCallback(final Context context) {
        mContext = context;
    }
    @Override
    public void success(final T t, final Response response) {
        throw new RuntimeException(SUCCESS_MUST_BE_IMPLEMENTED);
    }

    @Override
    public void failure(final RetrofitError error) {
        if (error != null && error.getResponse() != null) {
            Log.e(getClass().getSimpleName(), error.getResponse().getStatus() + " " + error.getMessage());
            if (error.getResponse().getStatus() == AUTHENTICATION_FAILURE) {
                Toast.makeText(mContext, mContext.getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show();
            }
        }
    }
}
