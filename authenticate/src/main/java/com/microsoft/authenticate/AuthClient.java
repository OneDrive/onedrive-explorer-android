package com.microsoft.authenticate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

/**
 * {@code LiveAuthClient} is a class responsible for retrieving a {@link AuthSession}, which
 * can be given to a {@link ODConnection} in order to make requests to the Live Connect API.
 */
public class AuthClient {

    private static class AuthCompleteRunnable extends AuthListenerCaller implements Runnable {

        private final AuthStatus status;
        private final AuthSession session;

        public AuthCompleteRunnable(AuthListener listener,
                                    Object userState,
                                    AuthStatus status,
                                    AuthSession session) {
            super(listener, userState);
            this.status = status;
            this.session = session;
        }

        @Override
        public void run() {
            listener.onAuthComplete(status, session, userState);
        }
    }

    private static class AuthErrorRunnable extends AuthListenerCaller implements Runnable {

        private final AuthException exception;

        public AuthErrorRunnable(AuthListener listener,
                                 Object userState,
                                 AuthException exception) {
            super(listener, userState);
            this.exception = exception;
        }

        @Override
        public void run() {
            listener.onAuthError(exception, userState);
        }

    }

    private static abstract class AuthListenerCaller {
        protected final AuthListener listener;
        protected final Object userState;

        public AuthListenerCaller(AuthListener listener, Object userState) {
            this.listener = listener;
            this.userState = userState;
        }
    }

    /**
     * This class observes an OAuthRequest and calls the appropriate Listener method.
     * On a successful response, it will call the
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * On an exception or an unsuccessful response, it will call
     * {@link AuthListener#onAuthError(AuthException, Object)}.
     */
    private class ListenerCallerObserver extends AuthListenerCaller
            implements OAuthRequestObserver,
            OAuthResponseVisitor {

        public ListenerCallerObserver(AuthListener listener, Object userState) {
            super(listener, userState);
        }

        @Override
        public void onException(AuthException exception) {
            new AuthErrorRunnable(listener, userState, exception).run();
        }

        @Override
        public void onResponse(OAuthResponse response) {
            response.accept(this);
        }

        @Override
        public void visit(OAuthErrorResponse response) {
            String error = response.getError().toString().toLowerCase(Locale.US);
            String errorDescription = response.getErrorDescription();
            String errorUri = response.getErrorUri();
            AuthException exception = new AuthException(error,
                    errorDescription,
                    errorUri);

            new AuthErrorRunnable(listener, userState, exception).run();
        }

        @Override
        public void visit(OAuthSuccessfulResponse response) {
            session.loadFromOAuthResponse(response);

            new AuthCompleteRunnable(listener, userState, AuthStatus.CONNECTED, session).run();
        }
    }

    /**
     * Observer that will, depending on the response, save or clear the refresh token.
     */
    private class RefreshTokenWriter implements OAuthRequestObserver, OAuthResponseVisitor {

        @Override
        public void onException(AuthException exception) {
        }

        @Override
        public void onResponse(OAuthResponse response) {
            response.accept(this);
        }

        @Override
        public void visit(OAuthErrorResponse response) {
            if (response.getError() == OAuth.ErrorType.INVALID_GRANT) {
                AuthClient.this.clearRefreshTokenFromPreferences();
            }
        }

        @Override
        public void visit(OAuthSuccessfulResponse response) {
            String refreshToken = response.getRefreshToken();
            if (!TextUtils.isEmpty(refreshToken)) {
                this.saveRefreshTokenToPreferences(refreshToken);
            }
        }

        private boolean saveRefreshTokenToPreferences(String refreshToken) {
            SharedPreferences settings =
                    applicationContext.getSharedPreferences(PreferencesConstants.FILE_NAME,
                            Context.MODE_PRIVATE);
            Editor editor = settings.edit();
            editor.putString(PreferencesConstants.REFRESH_TOKEN_KEY, refreshToken);

            return editor.commit();
        }
    }

    /**
     * An {@link OAuthResponseVisitor} that checks the {@link OAuthResponse} and if it is a
     * successful response, it loads the response into the given session.
     */
    private static class SessionRefresher implements OAuthResponseVisitor {

        private final AuthSession session;
        private boolean visitedSuccessfulResponse;

        public SessionRefresher(AuthSession session) {
            assert session != null;

            this.session = session;
            this.visitedSuccessfulResponse = false;
        }

