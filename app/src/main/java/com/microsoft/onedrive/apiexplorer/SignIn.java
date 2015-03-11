package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.api.client.auth.oauth2.Credential;
import com.wuman.android.auth.OAuthManager;

import java.io.IOException;

public class SignIn extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication mBaseApplication = (BaseApplication) getApplication();

        setContentView(R.layout.activity_sign_in);

        if (savedInstanceState == null) {
            final NotSignedInFragment notSignedInFragment = new NotSignedInFragment();
            notSignedInFragment.init(mBaseApplication.getOAuthManager(getFragmentManager()));
            getFragmentManager().beginTransaction()
                    .add(R.id.container, notSignedInFragment)
                    .commit();
        }
    }

    public static class NotSignedInFragment extends Fragment {

        private OAuthManager mAuthManager;

        public void init(final OAuthManager authManager) {
            mAuthManager = authManager;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater,
                                 final ViewGroup container,
                                 final Bundle savedInstanceState) {
            final View signInFragment = inflater.inflate(R.layout.fragment_sign_in, container, false);

            final OAuthManager.OAuthCallback<Credential> oAuthCallback = new OAuthManager.OAuthCallback<Credential>() {
                @Override
                public void run(final OAuthManager.OAuthFuture<Credential> future) {
                    try {
                        future.getResult();
                        Toast.makeText(signInFragment.getContext(), "Signed in!", Toast.LENGTH_LONG).show();
                        getActivity().finish();
                    } catch (final IOException e) {
                        Toast.makeText(signInFragment.getContext(), "Unable to sign in!", Toast.LENGTH_LONG).show();
                    }
                }
            };

            final Button signIn = (Button)signInFragment.findViewById(R.id.sign_in);
            signIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    mAuthManager.authorizeImplicitly("userId", oAuthCallback, new Handler());
                }
            });
            return signInFragment;
        }
    }
}