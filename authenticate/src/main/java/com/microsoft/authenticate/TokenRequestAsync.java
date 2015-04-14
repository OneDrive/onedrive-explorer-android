package com.microsoft.authenticate;

import android.os.AsyncTask;

/**
 * TokenRequestAsync performs an async token request. It takes in a TokenRequest,
 * executes it, checks the OAuthResponse, and then calls the given listener.
 */
class TokenRequestAsync extends AsyncTask<Void, Void, Void> implements ObservableOAuthRequest {

    private final DefaultObservableOAuthRequest observerable;

    /** Not null if there was an exception */
    private AuthException exception;

    /** Not null if there was a response */
    private OAuthResponse response;

    private final TokenRequest request;

    /**
     * Constructs a new TokenRequestAsync and initializes its member variables
     *
     * @param request to perform
     */
    public TokenRequestAsync(TokenRequest request) {
        assert request != null;

        this.observerable = new DefaultObservableOAuthRequest();
        this.request = request;
    }

    @Override
    public void addObserver(OAuthRequestObserver observer) {
        this.observerable.addObserver(observer);
    }

    @Override
    public boolean removeObserver(OAuthRequestObserver observer) {
        return this.observerable.removeObserver(observer);
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            this.response = this.request.execute();
        } catch (AuthException e) {
            this.exception = e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if (this.response != null) {
            this.observerable.notifyObservers(this.response);
        } else if (this.exception != null) {
            this.observerable.notifyObservers(this.exception);
        } else {
            final AuthException exception = new AuthException(ErrorMessages.CLIENT_ERROR);
            this.observerable.notifyObservers(exception);
        }
    }
}
