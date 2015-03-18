package com.microsoft.onedrive.apiexplorer;

import com.microsoft.onedriveaccess.model.Item;
import com.microsoft.onedriveaccess.model.Thumbnail;

import java.util.LinkedList;
import java.util.List;

/**
 * A item representing a piece of content from OneDrive.
 */
class DisplayItem {
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
     * @param item The Item
     * @param id   The internal id for the item
     */
    public DisplayItem(final Item item, final String id) {
        this.mItem = item;
        this.mId = id;
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
     * @return The first thumbnail for this item
     */
    public Thumbnail getThumbnail() {
        if (mItem.Thumbnails != null
            && !mItem.Thumbnails.isEmpty()
            && mItem.Thumbnails.get(0).Small != null
            && mItem.Thumbnails.get(0).Small.Url != null) {
            return mItem.Thumbnails.get(0).Small;
        }
        return null;
    }

    /**
     * Gets a list of the facets on this item
     * @return The list of facets
     */
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