        @Override
        public void visit(OAuthErrorResponse response) {
            this.visitedSuccessfulResponse = false;
        }

        @Override
        public void visit(OAuthSuccessfulResponse response) {
            this.session.loadFromOAuthResponse(response);
            this.visitedSuccessfulResponse = true;
        }

        public boolean visitedSuccessfulResponse() {
            return this.visitedSuccessfulResponse;
        }
    }

    /**
     * A AuthListener that does nothing on each of the call backs.
     * This is used so when a null listener is passed in, this can be used, instead of null,
     * to avoid if (listener == null) checks.
     */
    private static final AuthListener NULL_LISTENER = new AuthListener() {
        @Override
        public void onAuthComplete(AuthStatus status, AuthSession session, Object sender) {
        }

        @Override
        public void onAuthError(AuthException exception, Object sender) {
        }
    };

    private final Context applicationContext;
    private final String clientId;
    private boolean hasPendingLoginRequest;

    /**
     * Responsible for all network (i.e., HTTP) calls.
     * Tests will want to change this to mock the network and HTTP responses.
     *
     * @see #setHttpClient(HttpClient)
     */
    private HttpClient httpClient;

    /**
     * saved from initialize and used in the login call if login's scopes are null.
     */
    private Set<String> scopesFromInitialize;

    /**
     * One-to-one relationship between LiveAuthClient and AuthSession.
     */
    private final AuthSession session;
    {
        this.httpClient = new DefaultHttpClient();
        this.hasPendingLoginRequest = false;
        this.session = new AuthSession(this);
    }

    /**
     * Constructs a new {@code LiveAuthClient} instance and initializes its member variables.
     *
     * @param context  Context of the Application used to save any refresh_token.
     * @param clientId The client_id of the Live Connect Application to login to.
     */
    public AuthClient(Context context, String clientId) {
        this.applicationContext = context.getApplicationContext();
        this.clientId = clientId;
    }

