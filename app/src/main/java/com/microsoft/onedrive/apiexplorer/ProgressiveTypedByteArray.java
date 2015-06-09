package com.microsoft.onedrive.apiexplorer;

import java.io.IOException;
import java.io.OutputStream;

import retrofit.mime.TypedByteArray;

/**
 * Tracks progress as this byte array is written
 */
public class ProgressiveTypedByteArray extends TypedByteArray  {
    /**
     * Default buffer size
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Listens for progress of this array being written
     */
    private final ProgressListener mListener;

    /**
     * Constructs a new typed byte array.  Sets mimeType to {@code application/unknown} if absent.
     *
     * @param mimeType The mime type of this byte array
     * @param bytes The bytes that represent it
     * @param listener The progress listener
     * @throws NullPointerException if bytes are null
     */
    public ProgressiveTypedByteArray(final String mimeType, final byte[] bytes, final ProgressListener listener) {
        super(mimeType, bytes);
        this.mListener = listener;
    }

    @Override
    public void writeTo(final OutputStream out) throws IOException {
        final byte[] sourceBytes = this.getBytes();
        int writtenSoFar = 0;

        while (true) {
            final int toWrite = Math.min(DEFAULT_BUFFER_SIZE, sourceBytes.length - writtenSoFar);
            if (toWrite <= 0) {
                break;
            }
            out.write(sourceBytes, writtenSoFar, toWrite);
            writtenSoFar = writtenSoFar + toWrite;
            this.mListener.onProgress(writtenSoFar, sourceBytes.length);
        }
    }
}
