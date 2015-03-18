package com.microsoft.onedrive.apiexplorer;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;

import com.microsoft.onedriveaccess.IOneDriveService;
import com.microsoft.onedriveaccess.model.UploadSession;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * For a file, chunks it up and makes those chunks available to upload
 */
public class ChunkUploaderFragment extends ListFragment {

    /**
     * The parameter id for the parent folder id
     */
    private static final String ARG_PARENT_FOLDED_ID = "ParentFolderId";

    /**
     * The parameter id for the uploading item url
     */
    private static final String ARG_UPLOAD_ITEM_URI = "UploadItemUri";

    /**
     * The parent id
     */
    private String mParentFolderId;

    /**
     * The item url
     */
    private Uri mUri;

    /**
     * Creates an instance of the ChunkUploadFragment
     * @param parentFolderId The parent id that this item should be uploaded into
     * @param uploadItemUri The url of the item on device that should be uploaded
     * @return The new chunk upload fragment
     */
    public static ChunkUploaderFragment newInstance(final String parentFolderId, final Uri uploadItemUri) {
        final ChunkUploaderFragment fragment = new ChunkUploaderFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_PARENT_FOLDED_ID, parentFolderId);
        args.putParcelable(ARG_UPLOAD_ITEM_URI, uploadItemUri);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChunkUploaderFragment() {
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParentFolderId = getArguments().getString(ARG_PARENT_FOLDED_ID);
            mUri = getArguments().getParcelable(ARG_UPLOAD_ITEM_URI);
        }

        final ChunkUploadAdapter chunkUploadAdapter = new ChunkUploadAdapter(getActivity(), mUri);
        setListAdapter(chunkUploadAdapter);

        final String filename = FileContent.getValidFileName(getActivity().getContentResolver(), mUri);

        final ProgressDialog dialog = ProgressDialog.show(getActivity(), "Creating session",
                "Connecting to OneDrive to create the upload session", true, true);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                final BaseApplication application = (BaseApplication) getActivity().getApplication();
                final IOneDriveService oneDriveService = application.getOneDriveService();
                oneDriveService.createUploadSession(mParentFolderId, filename, new DefaultCallback<UploadSession>() {
                    @Override
                    public void success(final UploadSession session, final Response response) {
                        chunkUploadAdapter.setUploadSession(session);
                        dialog.dismiss();
                    }

                    @Override
                    public void failure(final RetrofitError error) {
                        super.failure(error);
                    }
                });
                return null;
            }
        }.execute((Void) null);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chunk_list, container, false);
        final AbsListView mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(getListAdapter());

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.action_get_details) {
            Toast.makeText(getActivity(), "Unsupported action " + item.getTitle(), Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

