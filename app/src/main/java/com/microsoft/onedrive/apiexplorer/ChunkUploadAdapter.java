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
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a list of chunks to be uploaded to OneDrive
 */
public class ChunkUploadAdapter extends ArrayAdapter<Chunk> {

    /**
     * The status code where more chunks should be uploaded
     */
    public static final int MORE_CHUNKS_TO_UPLOAD_CODE = 416;

    /**
     * 4MB chunks
     */
    public static final int DEFAULT_CHUNK_SIZE = 4194304;

    /**
     * The current activity
     */
    private final Activity mContext;

    /**
     * The layout inflater
     */
    private final LayoutInflater mInflater;

    /**
     * The item url to be chunked and uploaded
     */
    private final Uri mItemUri;

    /**
     * The request queue to use to upload chunks to OneDrive
     */
    private final RequestQueue mRequestQueue;

    /**
     * The size of the file to upload
     */
    private final int mFileSize;

    /**
     * The upload session to use for chunked uploading
     */
    private UploadSession mSession;

    /**
     * Default constructor
     * @param context The contents of this chunked upload adapter
     * @param itemUri The url of the item to upload
     */
    public ChunkUploadAdapter(final Activity context, final Uri itemUri) {
        super(context, R.layout.chunk_item);
        mContext = context;
        mItemUri = itemUri;

        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRequestQueue = ((BaseApplication)mContext.getApplication()).getRequestQueue();

        final ContentResolver contentResolver = context.getContentResolver();
        final ContentProviderClient contentProvider = contentResolver.acquireContentProviderClient(mItemUri);
        int size;
        try {
            size = FileContent.getFileSize(contentProvider, mItemUri);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        mFileSize = size;

        int maxChunkSize = DEFAULT_CHUNK_SIZE;

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

    /**
     * Sets the upload session that this adapter will use
     * @param session The upload session
     */
    public void setUploadSession(final UploadSession session) {
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

        ((TextView)view.findViewById(android.R.id.text1))
                .setText(res.getText(R.string.upload_chunk_label_prefix) + "" + position);
        ((TextView)view.findViewById(android.R.id.text2))
                .setText(res.getString(R.string.upload_chunk_range_format, chunk.getStart(), chunk.getEnd()));

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
            case None:
            default:
                message.setVisibility(View.INVISIBLE);
                button.setEnabled(true);
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
                        chunk.setStatus(Chunk.ChunkState.Failed);
                        if (error != null && error.networkResponse != null) {
                            final String text = "Bad chunk upload! Status code: " + error.networkResponse.statusCode;
                            Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
                            if (error.networkResponse.statusCode == MORE_CHUNKS_TO_UPLOAD_CODE) {
                                chunk.setStatus(Chunk.ChunkState.Success);
                            }
                        }
                        notifyDataSetInvalidated();
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
                        final BaseApplication application = (BaseApplication) mContext.getApplication();
                        final String accessToken = application.getCredentials().getAccessToken();
                        HashMap<String, String> headers = new HashMap<>(super.getHeaders());
                        headers.put("Authorization", "Bearer " + accessToken);
                        headers.put("Content-Range", String.format("bytes %d-%d/%d", chunk.getStart(),
                                chunk.getEnd() - 1 , mFileSize));
                        for (final String key : headers.keySet()) {
                            Log.e(getClass().getSimpleName(), "Headers " + key + " " + headers.get(key));
                        }
                        return headers;
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        final ContentResolver contentResolver = getContext().getContentResolver();
                        final ContentProviderClient contentProvider = contentResolver
                                .acquireContentProviderClient(mItemUri);
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
