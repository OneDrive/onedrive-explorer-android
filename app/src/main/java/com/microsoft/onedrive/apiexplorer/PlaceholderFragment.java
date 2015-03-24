package com.microsoft.onedrive.apiexplorer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.microsoft.onedriveaccess.IOneDriveService;

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

        final View view = inflater.inflate(R.layout.fragment_api_explorer, container, false);

        final Button button = (Button) view.findViewById(R.id.query_vroom);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment, ItemFragment.newInstance(IOneDriveService.ROOT_FOLDER_ID))
                                .addToBackStack(null)
                                .commit();
            }
        });

        return view;
    }
}
