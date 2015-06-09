package com.microsoft.onedrive.apiexplorer;

/**
 * Listener for progress
 */
public interface ProgressListener {

    /**
     * When progress is reported
     * @param current The current progress
     * @param max The max progress amount
     */
    void onProgress(final long current, final long max);
}
