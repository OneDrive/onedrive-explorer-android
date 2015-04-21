package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Array adapter for display items
 */
public class DisplayItemAdapter extends ArrayAdapter<DisplayItem> {

    /**
     * The layout inflater
     */
    private final LayoutInflater mInflater;

    /**
     * Default constructor
     * @param context The context of this adapter
     */
    public DisplayItemAdapter(final Activity context) {
        super(context, R.layout.display_item_resource);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.display_item_resource, parent, false);
        } else {
            view = convertView;
        }

        final DisplayItem item = getItem(position);

        ((TextView)view.findViewById(android.R.id.text1)).setText(item.getItem().Name);
        ((TextView)view.findViewById(android.R.id.text2)).setText(item.getTypeFacets());
        final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        final Bitmap image = item.getImage();
        if (image != null) {
            imageView.setImageBitmap(image);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }
        imageView.setContentDescription(getContext().getString(R.string.thumbnail_description, item.getItem().Name));


        return view;
    }
}
