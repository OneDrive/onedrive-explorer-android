package com.microsoft.onedrive.apiexplorer;

import android.app.ListFragment;
import android.net.Uri;
import android.os.Bundle;


public class ChunkUploaderFragment extends ListFragment {

    private static final String ARG_PARENT_FOLDED_ID = "ParentFolderId";
    private static final String ARG_UPLOAD_ITEM_URI = "UploadItemUri";

    private String mParentFolderId;
    private Uri mUri;

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

        setListAdapter(new ChunkUploadAdapter(getActivity()));
    }
}

