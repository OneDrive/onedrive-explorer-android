package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * OneDrive Api Explorer
 */
public class ApiExplorer extends Activity implements FolderFragment.OnFragmentInteractionListener {

    /**
     * OnCreate
     * @param savedInstanceState The instance information
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication application = (BaseApplication)getApplication();

        setContentView(R.layout.activity_api_explorer);

        if (application.getCredentials() == null) {
            final Intent intent = new Intent(this, SignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        final Button button = (Button) findViewById(R.id.query_vroom);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment, FolderFragment.newInstance("root"))
                        .commit();
            }
        });
    }

    @Override
    public void onFragmentInteraction(String id) {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, FolderFragment.newInstance(id))
                .commit();
    }


    /**
     * Creates the options menu
     * @param menu the menu to create
     * @return if the menu was created
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_api_explorer, menu);
        return true;
    }

    /**
     * Handle options menu selection
     * @param item The menu item that was selected
     * @return If the selection was handled
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sign_out) {
            final BaseApplication application = (BaseApplication)getApplication();
            application.signOut();
            final Intent restartApiExplorer = new Intent(this, ApiExplorer.class);
            restartApiExplorer.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            restartApiExplorer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(restartApiExplorer);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        /**
         * Default constructor
         */
        public PlaceholderFragment() {
        }

        /**
         * Handle creation of the view
         * @param inflater the layout inflator
         * @param container the hosting containing for this fragement
         * @param savedInstanceState saved state information
         * @return The constructed view
         */
        @Override
        public View onCreateView(final LayoutInflater inflater,
                                 final ViewGroup container,
                                 final Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_api_explorer, container, false);
        }
    }
}
