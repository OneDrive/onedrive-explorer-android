package com.microsoft.onedrive.apiexplorer;

import com.microsoft.onedriveaccess.model.Item;
import com.microsoft.onedriveaccess.model.Thumbnail;

import java.util.LinkedList;
import java.util.List;

/**
 * A item representing a piece of content from OneDrive.
 */
class DisplayItem {
    private Item mItem;
    private String mId;

    /**
     * Default Constructor
     *
     * @param item The Item
     * @param id   The internal id for the item
     */
    public DisplayItem(final Item item, final String id) {
        this.mItem = item;
        this.mId = id;
    }

    public String getId() {
        return mId;
    }

    public Item getItem() {
        return mItem;
    }

    public Thumbnail getThumbnail() {
        if (mItem.Thumbnails != null
            && !mItem.Thumbnails.isEmpty()
            && mItem.Thumbnails.get(0).Small != null
            && mItem.Thumbnails.get(0).Small.Url != null) {
            return mItem.Thumbnails.get(0).Small;
        }
        return null;
    }

    public String getTypeFacets() {
        final List<String> typeFacets = new LinkedList<>();
        if (mItem.Folder != null) {
            typeFacets.add("Folder");
        }
        if (mItem.File != null) {
            typeFacets.add("File");
        }
        if (mItem.Audio != null) {
            typeFacets.add("Audio");
        }
        if (mItem.Image != null) {
            typeFacets.add("Image");
        }
        if (mItem.Photo != null) {
            typeFacets.add("Photo");
        }
        if (mItem.SpecialFolder != null) {
            typeFacets.add("SpecialFolder");
        }
        if (mItem.Video != null) {
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
     * ToString() implementation
     *
     * @return The name of the item
     */
    @Override
    public String toString() {
        return mItem.Name;
    }
}
