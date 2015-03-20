package com.microsoft.onedrive.apiexplorer;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Methods for interacting with file contents
 */
public final class FileContent {

    /**
     * Characters that should be removed from files prior to uploading
     */
    private static final String ANSI_INVALID_CHARACTERS = "\\/:*?\"<>|";

    /**
     * Private constructor
     */
    private FileContent() {
    }

    /**
     * Gets the full bytes for a file
     * @param contentProvider The content provider
     * @param data The URI for the file
     * @return The full byte representation for the file
     * @throws IOException Any io mishaps
     * @throws RemoteException Any remote process call problems
     */
   static byte[] getFileBytes(final ContentProviderClient contentProvider, final Uri data)
            throws IOException, RemoteException {
        final ParcelFileDescriptor descriptor = contentProvider.openFile(data, "r");
        final int fileSize = (int) descriptor.getStatSize();
        return getFileBytes(contentProvider, data, 0, fileSize);
    }

    /**
     * Gets the specific offset'ed number of bytes for a file
     * @param contentProvider The content provider
     * @param data The URI for the file
     * @param offset The offset start location
     * @param size The amount of bytes to load
     * @return The full byte representation for the file
     * @throws IOException Any io mishaps
     * @throws RemoteException Any remote process call problems
     */
    static byte[] getFileBytes(final ContentProviderClient contentProvider,
                               final Uri data,
                               final int offset,
                               final int size)
            throws IOException, RemoteException {
        final ParcelFileDescriptor descriptor = contentProvider.openFile(data, "r");
        final FileInputStream fis = new FileInputStream(descriptor.getFileDescriptor());
        final ByteArrayOutputStream memorySteam = new ByteArrayOutputStream(size);
        FileContent.copyStreamContents(offset, size, fis, memorySteam);
        return memorySteam.toByteArray();
    }

    /**
     * Gets the size of a file
     * @param contentProvider The content provider
     * @param data The URI to the file
     * @return The size of the file
     * @throws FileNotFoundException If the file cannot be found
     * @throws RemoteException Any remote process call problems
     */
    static int getFileSize(final ContentProviderClient contentProvider,
                           final Uri data)
            throws FileNotFoundException, RemoteException {
        final ParcelFileDescriptor descriptor = contentProvider.openFile(data, "r");
        return (int) descriptor.getStatSize();
    }

    /**
     * Transforms a local filename from a URI into a valid filename on OneDrive + the extension
     * @param contentResolver The content resolver
     * @param data The URI for the file
     * @return The sanitized filename
     */
    static String getValidFileName(final ContentResolver contentResolver, final Uri data) {
        String fileName = removeInvalidCharacters(data.getLastPathSegment());
        if (fileName.indexOf('.') == -1) {
            final String mimeType = contentResolver.getType(data);
            final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            fileName = fileName + "." + extension;
        }
        return fileName;
    }

    /**
     * Removes invalid characters on OneDrive
     * @param fileName the file name to remove invalid characters from
     * @return The sanitized name
     */
    private static String removeInvalidCharacters(final String fileName) {
        // TODO: This is not complete as there are UNICODE specific characters that also need to be removed.
        String fixedUpString = Uri.decode(fileName);
        for (int i = 0; i < ANSI_INVALID_CHARACTERS.length(); i++) {
            fixedUpString = fixedUpString.replace(ANSI_INVALID_CHARACTERS.charAt(0), '_');
        }
        return Uri.encode(fixedUpString);
    }


    /**
     * Copies a stream around
     * @param offset The starting offset
     * @param size The size to copy
     * @param input The input source
     * @param output The output source
     * @return The number of bytes actually copied
     * @throws IOException If anything went wrong
     */
    private static int copyStreamContents(final long offset,
                                          final int size,
                                          final InputStream input,
                                          final OutputStream output)
            throws IOException {
        byte[] buffer = new byte[size];
        int count = 0;
        int n;

        final long skipAmount = input.skip(offset);
        if (skipAmount != offset) {
            throw new RuntimeException(
                    String.format("Unable to skip in the input stream actual %d, expected %d", skipAmount, offset));
        }
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
