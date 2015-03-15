package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.microsoft.onedrivesdk.model.UploadSession;

import java.util.ArrayList;
import java.util.List;

public class ChunkUploadAdapter extends ArrayAdapter<Chunk> {

    private final Activity mContext;
    private final LayoutInflater mInflater;
    private final String mParentItemId;
    private final Uri mItemUri;
    private final RequestQueue mRequestQueue;
    private final List<Chunk> mChunks = new ArrayList<>();
    private UploadSession mSession;

    public ChunkUploadAdapter(final Activity context, final Uri itemUri, final String parentItemId) {
        super(context, R.layout.chunk_item);
        mContext = context;
        mItemUri = itemUri;
        mParentItemId = parentItemId;

        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRequestQueue = ((BaseApplication)mContext.getApplication()).getRequestQueue();

        mChunks.add(new Chunk(0, 100));
        mChunks.add(new Chunk(101, 300));
        mChunks.add(new Chunk(301, 999));
    }

    public void SetUploadSession(final UploadSession session) {
        mSession = session;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.chunk_item, parent, false);
        } else {
            view = convertView;
        }

        final Resources res = view.getResources();
        final Chunk chunk = getItem(position);

        ((TextView)view.findViewById(android.R.id.text1)).setText(res.getText(R.string.upload_chunk_label_prefix) + "" + position);
        ((TextView)view.findViewById(android.R.id.text2)).setText(res.getString(R.string.upload_chunk_range_format, chunk.getStart(), chunk.getEnd()));

        final TextView message = (TextView)view.findViewById(android.R.id.message);
        final Button button = (Button)view.findViewById(android.R.id.button1);
        final ProgressBar progressBar = (ProgressBar)view.findViewById(android.R.id.progress);

        // If there is no upload session just wait
        if (mSession != null) {
            message.setVisibility(View.INVISIBLE);
            button.setEnabled(false);
            progressBar.setVisibility(View.INVISIBLE);
        }

        switch (chunk.getStatus()) {
            case None:
                message.setVisibility(View.INVISIBLE);
                button.setEnabled(true);
                progressBar.setVisibility(View.INVISIBLE);
                break;
            case Uploading:
                message.setVisibility(View.INVISIBLE);
                button.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                break;
            case Success:
                message.setVisibility(View.VISIBLE);
                message.setText(R.string.chunk_upload_success);
                button.setEnabled(false);
                progressBar.setVisibility(View.INVISIBLE);
                break;
            case Failed:
                message.setVisibility(View.VISIBLE);
                message.setText(R.string.chunk_upload_failure);
                button.setEnabled(false);
                progressBar.setVisibility(View.INVISIBLE);
                break;
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                chunk.setStatus(Chunk.ChunkState.Uploading);
                button.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(1);

                final Request<Void> request = new Request<Void>(Request.Method.PUT, mSession.UploadUrl, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        if (error.networkResponse.statusCode == 416) {
                            chunk.setStatus(Chunk.ChunkState.Success);
                        }
                        chunk.setStatus(Chunk.ChunkState.Failed);
                        Toast.makeText(getContext(), "Bad chunk upload! Status code: " + error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
                    }
                }) {
                    @Override
                    protected Response<Void> parseNetworkResponse(final NetworkResponse response) {
                        chunk.setStatus(Chunk.ChunkState.Success);
                        return null;
                    }

                    @Override
                    protected void deliverResponse(final Void response) {
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        return new byte[100];
                    }
                };
                mRequestQueue.add(request);
            }
        });

        return view;
    }
}
