package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.android.volley.toolbox.ImageLoader;
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
     * The request code for chunked upload
     */
    private static final int REQUEST_CODE_CHUNKED_UPLOAD = 8989;

    /**
     * The item id for this itme
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
        mQueryOptions.put("expand", "children(expand=thumbnails),thumbnails");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication baseApplication = (BaseApplication) getActivity().getApplication();
        final ImageLoader loader = baseApplication.getImageLoader();
        mAdapter = new DisplayItemAdapter(getActivity(), loader);

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
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mItem == null) {
            inflater.inflate(R.menu.menu_item_fragment, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mItem == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.action_copy:
                Toast.makeText(getActivity(), "Unsupported action " + item.getTitle(), Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_upload_chunked_file:
                upload(REQUEST_CODE_CHUNKED_UPLOAD);
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
    private Callback<Item> getItemCallback(final Context context) {
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
                        fragmentLabel = mItem.ParentReference.Path + "/" + mItem.Name;
                    } else {
                        fragmentLabel = mItem.Name;
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
                            adapter.add(new DisplayItem(childItem, childItem.Id));
                        }
                        getView().findViewById(android.R.id.list).setVisibility(View.VISIBLE);
                        getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                        getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void failure(final RetrofitError error) {
                if (getView() != null) {
                    getView().findViewById(android.R.id.list).setVisibility(View.GONE);
                    getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                    final TextView view = (TextView) getView().findViewById(android.R.id.empty);
                    view.setVisibility(View.VISIBLE);
                    view.setText(String.format("Error looking up this item %s", mItemId));
                }
            }
        };
    }

    /**
     * Refreshs the data for this fragment
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
                .setMessage("Are you sure you want to delete" + mItem.Name + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final BaseApplication application = (BaseApplication) getActivity().getApplication();
                        application.getOneDriveService().deleteItemId(item.Id,
                                new DefaultCallback<Response>(application) {
                            @Override
                            public void success(final Response response, final Response response2) {
                                Toast.makeText(getActivity(), "Deleted " + item.Name, Toast.LENGTH_LONG).show();
                                getActivity().onBackPressed();
                            }
                        });
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
     * Renames a item
     * @param item The item to rename
     */
    private void renameItem(final Item item) {
        final EditText newName = new EditText(getActivity());
        newName.setInputType(InputType.TYPE_CLASS_TEXT);
        newName.setHint(item.Name);
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.rename)
            .setIcon(android.R.drawable.ic_menu_edit)
            .setView(newName)
            .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    final Callback<Item> callback = new DefaultCallback<Item>(getActivity()) {
                        @Override
                        public void success(final Item item, final Response response) {
                            Toast.makeText(getActivity(), "Renamed file", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                        @Override
                        public void failure(final RetrofitError error) {
                            Toast.makeText(getActivity(), "Error renaming file " + item.Name, Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    };
                    Item updatedItem = new Item();
                    updatedItem.Id = item.Id;
                    updatedItem.Name = newName.getText().toString();
                    ((BaseApplication) getActivity().getApplication())
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
        final EditText newName = new EditText(getActivity());
        newName.setInputType(InputType.TYPE_CLASS_TEXT);
        newName.setHint("New Folder");

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.create_folder)
                .setView(newName)
                .setIcon(android.R.drawable.ic_menu_add)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final Callback<Item> callback = new DefaultCallback<Item>(getActivity()) {
                            @Override
                            public void success(final Item updatedItem, final Response response) {
                                Toast.makeText(getActivity(), "Renamed file " + updatedItem.Name, Toast.LENGTH_LONG)
                                        .show();
                                refresh();
                                dialog.dismiss();
                            }

                            @Override
                            public void failure(final RetrofitError error) {
                                super.failure(error);
                                Toast.makeText(getActivity(), "Error creating folder " + item.Name, Toast.LENGTH_LONG)
                                        .show();
                                dialog.dismiss();
                            }
                        };

                        final Item newItem = new Item();
                        newItem.Name = newName.getText().toString();
                        newItem.Folder = new Folder();

                        ((BaseApplication) getActivity().getApplication())
                                .getOneDriveService()
                                .createItemId(item.Id, newItem, callback);
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
     * Starts the uploading experience
     * @param requestCode The request code that will be used to choose simple/chunked uploading
     */
    private void upload(final int requestCode) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final BaseApplication application = (BaseApplication) getActivity().getApplication();
        final IOneDriveService oneDriveService = application.getOneDriveService();

        if (requestCode == REQUEST_CODE_SIMPLE_UPLOAD
                && data != null
                && data.getData() != null
                && data.getData().getScheme().equalsIgnoreCase("content")) {
            final AsyncTask<Void, Void, Void> uploadFile = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    try {
                        final ContentResolver contentResolver = getActivity().getContentResolver();
                        final ContentProviderClient contentProvider = contentResolver
                                .acquireContentProviderClient(data.getData());
                        final byte[] fileInMemory = FileContent.getFileBytes(contentProvider, data.getData());
                        contentProvider.release();

                        // Fix up the file name (needed for camera roll photos, etc
                        final String filename = FileContent.getValidFileName(contentResolver, data.getData());

                        oneDriveService.createItemId(mItem.Id, filename, fileInMemory,
                                new DefaultCallback<Item>(getActivity()) {
                                    @Override
                                    public void success(final Item item, final Response response) {
                                        Toast.makeText(getActivity(),
                                                "Upload " + filename + "complete!", Toast.LENGTH_LONG).show();
                                        refresh();
                                    }

                                    @Override
                                    public void failure(final RetrofitError error) {
                                        Toast.makeText(getActivity(),
                                                "Upload " + filename + "failed!", Toast.LENGTH_LONG).show();
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
        } else if (requestCode == REQUEST_CODE_CHUNKED_UPLOAD
                && data != null
                && data.getData() != null
                && data.getData().getScheme().equalsIgnoreCase("content")) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment, ChunkUploaderFragment.newInstance(mItem.Id, data.getData()))
                    .addToBackStack(null)
                    .commit();
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
