package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.onedriveaccess.model.Thumbnail;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

/**
 * Array adapter for display items
 */
public class DisplayItemAdapter extends ArrayAdapter<DisplayItem> {

    /**
     * The layout inflater
     */
    private final LayoutInflater mInflater;

    /**
     * The image cache
     */
    private final LruCache<String, Bitmap> mImageCache;

    /**
     * The http client
     */
    private final HttpClient mHttpClient;

    /**
     * Default constructor
     * @param context The context of this adapter
     * @param imageLoader The image loader for thumbnails
     * @param httpClient The http client for downloading thumbnails
     */
    public DisplayItemAdapter(final Activity context, final LruCache<String, Bitmap> imageLoader,
                              final HttpClient httpClient) {
        super(context, R.layout.display_item_resource);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageCache = imageLoader;
        mHttpClient = httpClient;
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


        final ImageView icon = (ImageView)view.findViewById(android.R.id.icon);

        final Thumbnail thumbnail = item.getThumbnail();
        icon.setImageResource(android.R.drawable.ic_menu_report_image);

        if (thumbnail != null && thumbnail.Url != null) {
            new AsyncTask<String, Void, Bitmap>(){

                @Override
                protected Bitmap doInBackground(String... params) {
                    final Bitmap foundImage = mImageCache.get(params[0]);
                    if (foundImage != null) {
                        return foundImage;
                    }

                    final HttpGet thumbnailRequest = new HttpGet();
                    try {
                        final HttpResponse response = mHttpClient.execute(thumbnailRequest);
                        final HttpEntity entity = response.getEntity();
                        return BitmapFactory.decodeStream(entity.getContent());
                    } catch (IOException ioe) {
                        Log.e(getClass().getSimpleName(), "Error downloading thumbnail " + params[0]);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(final Bitmap image) {
                    icon.setImageBitmap(image);
                }
            }.execute(thumbnail.Url);
        }

        ((TextView)view.findViewById(android.R.id.text1)).setText(item.getItem().Name);
        ((TextView)view.findViewById(android.R.id.text2)).setText(item.getTypeFacets());

        return view;
    }
}
