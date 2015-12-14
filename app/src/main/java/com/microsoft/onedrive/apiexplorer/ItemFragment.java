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

import com.onedrive.sdk.concurrency.AsyncMonitor;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.concurrency.IProgressCallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.OneDriveErrorCodes;
import com.onedrive.sdk.extensions.Folder;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;
import com.onedrive.sdk.extensions.ItemReference;
import com.onedrive.sdk.extensions.Permission;
import com.onedrive.sdk.options.Option;
import com.onedrive.sdk.options.QueryOption;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles interacting with Items on OneDrive
 */
@SuppressWarnings("ConstantConditions")
public class ItemFragment extends Fragment implements AbsListView.OnItemClickListener {

    /**
     * The item id argument string
     */
    private static final String ARG_ITEM_ID = "itemId";

    /**
     * The request code for simple upload
     */
    private static final int REQUEST_CODE_SIMPLE_UPLOAD = 6767;

    /**
     * The scheme to get content from a content resolver
     */
    private static final String SCHEME_CONTENT = "content";

    /**
     * The prefix for the item breadcrumb when the parent reference is unavailable
     */
    private static final String DRIVE_PREFIX = "/drive/";

    /**
     * Expansion options to get all children, thumbnails of children, and thumbnails
     */
    private static final String EXPAND_OPTIONS_FOR_CHILDREN_AND_THUMBNAILS = "children(expand=thumbnails),thumbnails";

    /**
     * Expansion options to get all children, thumbnails of children, and thumbnails when limited
     */
    private static final String EXPAND_OPTIONS_FOR_CHILDREN_AND_THUMBNAILS_LIMITED = "children,thumbnails";

    /**
     * The accepted file mime types for uploading to OneDrive
     */
    private static final String ACCEPTED_UPLOAD_MIME_TYPES = "*/*";

    /**
     * The copy destination preference key
     */
    private static final String COPY_DESTINATION_PREF_KEY = "copy_destination";

    /**
     * The item id for this item
     */
    private String mItemId;

    /**
     * The backing item representation
     */
    private Item mItem;

    /**
     * The listener for interacting with this fragment
     */
    private OnFragmentInteractionListener mListener;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private DisplayItemAdapter mAdapter;

    /**
     * If the current fragment should prioritize the empty view over the visualization
     */
    private final AtomicBoolean mEmpty = new AtomicBoolean(false);

    /**
     * Create a new instance of ItemFragment
     * @param itemId The item id to create it for
     * @return The fragment
     */
    static ItemFragment newInstance(final String itemId) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new DisplayItemAdapter(getActivity());

        final BaseApplication app = (BaseApplication) getActivity().getApplication();
        if (app.goToWifiSettingsIfDisconnected()) {
            return;
        }

        if (getArguments() != null) {
            mItemId = getArguments().getString(ARG_ITEM_ID);
        }

