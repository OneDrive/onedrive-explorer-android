package com.microsoft.onedrive.apiexplorer;

/**
 * Represents a chunk for uploading to onedrive
 */
public class Chunk {

    /**
     * Where this chunk starts in the file
     */
    private int mStart;

    /**
     * Where this chunk ends in the file
     */
    private int mEnd;

    /**
     * The state of this chunk
     */
    private ChunkState mChunkState = ChunkState.None;

    /**
     * Default constructor
     * @param start the start point in the file
     * @param end the endpoint in the file
     */
    public Chunk(final int start, final int end) {
        mStart = start;
        mEnd = end;
    }

    /**
     * Get the start point
     * @return the start point
     */
    public int getStart() {
        return mStart;
    }

    /**
     * Get the end point
     * @return the end point
     */
    public int getEnd() {
        return mEnd;
    }

    /**
     * Get the state of the chunk
     * @return The state of the chunk
     */
    public ChunkState getStatus() {
        return mChunkState;
    }

    /**
     * Sets the state of the chunk
     * @param status The value to set the state to
     */
    public void setStatus(final ChunkState status) {
        mChunkState = status;
    }

    /**
     * Chunk states
     */
    public enum ChunkState {
        /**
         * This chunk has not been uploaded
         */
        None,

        /**
         * This chunk is being uploaded
         */
        Uploading,

        /**
         * This chunk failed to upload
         */
        Failed,

        /**
         * This chunk was successfully uploaded
         */
        Success
    }
}
