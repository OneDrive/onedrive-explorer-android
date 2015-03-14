package com.microsoft.onedrive.apiexplorer;

import com.microsoft.onedrivesdk.model.Item;

/**
 * A item representing a piece of content from OneDrive.
 */
class DisplayItem {
    public Item mItem;
    public String mId;

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
     * ToString() implementation
     *
     * @return The name of the item
     */
    @Override
    public String toString() {
        return mItem.Name;
    }
}
