package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.microsoft.onedrivesdk.model.Item;
import com.microsoft.onedrivesdk.model.Thumbnail;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Peter Nied on 3/14/2015.
 */
public class DisplayItemAdapter extends ArrayAdapter<DisplayItem> {

    private final int mResource;
    private final LayoutInflater mInflater;
    private final RequestQueue mRequestQueue;
    private final ImageLoader mImageLoader;

    public DisplayItemAdapter(final Activity context) {
        super(context, R.layout.display_item_resource);
        mResource = R.layout.display_item_resource;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRequestQueue = Volley.newRequestQueue(context);
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<>(300);
            public void putBitmap(String url, Bitmap bitmap) {
                mCache.put(url, bitmap);
            }
            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }
        });
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        final DisplayItem item = getItem(position);

        final NetworkImageView icon = (NetworkImageView)view.findViewById(android.R.id.icon);

        final Thumbnail thumbnail = item.getThumbnail();
        icon.setDefaultImageResId(android.R.drawable.ic_menu_report_image);
        if (thumbnail != null) {
            icon.setImageUrl(thumbnail.Url, mImageLoader);
        }

        ((TextView)view.findViewById(android.R.id.text1)).setText(item.getItem().Name);
        ((TextView)view.findViewById(android.R.id.text2)).setText(item.getTypeFacets());

        return view;
    }
}
