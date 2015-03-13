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

/**
 * The sign in activity
 */
public class SignIn extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_in);

        if (savedInstanceState == null) {
            final NotSignedInFragment notSignedInFragment = new NotSignedInFragment();
            notSignedInFragment.init();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, notSignedInFragment)
                    .commit();
        }
    }

    /**
     * The fragment to display when the user is not signed in
     */
    public static class NotSignedInFragment extends Fragment {

        /**
         * Initializes this fragment
         */
        public void init() {
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
                    final BaseApplication baseApplication = (BaseApplication) getActivity().getApplication();
                    baseApplication
                            .getOAuthManager(getFragmentManager())
                            .authorizeImplicitly("userId", oAuthCallback, new Handler());
                }
            });
            return signInFragment;
        }
    }
}