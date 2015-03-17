package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.microsoft.onedriveaccess.model.Thumbnail;

/**
 * Created by Peter Nied on 3/14/2015.
 */
public class DisplayItemAdapter extends ArrayAdapter<DisplayItem> {

    private final int mResource;
    private final LayoutInflater mInflater;
    private final ImageLoader mImageLoader;

    public DisplayItemAdapter(final Activity context, final ImageLoader imageLoader, final RequestQueue requestQueue) {
        super(context, R.layout.display_item_resource);
        mResource = R.layout.display_item_resource;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageLoader = imageLoader;
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
