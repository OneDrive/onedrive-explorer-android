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

import com.google.gson.JsonElement;

import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.extensions.IDeltaCollectionPage;
import com.onedrive.sdk.extensions.Item;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shows the changes of the decedents of an item
 */
public class DeltaFragment extends Fragment {

    /**
     * The argument for the item id
     */
    private static final String ARG_ITEM_ID = "itemId";

    /**
     * The argument for the item name
     */
    private static final String ARG_ITEM_NAME_ID = "itemName";

    /**
     * The max number of pages to retrieve
     */
    private static final int MAX_PAGE_COUNT = 5;

    /**
     * The number of pages that have been downloaded
     */
    private final AtomicInteger mCurrentPagesCount = new AtomicInteger(0);

    /**
     * The item id
     */
    private String mItemId;

    /**
     * The item name
     */
    private String mItemName;

    /**
     * Create a new instance of ItemFragment
     * @param item the item
     * @return The fragment
     */
    static DeltaFragment newInstance(final Item item) {
        final DeltaFragment fragment = new DeltaFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_ITEM_ID, item.id);
        args.putString(ARG_ITEM_NAME_ID, item.name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication app = (BaseApplication) getActivity().getApplication();
        if (app.goToWifiSettingsIfDisconnected()) {
            return;
        }

        if (getArguments() != null) {
            mItemId = getArguments().getString(ARG_ITEM_ID);
            mItemName  = getArguments().getString(ARG_ITEM_NAME_ID);
        }

        if (getView() != null) {
            getView().findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
            getView().findViewById(R.id.json).setVisibility(View.INVISIBLE);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                                final ViewGroup container,
                                final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_json_view, container, false);
        ((TextView)view.findViewById(R.id.json)).setMovementMethod(new ScrollingMovementMethod());
        getActivity().setTitle(getString(R.string.delta_title, mItemName));

        refresh();
        return view;
    }

    /**
     * Refresh the UI
     */
    private void refresh() {
        if (getView() != null) {
            getView().findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
            final TextView jsonView = (TextView)getView().findViewById(R.id.json);
            jsonView.setVisibility(View.INVISIBLE);
            jsonView.setText("");
            mCurrentPagesCount.set(0);
        }

        final String deltaToken = getDeltaInfo().getString(mItemId, null);
        final Activity activity = getActivity();
        ((BaseApplication) activity.getApplication())
            .getOneDriveClient()
            .getDrive()
            .getItems(mItemId)
            .getDelta(deltaToken)
            .buildRequest()
            .select("id,name,deleted")
            .get(pageHandler());
    }

    /**
     * Create a handler for downloaded pages
     * @return The callback to handle a fresh DeltaPage
     */
    private ICallback<IDeltaCollectionPage> pageHandler() {
        return new DefaultCallback<IDeltaCollectionPage>(getActivity()) {
            @Override
            public void success(final IDeltaCollectionPage page) {
                final View view = getView();
                if (view == null) {
                    return;
                }

                final View viewById = view.findViewById(R.id.json);
                if (viewById == null) {
                    return;
                }

                if (mCurrentPagesCount.incrementAndGet() == MAX_PAGE_COUNT) {
                    Toast.makeText(getActivity(), R.string.max_pages_downloaded, Toast.LENGTH_LONG).show();
                    return;
                }

                final TextView jsonView = (TextView) viewById;
                final CharSequence originalText = jsonView.getText();
                final StringBuilder sb = new StringBuilder(originalText);
                for (final Item i : page.getCurrentPage()) {
                    try {
                        final int indentSpaces = 3;
                        sb.append(new JSONObject(i.getRawObject().toString()).toString(indentSpaces));
                    } catch (final JSONException ignored) {
                        Log.e(getClass().getName(), "Unable to parse the response body to json");
                    }
                    sb.append("\n");
                }

                if (page.getCurrentPage().size() == 0) {
                    sb.append(getString(R.string.empty_delta));
                }
                jsonView.setText(sb.toString());
                view.findViewById(android.R.id.progress).setVisibility(View.INVISIBLE);
                jsonView.setVisibility(View.VISIBLE);
                if (page.getNextPage() != null) {
                    page.getNextPage()
                        .buildRequest()
                        .get(pageHandler());
                }
                final JsonElement deltaToken = page.getRawObject().get("@delta.token");
                if (deltaToken != null) {
                    getDeltaInfo().edit().putString(mItemId, deltaToken.getAsString()).commit();
                }
            }
        };
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_delta_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refresh();
                return true;
            case R.id.reset_token:
                resetToken();
                return true;
            case R.id.reset_all_tokens:
                resetAllTokens();
                return true;
            default:
                return false;
        }
    }

    /**
     * Reset the delta token for the currently in view item
     */
    private void resetToken() {
        getDeltaInfo().edit().putString(mItemId, null).commit();
        Toast.makeText(getActivity(),
                       getString(R.string.cleared_saved_delta_token, mItemName),
                       Toast.LENGTH_LONG).show();
        refresh();
    }

    /**
     * Reset all delta tokens
     */
    private void resetAllTokens() {
        getDeltaInfo().edit().clear().commit();
        Toast.makeText(getActivity(),
                       getString(R.string.cleared_saved_delta_tokens),
                       Toast.LENGTH_LONG).show();
        refresh();
    }

    /**
     * Get the view delta preferences
     * @return The preferences
     */
    private SharedPreferences getDeltaInfo() {
        return getActivity().getSharedPreferences("delta", Context.MODE_PRIVATE);
    }
}
