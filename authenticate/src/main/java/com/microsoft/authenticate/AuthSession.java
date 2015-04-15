package com.microsoft.authenticate;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Represents an authentication session.
 */
public class AuthSession {

    private String mAccessToken;
    private String mAuthenticationToken;

    /** Keeps track of all the listeners, and fires the property change events */
    private final PropertyChangeSupport mChangeSupport;

    /**
     * The AuthClient that created this object.
     * This is needed in order to perform a refresh request.
     * There is a one-to-one relationship between the AuthSession and AuthClient.
     */
    private final AuthClient mCreator;

    private Date mExpiresIn;
    private String mRefreshToken;
    private Set<String> mScopes;
    private String mTokenType;

    /**
     * Constructors a new AuthSession, and sets its creator to the passed in
     * AuthClient. All other member variables are left uninitialized.
     *
     * @param creator
     */
    AuthSession(final AuthClient creator) {
        mCreator = creator;
        mChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the session that receives notification when any
     * property is changed.
     *
     * @param listener
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the session that receives notification when a
     * specific property is changed.
     *
     * @param propertyName
     * @param listener
     */
    public void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * @return The access token for the signed-in, connected user.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * @return A user-specific token that provides information to an app so that it can validate
     *         the user.
     */
    public String getAuthenticationToken() {
        return mAuthenticationToken;
    }

    /**
     * @return The exact time when a session expires.
     */
    public Date getExpiresIn() {
        // Defensive copy
        return new Date(mExpiresIn.getTime());
    }

    /**
     * @return An array of all PropertyChangeListeners for this session.
     */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return mChangeSupport.getPropertyChangeListeners();
    }

    /**
     * @param propertyName
     * @return An array of all PropertyChangeListeners for a specific property for this session.
     */
    public PropertyChangeListener[] getPropertyChangeListeners(final String propertyName) {
        return mChangeSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * @return A user-specific refresh token that the app can use to refresh the access token.
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * @return The scopes that the user has consented to.
     */
    public Iterable<String> getScopes() {
        // Defensive copy is not necessary, because scopes is an unmodifiableSet
        return mScopes;
    }

    /**
     * @return The type of token.
     */
    public String getTokenType() {
        return mTokenType;
    }

    /**
     * @return {@code true} if the session is expired.
     */
    public boolean isExpired() {
        if (mExpiresIn == null) {
            return true;
        }

        final Date now = new Date();

        return now.after(mExpiresIn);
    }

    /**
     * Removes a PropertyChangeListeners on a session.
     * @param listener
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener for a specific property on a session.
     * @param propertyName
     * @param listener
     */
    public void removePropertyChangeListener(final String propertyName,
                                             final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public String toString() {
        return String.format("AuthSession [accessToken=%s, authenticationToken=%s, expiresIn=%s, refreshToken=%s, scopes=%s, tokenType=%s]",
                mAccessToken,
                mAuthenticationToken,
                mExpiresIn,
                mRefreshToken,
                mScopes,
                mTokenType);
    }

    boolean contains(final Iterable<String> scopes) {
        if (scopes == null) {
            return true;
        } else if (mScopes == null) {
            return false;
        }

        for (final String scope : scopes) {
            if (!mScopes.contains(scope)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Fills in the AuthSession with the OAuthResponse.
     * WARNING: The OAuthResponse must not contain OAuth.ERROR.
     *
     * @param response to load from
     */
    void loadFromOAuthResponse(final OAuthSuccessfulResponse response) {
        mAccessToken = response.getAccessToken();
        mTokenType = response.getmTokenType().toString().toLowerCase(Locale.US);

        if (response.hasAuthenticationToken()) {
            mAuthenticationToken = response.getAuthenticationToken();
        }

        if (response.hasExpiresIn()) {
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, response.getmExpiresIn());
            setExpiresIn(calendar.getTime());
        }

        if (response.hasRefreshToken()) {
            mRefreshToken = response.getmRefreshToken();
        }

        if (response.hasScope()) {
            final String scopeString = response.getmScope();
            setScopes(Arrays.asList(scopeString.split(OAuth.SCOPE_DELIMITER)));
        }
    }

    /**
     * Refreshes this AuthSession
     *
     * @return true if it was able to refresh the refresh token.
     */
    boolean refresh() {
        return mCreator.refresh();
    }

    void setAccessToken(final String accessToken) {
        final String oldValue = mAccessToken;
        mAccessToken = accessToken;

        mChangeSupport.firePropertyChange("accessToken", oldValue, mAccessToken);
    }

    void setAuthenticationToken(final String authenticationToken) {
        final String oldValue = mAuthenticationToken;
        mAuthenticationToken = authenticationToken;

        mChangeSupport.firePropertyChange("authenticationToken",
                oldValue,
                mAuthenticationToken);
    }

    void setExpiresIn(final Date expiresIn) {
        final Date oldValue = mExpiresIn;
        mExpiresIn = new Date(expiresIn.getTime());

        mChangeSupport.firePropertyChange("expiresIn", oldValue, mExpiresIn);
    }

    void setRefreshToken(final String refreshToken) {
        final String oldValue = mRefreshToken;
        mRefreshToken = refreshToken;

        mChangeSupport.firePropertyChange("refreshToken", oldValue, mRefreshToken);
    }

    void setScopes(final Iterable<String> scopes) {
        final Iterable<String> oldValue = mScopes;

        // Defensive copy
        mScopes = new HashSet<String>();
        if (scopes != null) {
            for (String scope : scopes) {
                mScopes.add(scope);
            }
        }

        mScopes = Collections.unmodifiableSet(mScopes);

        mChangeSupport.firePropertyChange("scopes", oldValue, mScopes);
    }

    void setTokenType(final String tokenType) {
        final String oldValue = mTokenType;
        mTokenType = tokenType;

        mChangeSupport.firePropertyChange("tokenType", oldValue, mTokenType);
    }

    boolean willExpireInSecs(final int secs) {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, secs);

        final Date future = calendar.getTime();

        // if add secs seconds to the current time and it is after the expired time
        // then it is almost expired.
        return future.after(mExpiresIn);
    }
}