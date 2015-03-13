package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.microsoft.onedrive.apiexplorer.dummy.DummyContent;
import com.microsoft.onedrivesdk.IOneDriveService;
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
public class FolderFragment extends Fragment implements AbsListView.OnItemClickListener {
    private static final String ARG_FOLDER_ID = "folderId";
    private String mFolderId;
    private OnFragmentInteractionListener mListener;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;
    private final Map<String,String> mQueryOptions = new HashMap<>();

    // TODO: Rename and change types of parameters
    public static FolderFragment newInstance(final String folderId) {
        FolderFragment fragment = new FolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FOLDER_ID, folderId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FolderFragment() {
        mQueryOptions.put("expand", "children");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Change Adapter to display your content
        final ArrayAdapter<DummyContent.DummyItem> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, DummyContent.ITEMS);
        mAdapter = adapter;

        if (getArguments() != null) {
            mFolderId = getArguments().getString(ARG_FOLDER_ID);
            final BaseApplication app = (BaseApplication) getActivity().getApplication();
            final IOneDriveService oneDriveService = app.getOneDriveService();
            final Callback<Item> itemCallback = getItemCallback(adapter);
            oneDriveService.getItemId(mFolderId, mQueryOptions, itemCallback);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folder, container, false);

        // Set the adapter
        /*
      The fragment's ListView/GridView.
     */
        final AbsListView mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
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
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

    private Callback<Item> getItemCallback(final ArrayAdapter<DummyContent.DummyItem> adapter) {
        return new Callback<Item>() {
            @Override
            public void success(final Item item, final Response response) {
                adapter.clear();
                for (final Item childItem : item.Children) {
                    adapter.add(new DummyContent.DummyItem(childItem.Id, childItem.Name));
                }
                getView().findViewById(android.R.id.list).setVisibility(View.VISIBLE);
                getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
            }

            @Override
            public void failure(RetrofitError error) {
                getView().findViewById(android.R.id.list).setVisibility(View.GONE);
                getView().findViewById(android.R.id.progress).setVisibility(View.GONE);
                final TextView view = (TextView) getView().findViewById(android.R.id.empty);
                view.setVisibility(View.VISIBLE);
                view.setText(String.format("Error looking up this folder %s", mFolderId));
            }
        };
    }
}
