package com.microsoft.onedriveaccess;

import com.microsoft.onedriveacess.BuildConfig;

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
     * @param credential The credentials used by the interceptor
     * @return The interceptor object
     */
    public static RequestInterceptor getRequestInterceptor(final IODCredential credential) {
        return new RequestInterceptor() {

            @Override
            public void intercept(final RequestFacade request) {
                request.addHeader("Authorization", "bearer " + credential.getAccessToken());
                request.addHeader("User-Agent", "OneDriveSDK Android " + BuildConfig.VERSION_NAME);
            }
        };

    }
}
