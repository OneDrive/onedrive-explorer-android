package com.microsoft.onedrivesdk;

import com.microsoft.onedrivesdk.model.Drive;
import com.microsoft.onedrivesdk.model.Item;
import com.microsoft.onedrivesdk.model.ItemReference;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Path;

/**
 * Service interface that will connect to OneDrive
 */
public interface IOneDriveService {

    /**
     * Gets the default drive
     * @param driveCallback The callback when the drive has been retrieved
     */
    @GET("/v1.0/drive")
    @Headers("Accept: application/json")
    void getDrive(final Callback<Drive> driveCallback);

    /**
     * Gets the specificed drive
     * @param driveId the id of the drive to be retrieved
     * @param driveCallback The callback when the drive has been retrieved
     */
    @GET("/v1.0/drives/{drive-id}")
    @Headers("Accept: application/json")
    void getDrive(@Path("drive-id") final String driveId, final Callback<Drive> driveCallback);

    /**
     * Gets the root of the default drive
     * @param rootCallback The callback when the root has been retrieved
     */
    @GET("/v1.0/drives/root")
    @Headers("Accept: application/json")
    void getMyRoot(final Callback<Item> rootCallback);

    /**
     * Gets an item
     * @param itemReference The reference that will be used to look the item up
     * @param itemCallback The callback when the item has been retrieved
     */
    @GET("/v1.0/drives/root/:{item-path}:/")
    @Headers("Accept: application/json")
    void getItem(@Path("item-path") final ItemReference itemReference, final Callback<Item> itemCallback);
}
