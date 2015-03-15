package com.microsoft.onedrive.apiexplorer;

import android.content.Context;
import android.widget.ArrayAdapter;

public class ChunkUploadAdapter extends ArrayAdapter<Chunks> {

    public ChunkUploadAdapter(final Context context) {
        super(context, R.layout.chunk_item);
    }
}
