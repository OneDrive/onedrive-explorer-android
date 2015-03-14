package com.microsoft.onedrive.apiexplorer;

import android.util.Log;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Peter Nied on 3/13/2015.
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
