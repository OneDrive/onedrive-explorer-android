package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.Folder;
import com.microsoft.onedriveaccess.model.Item;

import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
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
     * The format of the stream to use when upload files
     */
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    /**
     * The prefix for the item breadcrumb when the parent reference is unavailable
     */
    private static final String DRIVE_PREFIX = "/drive/";

    /**
     * The query option to have the OneDrive service expand out results of navigation properties
     */
    private static final String EXPAND_QUERY_OPTION_NAME = "expand";

    /**
     * Expansion options to get all children, thumbnails of children, and thumbnails
     */
    private static final String EXPAND_OPTIONS_FOR_CHILDREN_AND_THUMBNAILS = "children(expand=thumbnails),thumbnails";

    /**
     * The accepted file mime types for uploading to OneDrive
     */
    private static final String ACCEPTED_UPLOAD_MIME_TYPES = "*/*";

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
    private ListAdapter mAdapter;

    /**
     * The query options that are to be used for all items requests
     */
    private final Map<String, String> mQueryOptions = new HashMap<>();

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

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemFragment() {
        mQueryOptions.put(EXPAND_QUERY_OPTION_NAME, EXPAND_OPTIONS_FOR_CHILDREN_AND_THUMBNAILS);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new DisplayItemAdapter(getActivity());

        if (getArguments() != null) {
            mItemId = getArguments().getString(ARG_ITEM_ID);
        }

        if (mItem != null) {
            ((TextView) getActivity().findViewById(R.id.fragment_label)).setText(mItem.ParentReference.Path);
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

        refresh();

        return view;
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        mListener = (OnFragmentInteractionListener) activity;
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


            // Make sure that the root folder has certain options unavailable
            if (IOneDriveService.ROOT_FOLDER_ID.equalsIgnoreCase(mItemId)) {
                menu.findItem(R.id.action_rename).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
            }

            // Make sure that if it is a file, we don't let you perform actions that don't make sense for files
            if (mItem.File != null) {
                menu.findItem(R.id.action_create_folder).setVisible(false);
                menu.findItem(R.id.action_upload_file).setVisible(false);
                menu.findItem(R.id.action_download).setVisible(true);
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
                Toast.makeText(getActivity(),
                        getActivity().getString(R.string.unsupported_action, item.getTitle()),
                        Toast.LENGTH_LONG).show();
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
            default:
                return false;
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent,
                            final View view, final int position,
                            final long id) {
        if (null != mListener) {
            mListener.onFragmentInteraction((DisplayItem)mAdapter.getItem(position));
        }
    }

    /**
     * Creates a callback for drilling into an item
     * @param context The application context to display messages
     * @return The callback to refresh this item with
     */
    private Callback<Item> getItemCallback(final BaseApplication context) {
        return new DefaultCallback<Item>(context) {
            @Override
            public void success(final Item item, final Response response) {
                mItem = item;
                if (getView() != null) {
                    final AbsListView mListView = (AbsListView) getView().findViewById(android.R.id.list);
                    final DisplayItemAdapter adapter = (DisplayItemAdapter)mListView.getAdapter();
                    adapter.clear();

                    final String fragmentLabel;
                    if (mItem.ParentReference != null) {
                        fragmentLabel = mItem.ParentReference.Path
                                + context.getString(R.string.item_path_separator)
                                + mItem.Name;
                    } else {
                        fragmentLabel = DRIVE_PREFIX + mItem.Name;
                    }
                    ((TextView)getActivity().findViewById(R.id.fragment_label)).setText(fragmentLabel);
                    if (item.Children.isEmpty()) {
                        getView().findViewById(android.R.id.list).setVisibility(View.GONE);
                        getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                        final TextView emptyText = (TextView)getView().findViewById(android.R.id.empty);
                        if (item.Folder != null) {
                            emptyText.setText(R.string.empty_list);
                        } else {
                            emptyText.setText(R.string.empty_file);
                        }
                        emptyText.setVisibility(View.VISIBLE);

                    } else {
                        for (final Item childItem : item.Children) {
                            adapter.add(new DisplayItem(adapter,
                                                        childItem,
                                                        childItem.Id,
                                                        context.getImageCache(),
                                                        context.getHttpClient()));
                        }
                        getView().findViewById(android.R.id.list).setVisibility(View.VISIBLE);
                        getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                        getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
                    }
                    getActivity().invalidateOptionsMenu();
                }
            }

            @Override
            public void failure(final RetrofitError error) {
                if (getView() != null) {
                    getView().findViewById(android.R.id.list).setVisibility(View.GONE);
                    getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                    final TextView view = (TextView) getView().findViewById(android.R.id.empty);
                    view.setVisibility(View.VISIBLE);
                    view.setText(context.getString(R.string.item_fragment_item_lookup_error) + mItemId);
                }
            }
        };
    }

    /**
     * Refreshes the data for this fragment
     */
    private void refresh() {
        if (getView() != null) {
            getView().findViewById(android.R.id.list).setVisibility(View.GONE);
            getView().findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
            getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
        mItem = null;

        final BaseApplication app = (BaseApplication) getActivity().getApplication();
        final IOneDriveService oneDriveService = app.getOneDriveService();
        final Callback<Item> itemCallback = getItemCallback(app);
        oneDriveService.getItemId(mItemId, mQueryOptions, itemCallback);

    }

    /**
     * Deletes the item represented by this fragment
     * @param item The item to delete
     */
    private void deleteItem(final Item item) {
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.delete)
            .setIcon(android.R.drawable.ic_delete)
            .setMessage(getActivity().getString(R.string.confirm_delete_action, mItem.Name))
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    final BaseApplication application = (BaseApplication) getActivity().getApplication();
                    application.getOneDriveService().deleteItemId(item.Id,
                            new DefaultCallback<Response>(application) {
                                @Override
                                public void success(final Response response, final Response response2) {
                                    Toast.makeText(getActivity(),
                                            application.getString(R.string.deleted_this_item, item.Name),
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
     * Renames a sourceItem
     * @param sourceItem The sourceItem to rename
     */
    private void renameItem(final Item sourceItem) {
        final Activity activity = getActivity();
        final EditText newName = new EditText(activity);
        newName.setInputType(InputType.TYPE_CLASS_TEXT);
        newName.setHint(sourceItem.Name);
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
            .setTitle(R.string.rename)
            .setIcon(android.R.drawable.ic_menu_edit)
            .setView(newName)
            .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    final Callback<Item> callback = new DefaultCallback<Item>(activity) {
                        @Override
                        public void success(final Item item, final Response response) {
                            Toast.makeText(activity,
                                    activity.getString(R.string.renamed_item, sourceItem.Name, item.Name),
                                    Toast.LENGTH_LONG).show();
                            refresh();
                            dialog.dismiss();
                        }

                        @Override
                        public void failure(final RetrofitError error) {
                            Toast.makeText(activity,
                                    activity.getString(R.string.rename_error, sourceItem.Name),
                                    Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    };
                    Item updatedItem = new Item();
                    updatedItem.Id = sourceItem.Id;
                    updatedItem.Name = newName.getText().toString();
                    ((BaseApplication) activity.getApplication())
                            .getOneDriveService()
                            .updateItemId(updatedItem.Id, updatedItem, callback);
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
                .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final Callback<Item> callback = new DefaultCallback<Item>(activity) {
                            @Override
                            public void success(final Item createdItem, final Response response) {
                                Toast.makeText(activity,
                                        activity.getString(R.string.created_folder, createdItem.Name, item.Name),
                                        Toast.LENGTH_LONG)
                                        .show();
                                refresh();
                                dialog.dismiss();
                            }

                            @Override
                            public void failure(final RetrofitError error) {
                                super.failure(error);
                                Toast.makeText(activity,
                                        activity.getString(R.string.new_folder_error, item.Name),
                                        Toast.LENGTH_LONG)
                                        .show();
                                dialog.dismiss();
                            }
                        };

                        final Item newItem = new Item();
                        newItem.Name = newName.getText().toString();
                        newItem.Folder = new Folder();

                        ((BaseApplication) activity.getApplication())
                                .getOneDriveService()
                                .createItemId(item.Id, newItem, callback);
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
        final IOneDriveService oneDriveService = application.getOneDriveService();

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
            final ProgressListener progressListener = new ProgressListener() {
                @Override
                public void onProgress(final long current, final long max) {
                    dialog.setProgress((int) current);
                    dialog.setMax((int) max);
                }
            };

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
                        final ProgressiveTypedByteArray ptba = new ProgressiveTypedByteArray(
                                APPLICATION_OCTET_STREAM, fileInMemory, progressListener);
                        oneDriveService.createItemId(mItem.Id, filename, ptba,
                                new DefaultCallback<Item>(getActivity()) {
                                    @Override
                                    public void success(final Item item, final Response response) {
                                        dialog.dismiss();
                                        Toast.makeText(getActivity(),
                                                application.getString(R.string.upload_complete, item.Name),
                                                Toast.LENGTH_LONG).show();
                                        refresh();
                                    }

                                    @Override
                                    public void failure(final RetrofitError error) {
                                        dialog.dismiss();
                                        Toast.makeText(getActivity(),
                                                application.getString(R.string.upload_failed, filename),
                                                Toast.LENGTH_LONG).show();
                                        super.failure(error);
                                    }
                                });
                    } catch (final Exception e) {
                        Log.e(getClass().getSimpleName(), e.getMessage());
                        Log.e(getClass().getSimpleName(), e.toString());
                    }
                    return null;
                }
            };
            uploadFile.execute((Void) null);
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

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.Content_downloadUrl));
        request.setTitle(item.Name);
        request.setDescription(activity.getString(R.string.file_from_onedrive));
        request.allowScanningByMediaScanner();
        if (item.File != null) {
            request.setMimeType(item.File.MimeType);
        }
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadManager.enqueue(request);
        Toast.makeText(activity, activity.getString(R.string.starting_download_message), Toast.LENGTH_LONG).show();
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
