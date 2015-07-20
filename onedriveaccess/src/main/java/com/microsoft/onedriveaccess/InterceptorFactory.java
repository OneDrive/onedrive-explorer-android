package com.microsoft.onedriveaccess;

import com.microsoft.onedriveacess.BuildConfig;
import com.microsoft.services.msa.LiveConnectSession;

import retrofit.RequestInterceptor;

/**
 * Produces Request interceptor for OneDrive service requests
 */
final class InterceptorFactory {

    /**
     * Default Constructor
     */
    private InterceptorFactory() {
    }

    /**
     * Creates an instance of the request interceptor
     * @param connectSession The credentials session to use for this connection
     * @return The interceptor object
     */
    public static RequestInterceptor getRequestInterceptor(final LiveConnectSession connectSession) {
        return new RequestInterceptor() {

            @Override
            public void intercept(final RequestFacade request) {
                request.addHeader("Authorization", "bearer " + connectSession.getAccessToken());
                request.addHeader("User-Agent", "OneDriveSDK Android " + BuildConfig.VERSION_NAME);
            }
        };
    }
}
