package com.microsoft.onedrivesdk;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Concrete object to interface with the OneDrive service
 */
public class ODConnection {
    /**
     * The credentials for this connection
     */
    private final IODCredential mCredential;

    /**
     * The verbose logcat setting
     */
    private boolean mVerboseLogcatOutput;

    /**
     * Default Constructor
     * @param credential The credentials to use for this connection
     */
    public ODConnection(final IODCredential credential) {
        mCredential = credential;
        mVerboseLogcatOutput = false;
    }

    /**
     * Changes the verbosity of the logcat output while requests are issued.
     * @param value <b>True</b> to enable verbose logging, <b>False</b> for minimal logging.
     */
    public void setVerboseLogcatOutput(final boolean value) {
        mVerboseLogcatOutput = value;
    }

    /**
     * Creates an instance of the IOneDriveService
     * @return The IOneDriveService
     */
    public IOneDriveService getService() {
        final GsonConverter converter = new GsonConverter(GsonFactory.getGsonInstance());
        final RequestInterceptor requestInterceptor = InterceptorFactory.getRequestInterceptor(mCredential);
        final RestAdapter adapter = new RestAdapter.Builder()
                .setLogLevel(getLogLevel())
                .setEndpoint("https://api.onedrive.com")
                .setConverter(converter)
                .setRequestInterceptor(requestInterceptor)
                .build();

        return adapter.create(IOneDriveService.class);
    }

    /**
     * Gets the RestAdapter.LogLevel to use for this connection
     * @return The logging level
     */
    private RestAdapter.LogLevel getLogLevel() {
        if (mVerboseLogcatOutput) {
            return RestAdapter.LogLevel.FULL;
        }
        return RestAdapter.LogLevel.BASIC;
    }
}
