package com.microsoft.onedriveaccess;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.Converter;

public abstract class AbstractODConnection {

    /**
     * Creates an instance of the IOneDriveService
     *
     * @return The IOneDriveService
     */
    public IOneDriveService getService() {
        final RestAdapter adapter = new RestAdapter.Builder()
                .setLogLevel(getLogLevel())
                .setEndpoint(getEndpoint())
                .setConverter(getConverter())
                .setRequestInterceptor(getInterceptor())
                .build();

        return adapter.create(IOneDriveService.class);
    }

    protected abstract RequestInterceptor getInterceptor();

    protected abstract RestAdapter.LogLevel getLogLevel();

    protected abstract String getEndpoint();

    protected abstract Converter getConverter();
}
