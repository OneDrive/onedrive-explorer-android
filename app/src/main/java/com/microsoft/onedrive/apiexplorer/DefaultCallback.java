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

    final Context mContext;

    public DefaultCallback(final Context context) {
        mContext = context;
    }
    @Override
    public void success(final T t, final Response response) {
        throw new RuntimeException("Not implemented success");
    }

    @Override
    public void failure(final RetrofitError error) {
        if (error != null && error.getResponse() != null) {
            Log.e(getClass().getSimpleName(), error.getResponse().getStatus() + " " + error.getMessage());
            if (error.getResponse().getStatus() == 401) {
                Toast.makeText(mContext, "The credentials are no longer valid", Toast.LENGTH_LONG).show();
            }
        }
    }
}
