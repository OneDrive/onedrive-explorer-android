package com.microsoft.onedrive.apiexplorer;

import com.microsoft.onedrivesdk.model.Item;
import com.microsoft.onedrivesdk.model.Thumbnail;

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
            && mItem.Thumbnails.get(0).Medium != null
            && mItem.Thumbnails.get(0).Medium.Url != null) {
            return mItem.Thumbnails.get(0).Medium;
        }
        return null;
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
