package com.microsoft.onedrive.apiexplorer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.microsoft.onedrivesdk.model.Item;
import com.microsoft.onedrivesdk.model.Thumbnail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.Inflater;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Body;

/**
 * Created by Peter Nied on 3/14/2015.
 */
public class DisplayItemAdapter extends ArrayAdapter<DisplayItem> {

    private final int mResource;
    private final LayoutInflater mInflator;
    private final Activity mContext;
    private final RequestQueue mRequestQueue;
    private final ImageLoader mImageLoader;

    public DisplayItemAdapter(final Activity context) {
        super(context, R.layout.display_item_resource);
        mContext = context;
        mResource = R.layout.display_item_resource;
        mInflator = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = mInflator.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        final DisplayItem item = getItem(position);

        final NetworkImageView icon = (NetworkImageView)view.findViewById(android.R.id.icon);

        final Thumbnail thumbnail = item.getThumbnail();
        if (thumbnail != null) {
            icon.setDefaultImageResId(android.R.drawable.ic_menu_report_image);
            icon.setImageUrl(thumbnail.Url, mImageLoader);
        }

        ((TextView)view.findViewById(android.R.id.text1)).setText(item.getItem().Name);
        ((TextView)view.findViewById(android.R.id.text2)).setText(getTypeFacets(item.getItem()));

        return view;
    }

    public String getTypeFacets(final Item item) {
        final List<String> typeFacets = new LinkedList<>();
        if (item.Folder != null) {
            typeFacets.add("Folder");
        }
        if (item.File != null) {
            typeFacets.add("File");
        }
        if (item.Audio != null) {
            typeFacets.add("Audio");
        }
        if (item.Image != null) {
            typeFacets.add("Image");
        }
        if (item.Photo != null) {
            typeFacets.add("Photo");
        }
        if (item.SpecialFolder != null) {
            typeFacets.add("SpecialFolder");
        }
        if (item.Video != null) {
            typeFacets.add("Video");
        }
        final String joiner = ", ";
        final StringBuilder sb = new StringBuilder();
        for (final String facet : typeFacets) {
            sb.append(facet);
            sb.append(joiner);
        }
        sb.delete(sb.lastIndexOf(joiner), sb.length());

        return sb.toString();
    }

    /**
     * Extract a photo from OneDrive and creates a scaled bitmap according to the device resolution, this is needed to
     * prevent memory over-allocation that can cause some devices to crash when opening high-resolution pictures
     *
     * Note: this method should not be used for downloading photos, only for displaying photos on-screen
     *
     * @param photo The source photo to download
     * @param imageStream The stream that contains the photo
     * @return Scaled bitmap representation of the photo
     * @see http://stackoverflow.com/questions/477572/strange-out-of-memory-issue-while-loading-an-image-to-a-bitmap-object/823966#823966
     */
    /*
    private Bitmap extractScaledBitmap(Thumbnail photo, InputStream imageStream) {
        Display display = mContext.getWindowManager().getDefaultDisplay();
        int IMAGE_MAX_SIZE = Math.max(display.getWidth(), display.getHeight());

        int scale = 1;
        if (photo.Height > IMAGE_MAX_SIZE  || photo.Width > IMAGE_MAX_SIZE) {
            scale = (int)Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
                    (double) Math.max(photo.Height, photo.Width)) / Math.log(0.5)));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inSampleSize = scale;
        return BitmapFactory.decodeStream(imageStream, (Rect)null, options);
    };*/
}