        if (mItem != null) {
            ((TextView) getActivity().findViewById(R.id.fragment_label)).setText(mItem.parentReference.path);
        } else {
            ((TextView)getActivity().findViewById(R.id.fragment_label)).setText(null);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_folder, container, false);
        final AbsListView mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        ((RadioButton) view.findViewById(android.R.id.button1)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        if (isChecked) {
                            setFocus(ItemFocus.Visualization, getView());
                        }
                    }
                });

        ((RadioButton)view.findViewById(android.R.id.button2)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        if (isChecked) {
                            setFocus(ItemFocus.Json, getView());
                        }
                    }
                });

        ((TextView)view.findViewById(R.id.json)).setMovementMethod(new ScrollingMovementMethod());

        refresh();

        return view;
    }

    // onAttach(Context) never gets called on API22 and earlier devices
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(final Activity context) {
        super.onAttach(context);
        mListener = (OnFragmentInteractionListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (mItem != null) {
            // Add menu options
            inflater.inflate(R.menu.menu_item_fragment, menu);

            // Assume we are a folder first
            menu.findItem(R.id.action_download).setVisible(false);
            menu.findItem(R.id.action_copy).setVisible(false);
            configureSetCopyDestinationMenuItem(menu.findItem(R.id.action_set_copy_destination));


            // Make sure that the root folder has certain options unavailable
            if ("root".equalsIgnoreCase(mItemId)) {
                menu.findItem(R.id.action_rename).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
            }

            // Make sure that if it is a file, we don't let you perform actions that don't make sense for files
            if (mItem.file != null) {
                menu.findItem(R.id.action_create_folder).setVisible(false);
                menu.findItem(R.id.action_upload_file).setVisible(false);
                menu.findItem(R.id.action_download).setVisible(true);
                menu.findItem(R.id.action_copy).setVisible(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mItem == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.action_copy:
                copy(mItem);
                return true;
            case R.id.action_set_copy_destination:
                setCopyDestination(mItem);
                return true;
            case R.id.action_upload_file:
                upload(REQUEST_CODE_SIMPLE_UPLOAD);
                return true;
            case R.id.action_refresh:
                refresh();
                return true;
            case R.id.action_create_folder:
                createFolder(mItem);
                return true;
            case R.id.action_rename:
                renameItem(mItem);
                return true;
            case R.id.action_delete:
                deleteItem(mItem);
                return true;
            case R.id.action_download:
                download(mItem);
                return true;
            case R.id.action_create_link:
                createLink(mItem);
                return true;
            case R.id.action_view_delta:
                viewDelta(mItem);
                return true;
            case R.id.action_navigate_by_path:
                navigateByPath(mItem);
                return true;
            default:
                return false;
        }
    }

    /**
     * Sets the copy destination within the preferences
     * @param item The item to mark as the destination
     */
    private void setCopyDestination(final Item item) {
        getCopyPrefs().edit().putString(COPY_DESTINATION_PREF_KEY, item.id).commit();
        getActivity().invalidateOptionsMenu();
    }

    /**
     * Copies an item onto the current destination in the copy preferences
     * @param item The item to copy
     */
    private void copy(final Item item) {
        final BaseApplication app = (BaseApplication) getActivity().getApplication();
        final IOneDriveClient oneDriveClient = app.getOneDriveClient();
        final ItemReference parentReference = new ItemReference();
        parentReference.id = getCopyPrefs().getString(COPY_DESTINATION_PREF_KEY, null);

        final ProgressDialog dialog = new ProgressDialog(getActivity(), ProgressDialog.STYLE_HORIZONTAL);
        dialog.setTitle("Copying item");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage("Waiting for copy to complete");

        final IProgressCallback<Item> progressCallback = new IProgressCallback<Item>() {
            @Override
            public void progress(final long current, final long max) {
                dialog.setMax((int)current);
                dialog.setMax((int) max);
            }

            @Override
            public void success(final Item item) {
                dialog.dismiss();
                final String string = getString(R.string.copy_success_message,
                                                item.name,
                                                item.parentReference.path);
                Toast.makeText(getActivity(), string, Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(final ClientException error) {
                dialog.dismiss();
                new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.error_title)
                    .setMessage(error.getMessage())
                    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
            }
        };

        final DefaultCallback<AsyncMonitor<Item>> callback
            = new DefaultCallback<AsyncMonitor<Item>>(getActivity()) {
            @Override
            public void success(final AsyncMonitor<Item> itemAsyncMonitor) {
                final int millisBetweenPoll = 1000;
                itemAsyncMonitor.pollForResult(millisBetweenPoll, progressCallback);
            }
        };
        oneDriveClient
            .getDrive()
            .getItems(item.id)
            .getCopy(item.name, parentReference)
            .buildRequest()
            .create(callback);
        dialog.show();
    }

    @Override
    public void onItemClick(final AdapterView<?> parent,
                            final View view, final int position,
                            final long id) {
        if (null != mListener) {
            mListener.onFragmentInteraction((DisplayItem) mAdapter.getItem(position));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.stopDownloadingThumbnails();
    }

    /**
     * Creates a callback for drilling into an item
     * @param context The application context to display messages
     * @return The callback to refresh this item with
     */
    private ICallback<Item> getItemCallback(final BaseApplication context) {
        return new DefaultCallback<Item>(context) {
            @Override
            public void success(final Item item) {
                mItem = item;
                if (getView() != null) {
                    final AbsListView mListView = (AbsListView) getView().findViewById(android.R.id.list);
                    final DisplayItemAdapter adapter = (DisplayItemAdapter)mListView.getAdapter();
                    adapter.clear();

                    String text = null;
                    try {
                        String rawString = item.getRawObject().toString();
                        final JSONObject object = new JSONObject(rawString);
                        final int intentSize = 3;
                        text = object.toString(intentSize);
                    } catch (final Exception e) {
                        Log.e(getClass().getName(), "Unable to parse the response body to json");
                    }

                    if (text != null) {
                        ((TextView) getView().findViewById(R.id.json)).setText(text);
                    }

                    final String fragmentLabel;
                    if (mItem.parentReference != null) {
                        fragmentLabel = mItem.parentReference.path
                                + context.getString(R.string.item_path_separator)
                                + mItem.name;
                    } else {
                        fragmentLabel = DRIVE_PREFIX + mItem.name;
                    }
                    ((TextView)getActivity().findViewById(R.id.fragment_label)).setText(fragmentLabel);

                    mEmpty.set(item.children == null || item.children.getCurrentPage().isEmpty());

                    if (item.children == null || item.children.getCurrentPage().isEmpty()) {
                        final TextView emptyText = (TextView)getView().findViewById(android.R.id.empty);
                        if (item.folder != null) {
                            emptyText.setText(R.string.empty_list);
                        } else {
                            emptyText.setText(R.string.empty_file);
                        }
                        setFocus(ItemFocus.Empty, getView());

                    } else {
                        for (final Item childItem : item.children.getCurrentPage()) {
                            adapter.add(new DisplayItem(adapter,
                                                        childItem,
                                                        childItem.id,
                                                        context.getImageCache()));
                        }
                        setFocus(ItemFocus.Visualization, getView());
                    }
                    getActivity().invalidateOptionsMenu();
                }
            }

            @Override
            public void failure(final ClientException error) {
                if (getView() != null) {
                    final TextView view = (TextView) getView().findViewById(android.R.id.empty);
                    view.setText(context.getString(R.string.item_fragment_item_lookup_error, mItemId));
                    setFocus(ItemFocus.Empty, getView());
                }
            }
        };
    }

    /**
     * Refreshes the data for this fragment
     */
    private void refresh() {
        if (getView() != null) {
            setFocus(ItemFocus.Progress, getView());
        }
        mItem = null;

        final BaseApplication app = (BaseApplication) getActivity().getApplication();
        final IOneDriveClient oneDriveClient = app.getOneDriveClient();
        final ICallback<Item> itemCallback = getItemCallback(app);

        final String itemId;
        if (mItemId.equals("root")) {
            itemId = "root";
        } else {
            itemId = mItemId;
        }

        oneDriveClient
            .getDrive()
            .getItems(itemId)
            .buildRequest()
            .expand(getExpansionOptions(oneDriveClient))
            .get(itemCallback);
    }

    /**
     * Gets the expansion options for requests on items
     * @see {https://github.com/OneDrive/onedrive-api-docs/issues/203}
     * @param oneDriveClient the OneDrive client
     * @return The string for expand options
     */
    @NonNull
    private String getExpansionOptions(final IOneDriveClient oneDriveClient) {
        final String expansionOption;
        switch (oneDriveClient.getAuthenticator().getAccountInfo().getAccountType()) {
            case MicrosoftAccount:
                expansionOption = EXPAND_OPTIONS_FOR_CHILDREN_AND_THUMBNAILS;
                break;

            default:
                expansionOption = EXPAND_OPTIONS_FOR_CHILDREN_AND_THUMBNAILS_LIMITED;
                break;
        }
        return expansionOption;
    }

    /**
     * Deletes the item represented by this fragment
     * @param item The item to delete
     */
    private void deleteItem(final Item item) {
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.delete)
            .setIcon(android.R.drawable.ic_delete)
            .setMessage(getActivity().getString(R.string.confirm_delete_action, mItem.name))
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    final BaseApplication application = (BaseApplication) getActivity()
                                                                              .getApplication();
                    application.getOneDriveClient()
                        .getDrive()
                        .getItems(item.id)
                        .buildRequest()
                        .delete(new DefaultCallback<Void>(application) {
                            @Override
                            public void success(final Void response) {
                                Toast.makeText(getActivity(),
                                        application.getString(R.string.deleted_this_item,
                                                item.name),
                                        Toast.LENGTH_LONG).show();
                                getActivity().onBackPressed();
                            }
                        });
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    dialog.cancel();
                }
            })
            .create();
        alertDialog.show();
    }

    /**
     * Creates a link on this item
     * @param item The item to delete
     */
    private void createLink(final Item item) {
        final CharSequence[] items = {"view", "edit"};
        final int nothingSelected = -1;
        final AtomicInteger selection = new AtomicInteger(nothingSelected);
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.create_link)
                .setIcon(android.R.drawable.ic_menu_share)
                .setPositiveButton(R.string.create_link, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (selection.get() == nothingSelected) {
                            return;
                        }

                        final BaseApplication application = (BaseApplication) getActivity()
                                                                                  .getApplication();
                        application.getOneDriveClient()
                            .getDrive()
                            .getItems(item.id)
                            .getCreateLink(items[selection.get()].toString())
                            .buildRequest()
                            .create(new DefaultCallback<Permission>(getActivity()) {
                                @Override
                                public void success(final Permission permission) {
                                    final ClipboardManager cm = (ClipboardManager)
                                                                    getActivity()
                                                                        .getSystemService(Context.CLIPBOARD_SERVICE);
                                    final ClipData data =
                                        ClipData.newPlainText("Link Url", permission.link.webUrl);
                                    cm.setPrimaryClip(data);
                                    Toast.makeText(getActivity(),
                                                      application.getString(R.string.created_link),
                                                      Toast.LENGTH_LONG).show();
                                    getActivity().onBackPressed();
                                }
                            });
                    }
                })
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        selection.set(which);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.cancel();
                    }
                })
                .create();
        alertDialog.show();
    }

    /**
     * Renames a sourceItem
     * @param sourceItem The sourceItem to rename
     */
    private void renameItem(final Item sourceItem) {
        final Activity activity = getActivity();
        final EditText newName = new EditText(activity);
        newName.setInputType(InputType.TYPE_CLASS_TEXT);
        newName.setHint(sourceItem.name);
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.rename)
            .setIcon(android.R.drawable.ic_menu_edit)
            .setView(newName)
            .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    final ICallback<Item> callback = new DefaultCallback<Item>(getActivity()) {
                        @Override
                        public void success(final Item item) {
                            Toast.makeText(activity,
                                              activity
                                                  .getString(R.string.renamed_item, sourceItem.name,
                                                                item.name),
                                              Toast.LENGTH_LONG).show();
                            refresh();
                            dialog.dismiss();
                        }

                        @Override
                        public void failure(final ClientException error) {
                            Toast.makeText(activity,
                                              activity.getString(R.string.rename_error,
                                                                    sourceItem.name),
                                              Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    };
                    Item updatedItem = new Item();
                    updatedItem.id = sourceItem.id;
                    updatedItem.name = newName.getText().toString();
                    ((BaseApplication) activity.getApplication())
                        .getOneDriveClient()
                        .getDrive()
                        .getItems(updatedItem.id)
                        .buildRequest()
                        .update(updatedItem, callback);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    dialog.cancel();
                }
            })
            .create();
        alertDialog.show();
    }

    /**
     * Creates a folder
     * @param item The parent of the folder to create
     */
    private void createFolder(final Item item) {
        final Activity activity = getActivity();
        final EditText newName = new EditText(activity);
        newName.setInputType(InputType.TYPE_CLASS_TEXT);
        newName.setHint(activity.getString(R.string.new_folder_hint));

        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.create_folder)
                .setView(newName)
                .setIcon(android.R.drawable.ic_menu_add)
                .setPositiveButton(R.string.create_folder, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final ICallback<Item> callback = new DefaultCallback<Item>(activity) {
                            @Override
                            public void success(final Item createdItem) {
                                Toast.makeText(activity,
                                                  activity.getString(R.string.created_folder,
                                                                        createdItem.name,
                                                                        item.name),
                                                  Toast.LENGTH_LONG)
                                    .show();
                                refresh();
                                dialog.dismiss();
                            }

                            @Override
                            public void failure(final ClientException error) {
                                super.failure(error);
                                Toast.makeText(activity,
                                                  activity.getString(R.string.new_folder_error,
                                                                        item.name),
                                                  Toast.LENGTH_LONG)
                                    .show();
                                dialog.dismiss();
                            }
                        };

                        final Item newItem = new Item();
                        newItem.name = newName.getText().toString();
                        newItem.folder = new Folder();

                        ((BaseApplication) activity.getApplication())
                            .getOneDriveClient()
                            .getDrive()
                            .getItems(mItemId)
                            .getChildren()
                            .buildRequest()
                            .create(newItem, callback);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.cancel();
                    }
                })
                .create();
        alertDialog.show();
    }

    /**
     * Starts the uploading experience
     * @param requestCode The request code that will be used to choose simple/chunked uploading
     */
    private void upload(final int requestCode) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType(ACCEPTED_UPLOAD_MIME_TYPES);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final BaseApplication application = (BaseApplication) getActivity().getApplication();
        final IOneDriveClient oneDriveClient = application.getOneDriveClient();

        if (requestCode == REQUEST_CODE_SIMPLE_UPLOAD
                && data != null
                && data.getData() != null
                && data.getData().getScheme().equalsIgnoreCase(SCHEME_CONTENT)) {

            final ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setTitle(R.string.upload_in_progress_title);
            dialog.setMessage(getString(R.string.upload_in_progress_message));
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgressNumberFormat(getString(R.string.upload_in_progress_number_format));
            dialog.show();
            final AsyncTask<Void, Void, Void> uploadFile = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    try {
                        final ContentResolver contentResolver = getActivity().getContentResolver();
                        final ContentProviderClient contentProvider = contentResolver
                                .acquireContentProviderClient(data.getData());
                        final byte[] fileInMemory = FileContent.getFileBytes(contentProvider, data.getData());
                        contentProvider.release();

                        // Fix up the file name (needed for camera roll photos, etc)
                        final String filename = FileContent.getValidFileName(contentResolver, data.getData());
                        final Option option = new QueryOption("@name.conflictBehavior", "fail");
                        oneDriveClient
                            .getDrive()
                            .getItems(mItemId)
                            .getChildren()
                            .byId(filename)
                            .getContent()
                            .buildRequest(Collections.singletonList(option))
                            .put(fileInMemory,
                                new IProgressCallback<Item>() {
                                    @Override
                                    public void success(final Item item) {
                                        dialog.dismiss();
                                        Toast.makeText(getActivity(),
                                                          application
                                                              .getString(R.string.upload_complete,
                                                                            item.name),
                                                          Toast.LENGTH_LONG).show();
                                        refresh();
                                    }

                                    @Override
                                    public void failure(final ClientException error) {
                                        dialog.dismiss();
                                        if (error.isError(OneDriveErrorCodes.NameAlreadyExists)) {
                                            Toast.makeText(getActivity(),
                                                           R.string.upload_failed_name_conflict,
                                                           Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(getActivity(),
                                                              application
                                                                  .getString(R.string.upload_failed,
                                                                                filename),
                                                              Toast.LENGTH_LONG).show();
                                        }
                                    }

                                    @Override
                                    public void progress(final long current, final long max) {
                                        dialog.setProgress((int) current);
                                        dialog.setMax((int) max);
                                    }
                                });
                    } catch (final Exception e) {
                        Log.e(getClass().getSimpleName(), e.getMessage());
                        Log.e(getClass().getSimpleName(), e.toString());
                    }
                    return null;
                }
            };
            uploadFile.execute();
        }
    }

    /**
     * Downloads this item
     * @param item The item to download
     */
    private void download(final Item item) {
        final Activity activity = getActivity();
        final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context
                .DOWNLOAD_SERVICE);
        final String downloadUrl = item.getRawObject().get("@content.downloadUrl").getAsString();
        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle(item.name);
        request.setDescription(activity.getString(R.string.file_from_onedrive));
        request.allowScanningByMediaScanner();
        if (item.file != null) {
            request.setMimeType(item.file.mimeType);
        }
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadManager.enqueue(request);
        Toast.makeText(activity, activity.getString(R.string.starting_download_message),
                          Toast.LENGTH_LONG).show();
    }

    /**
     * Starts up a new View Delta viewer
     * @param item The item to delta over
     */
    private void viewDelta(final Item item) {
        final DeltaFragment fragment = DeltaFragment.newInstance(item);
        navigateToFragment(fragment);
    }

    /**
     * Navigate to a new fragment
     * @param fragment the fragment to navigate into
     */
    private void navigateToFragment(final Fragment fragment) {
        mAdapter.stopDownloadingThumbnails();
        getFragmentManager()
            .beginTransaction()
            .add(R.id.fragment, fragment)
            .addToBackStack(null)
            .commit();
    }

    /**
     * Navigates to an item by path
     * @param item the source item
     */
    private void navigateByPath(final Item item) {
        final BaseApplication application = (BaseApplication) getActivity().getApplication();
        final IOneDriveClient oneDriveClient = application.getOneDriveClient();
        final Activity activity = getActivity();

        final EditText itemPath = new EditText(activity);
        itemPath.setInputType(InputType.TYPE_CLASS_TEXT);

        final DefaultCallback<Item> itemCallback = new DefaultCallback<Item>(activity) {
            @Override
            public void success(final Item item) {
                final ItemFragment fragment = ItemFragment.newInstance(item.id);
                navigateToFragment(fragment);
            }
        };

        new AlertDialog.Builder(activity)
            .setIcon(android.R.drawable.ic_dialog_map)
            .setTitle(R.string.navigate_by_path)
            .setView(itemPath)
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    dialog.dismiss();
                }
            })
            .setPositiveButton(R.string.navigate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    oneDriveClient
                        .getDrive()
                        .getItems(item.id)
                        .getItemWithPath(itemPath.getText().toString())
                        .buildRequest()
                        .expand(getExpansionOptions(oneDriveClient))
                        .get(itemCallback);
                }
            })
            .create()
            .show();
    }

    /**
     * Sets the focus on one of the primary fixtures of this fragment
     *
     * @param focus The focus to appear
     * @param view the root of the fragment
     */
    private void setFocus(final ItemFocus focus, final View view) {
        ItemFocus actualFocus = focus;
        if (focus == ItemFocus.Visualization && mEmpty.get()) {
            actualFocus = ItemFocus.Empty;
        }

        for (final ItemFocus focusable : ItemFocus.values()) {
            if (focusable == actualFocus) {
                view.findViewById(focusable.mId).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(focusable.mId).setVisibility(View.GONE);
            }
        }
    }

    /**
     * Configure the SetCopyDestination menu item
     * @param item The menu item for SetCopyDestination
     */
    private void configureSetCopyDestinationMenuItem(final MenuItem item) {
        if (mItem.file != null) {
            item.setVisible(false);
        } else {
            item.setVisible(true);
            item.setChecked(false);
            if (getCopyPrefs().getString(COPY_DESTINATION_PREF_KEY, null) != null) {
                item.setChecked(true);
            }
        }
    }

    /**
     * Get the copy preferences
     * @return The copy preferences
     */
    private SharedPreferences getCopyPrefs() {
        return getActivity().getSharedPreferences("copy", Context.MODE_PRIVATE);
    }

    /**
     * The available fixtures to get focus
     */
    private enum ItemFocus {
        /**
         * The visualization pane
         */
        Visualization(android.R.id.list),

        /**
         * The json response pane
         */
        Json(R.id.json),

        /**
         * The 'empty view' pane
         */
        Empty(android.R.id.empty),

        /**
         * The in progress pane
         */
        Progress(android.R.id.progress);

        /**
         * The resource id for the item
         */
        private final int mId;

        /**
         * The default constructor
         * @param id the resource id for this item
         */
        ItemFocus(final int id) {
            mId = id;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Action when fragments are interacted with
         * @param item The item that was interacted with
         */
        void onFragmentInteraction(final DisplayItem item);
    }
}
