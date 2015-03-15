package com.microsoft.onedrive.apiexplorer;

/**
 * Created by Peter Nied on 3/15/2015.
 */
public class Chunk {

    private long mStart;
    private long mEnd;
    private ChunkState mChunkState = ChunkState.None;

    public Chunk(final long start, final long end) {
        mStart = start;
        mEnd = end;
    }

    public long getStart(){
        return mStart;
    }

    public long getEnd(){
        return mEnd;
    }

    public ChunkState getStatus() {
        return mChunkState;
    }

    public void setStatus(final ChunkState status) {
        mChunkState = status;
    }

    public enum ChunkState {
        None,
        Uploading,
        Failed,
        Success
    }
}
