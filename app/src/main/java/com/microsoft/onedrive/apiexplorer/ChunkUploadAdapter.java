package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
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
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.microsoft.onedriveaccess.model.Item;
import com.microsoft.onedriveaccess.model.UploadSession;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkUploadAdapter extends ArrayAdapter<Chunk> {

    private final Activity mContext;
    private final LayoutInflater mInflater;
    private final String mParentItemId;
    private final Uri mItemUri;
    private final RequestQueue mRequestQueue;
    private final List<Chunk> mChunks = new ArrayList<>();
    private final int mChunkSize;
    private UploadSession mSession;

    public ChunkUploadAdapter(final Activity context, final Uri itemUri, final String parentItemId) {
        super(context, R.layout.chunk_item);
        mContext = context;
        mItemUri = itemUri;
        mParentItemId = parentItemId;

        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRequestQueue = ((BaseApplication)mContext.getApplication()).getRequestQueue();

        final ContentProviderClient contentProvider = context.getContentResolver().acquireContentProviderClient(mItemUri);
        int size;
        try {
            size = FileContent.getFileSize(contentProvider, mItemUri);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        mChunkSize = size;

        int maxChunkSize = 1 * 1024 * 1024; // 4MB Chunks

        int lastChunkStart = 0;
        while (maxChunkSize < size) {
            add(new Chunk(lastChunkStart, lastChunkStart + maxChunkSize));
            size = size - maxChunkSize;
            lastChunkStart += maxChunkSize;
        }
        if (size != 0) {
            add(new Chunk(lastChunkStart, size));
        }
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

                final Request<Item> request = new Request<Item>(Request.Method.PUT, mSession.UploadUrl,
                        new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        if (error != null && error.networkResponse != null && error.networkResponse.statusCode == 416) {
                            chunk.setStatus(Chunk.ChunkState.Success);
                        }
                        chunk.setStatus(Chunk.ChunkState.Failed);
                        notifyDataSetInvalidated();
                        if (error != null && error.networkResponse != null) {
                            Toast.makeText(getContext(), "Bad chunk upload! Status code: " + error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
                        }
                    }
                }) {
                    @Override
                    protected Response<Item> parseNetworkResponse(final NetworkResponse response) {
                        chunk.setStatus(Chunk.ChunkState.Success);
                        ByteArrayInputStream bais = null;
                        try {
                            bais = new ByteArrayInputStream(response.data);
                            final JsonReader reader = new JsonReader(new InputStreamReader(bais));
                            final Gson gson = new Gson();
                            return gson.fromJson(reader, Item.class);
                        } finally {
                            if (bais != null) {
                                try {
                                    bais.close();
                                } catch (final Exception e) {
                                    Log.e(getClass().getSimpleName(), e.toString());
                                }
                            }
                        }
                    }

                    @Override
                    protected void deliverResponse(final Item response) {
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        final String accessToken = ((BaseApplication)mContext.getApplication()).getCredentials().getAccessToken();
                        HashMap<String, String> headers = new HashMap<>(super.getHeaders());
                        headers.put("Authorization", "Bearer " + accessToken);
                        headers.put("Content-Range", String.format("bytes %d-%d/%d", chunk.getStart(),
                                chunk.getEnd() - 1 , mChunkSize));
                        for(final String key : headers.keySet()) {
                            Log.e(getClass().getSimpleName(), "Headers " + key + " " + headers.get(key).toString());
                        }
                        return headers;
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        final ContentResolver contentResolver = getContext().getContentResolver();
                        final ContentProviderClient contentProvider = contentResolver.acquireContentProviderClient(mItemUri);
                        final int chunkSize = chunk.getEnd() - chunk.getStart();
                        byte[] body;
                        try {
                            body = FileContent.getFileBytes(contentProvider, mItemUri, chunk.getStart(), chunkSize);
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            contentProvider.release();
                        }
                        return body;
                    }

                    @Override
                    public Priority getPriority() {
                        return Priority.IMMEDIATE;
                    }
                };
                mRequestQueue.add(request);
            }
        });

        return view;
    }
}
