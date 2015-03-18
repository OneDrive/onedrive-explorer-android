package com.microsoft.onedrive.apiexplorer;

import android.util.Log;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A default callback that logs errors
 * @param <T> The type returned by this callback
 */
public class DefaultCallback<T> implements Callback<T> {
    @Override
    public void success(final T t, final Response response) {
        throw new RuntimeException("Not implemented success");
    }

    @Override
    public void failure(final RetrofitError error) {
        Log.e(getClass().getSimpleName(), error.getResponse().getStatus() + " " + error.getMessage());
    }
}
