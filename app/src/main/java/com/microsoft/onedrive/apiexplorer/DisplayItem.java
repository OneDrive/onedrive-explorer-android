package com.microsoft.onedrive.apiexplorer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import com.microsoft.onedriveaccess.model.Item;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * A item representing a piece of content from OneDrive.
 */
class DisplayItem {

    /**
     * The item factory that created this item
     */
    private final LruCache<String, Bitmap> mImageCache;

    /**
     * The actual backing item instance
     */
    private Item mItem;

    /**
     * The id for this display item
     */
    private String mId;

    /**
     * Default Constructor
     *
     * @param adapter The adapter for this item
     * @param item The Item
     * @param id   The internal id for the item
     * @param imageCache The thumbnail image cache
     * @param httpClient The http client
     */
    public DisplayItem(final DisplayItemAdapter adapter,
                       final Item item,
                       final String id,
                       final LruCache<String, Bitmap> imageCache,
                       final HttpClient httpClient) {
        mImageCache = imageCache;
        mItem = item;
        mId = id;

        if (getThumbnailUrl() != null) {
            new AsyncTask<Void, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(final Void... params) {
                    final Bitmap foundImage = imageCache.get(getThumbnailUrl());
                    if (foundImage != null) {
                        return foundImage;
                    }

                    final HttpGet thumbnailRequest = new HttpGet(getThumbnailUrl());
                    try {
                        final HttpResponse response = httpClient.execute(thumbnailRequest);
                        final HttpEntity entity = response.getEntity();
                        return BitmapFactory.decodeStream(entity.getContent());
                    } catch (final IOException ioe) {
                        Log.e(getClass().getSimpleName(), "Error downloading thumbnail " + getThumbnailUrl());
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(final Bitmap image) {
                    if (image != null) {
                        imageCache.put(getThumbnailUrl(), image);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
            .execute((Void)null);
        }
    }

    /**
     * The item id
     * @return the item id
     */
    public String getId() {
        return mId;
    }

    /**
     * The backing item instance
     * @return The item instance
     */
    public Item getItem() {
        return mItem;
    }

    /**
     * Gets a thumbnail used for visualization
     * @return The first thumbnail url for this item
     */
    public String getThumbnailUrl() {
        if (mItem.Thumbnails != null
            && !mItem.Thumbnails.isEmpty()
            && mItem.Thumbnails.get(0).Small != null
            && mItem.Thumbnails.get(0).Small.Url != null) {
            return mItem.Thumbnails.get(0).Small.Url;
        }
        return null;
    }

    /**
     * Gets a list of the facets on this item
     * @return The list of facets
     */
    public String  getTypeFacets() {
        final List<String> typeFacets = new LinkedList<>();
        if (mItem.Folder != null) {
            typeFacets.add(mItem.Folder.getClass().getSimpleName());
        }
        if (mItem.File != null) {
            typeFacets.add(mItem.File.getClass().getSimpleName());
        }
        if (mItem.Audio != null) {
            typeFacets.add(mItem.Audio.getClass().getSimpleName());
        }
        if (mItem.Image != null) {
            typeFacets.add(mItem.Image.getClass().getSimpleName());
        }
        if (mItem.Photo != null) {
            typeFacets.add(mItem.Photo.getClass().getSimpleName());
        }
        if (mItem.SpecialFolder != null) {
            typeFacets.add(mItem.SpecialFolder.getClass().getSimpleName());
        }
        if (mItem.Video != null) {
            typeFacets.add(mItem.Video.getClass().getSimpleName());
        }
        final String joiner = ", ";
        final StringBuilder sb = new StringBuilder();
        for (final String facet : typeFacets) {
            sb.append(facet);
            sb.append(joiner);
        }

        final int lastIndexOfJoiner = sb.lastIndexOf(joiner);
        if (lastIndexOfJoiner != -1) {
            sb.delete(lastIndexOfJoiner, sb.length());
        }

        return sb.toString();
    }

    /**
     * ToString() implementation
     *
     * @return The name of the item
     */
    @Override
    public String toString() {
        return mItem.Name;
    }

    /**
     * The image for this display item
     * @return The image, or null if non was found
     */
    public Bitmap getImage() {
        final String url = getThumbnailUrl();
        if (url != null && mImageCache.get(url) != null) {
            return mImageCache.get(url);
        }
        return null;
    }
}
