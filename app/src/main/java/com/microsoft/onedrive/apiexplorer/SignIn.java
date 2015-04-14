package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.microsoft.authenticate.AuthException;
import com.microsoft.authenticate.AuthListener;
import com.microsoft.authenticate.AuthSession;
import com.microsoft.authenticate.AuthStatus;

import java.util.Arrays;
import java.util.List;

/**
 * The sign in activity
 */
public class SignIn extends Activity {

    /**
     * The scopes used for this app
     */
    private static final List<String> SCOPES = Arrays.asList("wl.signin", "onedrive.readwrite");

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_in);

        if (savedInstanceState == null) {
            final NotSignedInFragment notSignedInFragment = new NotSignedInFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, notSignedInFragment)
                    .commit();
        }
    }

    /**
     * The fragment to display when the user is not signed in
     */
    public static class NotSignedInFragment extends Fragment {

        @Override
        public View onCreateView(final LayoutInflater inflater,
                                 final ViewGroup container,
                                 final Bundle savedInstanceState) {
            final View signInFragment = inflater.inflate(R.layout.fragment_sign_in, container, false);

            final Context context = signInFragment.getContext();
            final AuthListener authListener = new AuthListener() {
                @Override
                public void onAuthComplete(final AuthStatus status, final AuthSession session, final Object userState) {
                    Toast.makeText(context, context.getString(R.string.signed_in), Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }

                @Override
                public void onAuthError(final AuthException exception, final Object userState) {
                    Toast.makeText(context, "Sign in failed! " + exception.getMessage(), Toast.LENGTH_LONG).show();
                }
            };

            final Button signIn = (Button) signInFragment.findViewById(R.id.sign_in);
            signIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Activity activity = getActivity();
                    final BaseApplication baseApplication = (BaseApplication) activity.getApplication();
                    baseApplication.getAuthClient().login(getActivity(), SCOPES, authListener);
                }
            });
            return signInFragment;
        }
    }
}