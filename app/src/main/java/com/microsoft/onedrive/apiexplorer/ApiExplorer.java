package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * OneDrive Api Explorer
 */
public class ApiExplorer extends Activity implements ItemFragment.OnFragmentInteractionListener {

    /**
     * OnCreate
     * @param savedInstanceState The instance information
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication application = (BaseApplication)getApplication();

        setContentView(R.layout.activity_api_explorer);

        if (application.getAuthClient().getSession().isExpired()) {
            final Intent intent = new Intent(this, SignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onFragmentInteraction(final DisplayItem item) {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, ItemFragment.newInstance(item.getId()))
                .addToBackStack(null)
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
        if (id == R.id.action_clear_creds) {
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

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
            return;
        }
        getFragmentManager().popBackStack();
    }
}
