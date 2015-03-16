package com.microsoft.onedrive.apiexplorer;

/**
 * Created by Peter Nied on 3/15/2015.
 */
public class Chunk {

    private int mStart;
    private int mEnd;
    private ChunkState mChunkState = ChunkState.None;

    public Chunk(final int start, final int end) {
        mStart = start;
        mEnd = end;
    }

    public int getStart(){
        return mStart;
    }

    public int getEnd(){
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
