package com.microsoft.onedrivesdk;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Concrete object to interface with the OneDrive service
 */
public class ODConnection {
    private final IODCredential mCredential;
    private boolean mVerboseLogcatOutput;

    public ODConnection(final IODCredential credential) {
        mCredential = credential;
        mVerboseLogcatOutput = false;
    }

    public void setVerboseLogcatOutput(final boolean value) {
        mVerboseLogcatOutput = value;
    }

    public IOneDriveService getService() {

        RestAdapter adapter = new RestAdapter.Builder()
                .setLogLevel(getLogLevel())
                .setEndpoint("https://api.onedrive.com")
                .setConverter(new GsonConverter(GsonFactory.getGsonInstance()))
                .setRequestInterceptor(InterceptorFactory.getRequestInterceptor(mCredential))
                .build();

        return adapter.create(IOneDriveService.class);
    }

    private RestAdapter.LogLevel getLogLevel() {
        if (mVerboseLogcatOutput) {
            return RestAdapter.LogLevel.FULL;
        }
        return RestAdapter.LogLevel.BASIC;
    }
}
