// ------------------------------------------------------------------------------
// Copyright (c) 2015 Microsoft Corporation
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
// ------------------------------------------------------------------------------

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
        item.resumeThumbnailDownload();

        ((TextView)view.findViewById(android.R.id.text1)).setText(item.getItem().name);
        ((TextView)view.findViewById(android.R.id.text2)).setText(item.getTypeFacets());
        final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        final Bitmap image = item.getImage();
        if (image != null) {
            imageView.setImageBitmap(image);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }
        imageView.setContentDescription(getContext().getString(R.string.thumbnail_description, item.getItem().name));

        return view;
    }

    /**
     * Stop issuing requests for thumbnails contained within this Adapter
     */
    public void stopDownloadingThumbnails() {
        for (int i = 0; i < getCount(); i++) {
            getItem(i).cancelThumbnailDownload();
        }
    }
}
