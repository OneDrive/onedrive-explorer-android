package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.onedrivesdk.IOneDriveService;
import com.microsoft.onedrivesdk.model.Folder;
import com.microsoft.onedrivesdk.model.Item;

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
    private static final String ARG_ITEM_ID = "itemId";
    private String mItemId;
    private Item mItem;
    private OnFragmentInteractionListener mListener;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;
    private final Map<String, String> mQueryOptions = new HashMap<>();

    // TODO: Rename and change types of parameters
    public static ItemFragment newInstance(final String itemId) {
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
        mQueryOptions.put("expand", "children");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1);

        if (getArguments() != null) {
            mItemId = getArguments().getString(ARG_ITEM_ID);
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
            case R.id.action_upload_chunked_file:
            case R.id.action_upload_file:
                Toast.makeText(getActivity(), "Unsupported action " + item.getTitle(), Toast.LENGTH_LONG).show();
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

    /**
     * Creates a callback for drilling into an item
     * @return The callback to refresh this item with
     */
    private Callback<Item> getItemCallback() {
        return new DefaultCallback<Item>() {
            @Override
            public void success(final Item item, final Response response) {
                mItem = item;
                if (getView() != null) {
                    final AbsListView mListView = (AbsListView) getView().findViewById(android.R.id.list);
                    final ArrayAdapter<DisplayItem> adapter = (ArrayAdapter<DisplayItem>)mListView.getAdapter();
                    adapter.clear();
                    for (final Item childItem : item.Children) {
                        adapter.add(new DisplayItem(childItem, childItem.Id));
                    }
                    getView().findViewById(android.R.id.list).setVisibility(View.VISIBLE);
                    getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                    getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
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

    private void refresh() {
        if (getView() != null) {
            getView().findViewById(android.R.id.list).setVisibility(View.GONE);
            getView().findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
            getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
        mItem = null;

        final BaseApplication app = (BaseApplication) getActivity().getApplication();
        final IOneDriveService oneDriveService = app.getOneDriveService();
        final Callback<Item> itemCallback = getItemCallback();
        oneDriveService.getItemId(mItemId, mQueryOptions, itemCallback);

    }

    private void deleteItem(final Item item) {
        ((BaseApplication) getActivity().getApplication()).getOneDriveService().deleteItemId(item.Id, new DefaultCallback<Response>() {
            @Override
            public void success(final Response response, final Response response2) {
                Toast.makeText(getActivity(), "Deleted " + item.Name, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renameItem(final Item item) {
        final EditText newName = new EditText(getActivity());
        newName.setInputType(InputType.TYPE_CLASS_TEXT);
        newName.setHint(item.Name);
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.rename)
            .setView(newName)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    final Callback<Item> callback = new DefaultCallback<Item>() {
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
                    item.Name = newName.getText().toString();
                    ((BaseApplication) getActivity().getApplication())
                            .getOneDriveService()
                            .updateItemId(item.Id, item, callback);
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

    private void createFolder(final Item item) {
        final EditText newName = new EditText(getActivity());
        newName.setInputType(InputType.TYPE_CLASS_TEXT);
        newName.setHint("New Folder");

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.rename)
                .setView(newName)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final Callback<Item> callback = new DefaultCallback<Item>() {
                            @Override
                            public void success(final Item item, final Response response) {
                                Toast.makeText(getActivity(), "Renamed file", Toast.LENGTH_LONG).show();
                                refresh();
                                dialog.dismiss();
                            }
                            @Override
                            public void failure(final RetrofitError error) {
                                Toast.makeText(getActivity(), "Error creating folder " + item.Name, Toast.LENGTH_LONG).show();
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

}
