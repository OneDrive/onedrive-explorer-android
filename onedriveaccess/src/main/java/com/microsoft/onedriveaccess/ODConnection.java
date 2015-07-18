package com.microsoft.onedriveaccess;

import com.microsoft.services.msa.LiveConnectSession;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

/**
 * Concrete object to interface with the OneDrive service
 */
public class ODConnection extends GsonODConnection {

    /**
     * The credentials session to use for this connection
     */
    private final LiveConnectSession mConnectSession;

    /**
     * The verbose logcat setting
     */
    private boolean mVerboseLogcatOutput;

    /**
     * Default Constructor
     *
     * @param connectSession The credentials session to use for this connection
     */
    public ODConnection(final LiveConnectSession connectSession) {
        mConnectSession = connectSession;
        mVerboseLogcatOutput = true;
    }

    /**
     * Changes the verbosity of the logcat output while requests are issued.
     *
     * @param value <b>True</b> to enable verbose logging, <b>False</b> for minimal logging.
     */
    public void setVerboseLogcatOutput(final boolean value) {
        mVerboseLogcatOutput = value;
    }

    @Override
    protected RequestInterceptor getInterceptor() {
        return InterceptorFactory.getRequestInterceptor(mConnectSession);
    }

    /**
     * Gets the RestAdapter.LogLevel to use for this connection
     *
     * @return The logging level
     */
    @Override
    public RestAdapter.LogLevel getLogLevel() {
        if (mVerboseLogcatOutput) {
            return RestAdapter.LogLevel.FULL;
        }
        return RestAdapter.LogLevel.BASIC;
    }

    @Override
    protected String getEndpoint() {
        return "https://api.onedrive.com";
    }
}
