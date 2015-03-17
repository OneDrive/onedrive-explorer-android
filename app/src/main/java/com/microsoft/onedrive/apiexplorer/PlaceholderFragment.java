package com.microsoft.onedrive.apiexplorer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

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
        final int color = getResources().getColor(android.R.color.white);
        getView().setBackgroundColor(color);
        return inflater.inflate(R.layout.fragment_api_explorer, container, false);
    }
}
