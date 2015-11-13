// ------------------------------------------------------------------------------
// Copyright (c) 2015 Microsoft Corporation
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
// ------------------------------------------------------------------------------

package com.microsoft.onedrive.apiexplorer;

import com.onedrive.sdk.concurrency.ICallback;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
     * @param inflater the layout inflater
     * @param container the hosting containing for this fragment
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
                button.setEnabled(false);
                final BaseApplication app = (BaseApplication)getActivity().getApplication();
                final ICallback<Void> serviceCreated = new DefaultCallback<Void>(getActivity()) {
                    @Override
                    public void success(final Void result) {
                        navigateToRoot();
                        button.setEnabled(true);
                    }
                };
                try {
                    app.getOneDriveClient();
                    button.setEnabled(true);
                } catch (final UnsupportedOperationException ignored) {
                    app.createOneDriveClient(getActivity(), serviceCreated);
                }
            }
        });

        return view;
    }

    /**
     * Navigate to the root object in the onedrive
     */
    private void navigateToRoot() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, ItemFragment.newInstance("root"))
                .addToBackStack(null)
                        .commit();
    }
}