    /**
     * @return the client_id of the Live Connect application.
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     *
     * @param scopes   to initialize the {@link AuthSession} with.
     *                 See <a href="http://msdn.microsoft.com/en-us/library/hh243646.aspx">MSDN Live Connect
     *                 Reference's Scopes and permissions</a> for a list of scopes and explanations.
     * @param listener called on either completion or error during the initialize process.
     */
    public void initialize(Iterable<String> scopes, AuthListener listener) {
        this.initialize(scopes, listener, null, null);
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     *
     * @param scopes    to initialize the {@link AuthSession} with.
     *                  See <a href="http://msdn.microsoft.com/en-us/library/hh243646.aspx">MSDN Live Connect
     *                  Reference's Scopes and permissions</a> for a list of scopes and explanations.
     * @param listener  called on either completion or error during the initialize process
     * @param userState arbitrary object that is used to determine the caller of the method.
     */
    public void initialize(Iterable<String> scopes, AuthListener listener, Object userState) {
        initialize(scopes, listener, userState, null);
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     *
     * @param scopes       to initialize the {@link AuthSession} with.
     *                     See <a href="http://msdn.microsoft.com/en-us/library/hh243646.aspx">MSDN Live Connect
     *                     Reference's Scopes and permissions</a> for a list of scopes and explanations.
     * @param listener     called on either completion or error during the initialize process
     * @param userState    arbitrary object that is used to determine the caller of the method.
     * @param refreshToken optional previously saved token to be used by this client.
     */
    public void initialize(Iterable<String> scopes, AuthListener listener, Object userState,
                           String refreshToken) {
        if (listener == null) {
            listener = NULL_LISTENER;
        }

        if (scopes == null) {
            scopes = Arrays.asList(new String[0]);
        }

        // copy scopes for login
        this.scopesFromInitialize = new HashSet<String>();
        for (String scope : scopes) {
            this.scopesFromInitialize.add(scope);
        }
        this.scopesFromInitialize = Collections.unmodifiableSet(this.scopesFromInitialize);

        //if no token is provided, try to get one from SharedPreferences
        if (refreshToken == null) {
            refreshToken = this.getRefreshTokenFromPreferences();
        }

        if (refreshToken == null) {
            listener.onAuthComplete(AuthStatus.UNKNOWN, null, userState);
            return;
        }

        RefreshAccessTokenRequest request =
                new RefreshAccessTokenRequest(this.httpClient,
                        this.clientId,
                        refreshToken,
                        TextUtils.join(OAuth.SCOPE_DELIMITER, scopes));
        TokenRequestAsync asyncRequest = new TokenRequestAsync(request);

        asyncRequest.addObserver(new ListenerCallerObserver(listener, userState));
        asyncRequest.addObserver(new RefreshTokenWriter());

        asyncRequest.execute();
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     * <p/>
     * This initialize will use the last successfully used scopes from either a login or initialize.
     *
     * @param listener called on either completion or error during the initialize process.
     */
    public void initialize(AuthListener listener) {
        this.initialize(listener, null);
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     * <p/>
     * This initialize will use the last successfully used scopes from either a login or initialize.
     *
     * @param listener  called on either completion or error during the initialize process.
     * @param userState arbitrary object that is used to determine the caller of the method.
     */
    public void initialize(AuthListener listener, Object userState) {
        this.initialize(null, listener, userState, null);
    }

    /**
     * Logs in an user with the given scopes.
     * <p/>
     * login displays a {@link Dialog} that will prompt the
     * user for a username and password, and ask for consent to use the given scopes.
     * A {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     *
     * @param activity {@link Activity} instance to display the Login dialog on.
     * @param scopes   to initialize the {@link AuthSession} with.
     *                 See <a href="http://msdn.microsoft.com/en-us/library/hh243646.aspx">MSDN Live Connect
     *                 Reference's Scopes and permissions</a> for a list of scopes and explanations.
     * @param listener called on either completion or error during the login process.
     * @throws IllegalStateException if there is a pending login request.
     */
    public void login(Activity activity, Iterable<String> scopes, AuthListener listener) {
        this.login(activity, scopes, listener, null);
    }

    /**
     * Logs in an user with the given scopes.
     * <p/>
     * login displays a {@link Dialog} that will prompt the
     * user for a username and password, and ask for consent to use the given scopes.
     * A {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     *
     * @param activity  {@link Activity} instance to display the Login dialog on
     * @param scopes    to initialize the {@link AuthSession} with.
     *                  See <a href="http://msdn.microsoft.com/en-us/library/hh243646.aspx">MSDN Live Connect
     *                  Reference's Scopes and permissions</a> for a list of scopes and explanations.
     * @param listener  called on either completion or error during the login process.
     * @param userState arbitrary object that is used to determine the caller of the method.
     * @throws IllegalStateException if there is a pending login request.
     */
    public void login(Activity activity,
                      Iterable<String> scopes,
                      AuthListener listener,
                      Object userState) {
        if (listener == null) {
            listener = NULL_LISTENER;
        }

        if (this.hasPendingLoginRequest) {
            throw new IllegalStateException(ErrorMessages.LOGIN_IN_PROGRESS);
        }

        // if no scopes were passed in, use the scopes from initialize or if those are empty,
        // create an empty list
        if (scopes == null) {
            if (this.scopesFromInitialize == null) {
                scopes = Arrays.asList(new String[0]);
            } else {
                scopes = this.scopesFromInitialize;
            }
        }

        // if the session is valid and contains all the scopes, do not display the login ui.
        boolean showDialog = this.session.isExpired() ||
                !this.session.contains(scopes);
        if (!showDialog) {
            listener.onAuthComplete(AuthStatus.CONNECTED, this.session, userState);
            return;
        }

        String scope = TextUtils.join(OAuth.SCOPE_DELIMITER, scopes);
        String redirectUri = Config.INSTANCE.getOAuthDesktopUri().toString();
        AuthorizationRequest request = new AuthorizationRequest(activity,
                this.httpClient,
                this.clientId,
                redirectUri,
                scope);

        request.addObserver(new ListenerCallerObserver(listener, userState));
        request.addObserver(new RefreshTokenWriter());
        request.addObserver(new OAuthRequestObserver() {
            @Override
            public void onException(AuthException exception) {
                AuthClient.this.hasPendingLoginRequest = false;
            }

            @Override
            public void onResponse(OAuthResponse response) {
                AuthClient.this.hasPendingLoginRequest = false;
            }
        });

        this.hasPendingLoginRequest = true;

        request.execute();
    }

    /**
     * Logs out the given user.
     * <p/>
     * Also, this method clears the previously created {@link AuthSession}.
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)} will be
     * called on completion. Otherwise,
     * {@link AuthListener#onAuthError(AuthException, Object)} will be called.
     *
     * @param listener called on either completion or error during the logout process.
     */
    public void logout(AuthListener listener) {
        this.logout(listener, null);
    }

    /**
     * Logs out the given user.
     * <p/>
     * Also, this method clears the previously created {@link AuthSession}.
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)} will be
     * called on completion. Otherwise,
     * {@link AuthListener#onAuthError(AuthException, Object)} will be called.
     *
     * @param listener  called on either completion or error during the logout process.
     * @param userState arbitrary object that is used to determine the caller of the method.
     */
    public void logout(AuthListener listener, Object userState) {
        if (listener == null) {
            listener = NULL_LISTENER;
        }

        session.setAccessToken(null);
        session.setAuthenticationToken(null);
        session.setRefreshToken(null);
        session.setScopes(null);
        session.setTokenType(null);

        clearRefreshTokenFromPreferences();

        CookieSyncManager cookieSyncManager =
                CookieSyncManager.createInstance(this.applicationContext);
        CookieManager manager = CookieManager.getInstance();
        Uri logoutUri = Config.INSTANCE.getOAuthLogoutUri();
        String url = logoutUri.toString();
        String domain = logoutUri.getHost();

        List<String> cookieKeys = this.getCookieKeysFromPreferences();
        for (String cookieKey : cookieKeys) {
            String value = TextUtils.join("", new String[]{
                    cookieKey,
                    "=; expires=Thu, 30-Oct-1980 16:00:00 GMT;domain=",
                    domain,
                    ";path=/;version=1"
            });

            manager.setCookie(url, value);
        }

        cookieSyncManager.sync();
        listener.onAuthComplete(AuthStatus.UNKNOWN, null, userState);
    }

    /**
     * @return The {@link HttpClient} instance used by this {@code LiveAuthClient}.
     */
    HttpClient getHttpClient() {
        return this.httpClient;
    }

    /**
     * @return The {@link AuthSession} instance that this {@code LiveAuthClient} created.
     */
    public AuthSession getSession() {
        return session;
    }

    /**
     * Refreshes the previously created session.
     *
     * @return true if the session was successfully refreshed.
     */
    boolean refresh() {
        String scope = TextUtils.join(OAuth.SCOPE_DELIMITER, this.session.getScopes());
        String refreshToken = this.session.getRefreshToken();

        if (TextUtils.isEmpty(refreshToken)) {
            return false;
        }

        RefreshAccessTokenRequest request =
                new RefreshAccessTokenRequest(this.httpClient, this.clientId, refreshToken, scope);

        OAuthResponse response;
        try {
            response = request.execute();
        } catch (AuthException e) {
            return false;
        }

        SessionRefresher refresher = new SessionRefresher(this.session);
        response.accept(refresher);
        response.accept(new RefreshTokenWriter());

        return refresher.visitedSuccessfulResponse();
    }

    /**
     * Sets the {@link HttpClient} that is used for HTTP requests by this {@code LiveAuthClient}.
     * Tests will want to change this to mock the network/HTTP responses.
     *
     * @param client The new HttpClient to be set.
     */
    void setHttpClient(HttpClient client) {
        assert client != null;
        this.httpClient = client;
    }

    /**
     * Clears the refresh token from this {@code LiveAuthClient}'s
     * {@link Activity#getPreferences(int)}.
     *
     * @return true if the refresh token was successfully cleared.
     */
    private boolean clearRefreshTokenFromPreferences() {
        SharedPreferences settings = getSharedPreferences();
        Editor editor = settings.edit();
        editor.remove(PreferencesConstants.REFRESH_TOKEN_KEY);

        return editor.commit();
    }

    private SharedPreferences getSharedPreferences() {
        return applicationContext.getSharedPreferences(PreferencesConstants.FILE_NAME,
                Context.MODE_PRIVATE);
    }

    private List<String> getCookieKeysFromPreferences() {
        SharedPreferences settings = getSharedPreferences();
        String cookieKeys = settings.getString(PreferencesConstants.COOKIES_KEY, "");

        return Arrays.asList(TextUtils.split(cookieKeys, PreferencesConstants.COOKIE_DELIMITER));
    }

    /**
     * Retrieves the refresh token from this {@code LiveAuthClient}'s
     * {@link Activity#getPreferences(int)}.
     *
     * @return the refresh token from persistent storage.
     */
    private String getRefreshTokenFromPreferences() {
        SharedPreferences settings = getSharedPreferences();
        return settings.getString(PreferencesConstants.REFRESH_TOKEN_KEY, null);
    }
}