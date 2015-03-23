// Auto-generated by OneDriveClassify on Monday, March 23, 2015
//    from https://api.onedrive.com/v1.0/$metadata

package com.microsoft.onedriveaccess.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Drive {

   @SerializedName("id")
   public String Id;
   @SerializedName("driveType")
   public String DriveType;
   @SerializedName("owner")
   public IdentitySet Owner;
   @SerializedName("quota")
   public Quota Quota;
   @SerializedName("items")
   public List<Item> Items;
   @SerializedName("shared")
   public List<Item> Shared;
   @SerializedName("special")
   public List<Item> Special;

}
