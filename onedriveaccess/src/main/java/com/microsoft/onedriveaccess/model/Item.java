// Auto-generated by OneDriveClassify on Saturday, March 21, 2015
//    from https://api.onedrive.com/v1.0/$metadata

package com.microsoft.onedriveaccess.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;

public class Item {

   @SerializedName("content")
   public Object Content;
   @SerializedName("createdBy")
   public IdentitySet CreatedBy;
   @SerializedName("createdDateTime")
   public Date CreatedDateTime;
   @SerializedName("cTag")
   public String CTag;
   @SerializedName("eTag")
   public String ETag;
   @SerializedName("id")
   public String Id;
   @SerializedName("lastModifiedBy")
   public IdentitySet LastModifiedBy;
   @SerializedName("lastModifiedDateTime")
   public Date LastModifiedDateTime;
   @SerializedName("name")
   public String Name;
   @SerializedName("parentReference")
   public ItemReference ParentReference;
   @SerializedName("size")
   public Long Size;
   @SerializedName("webUrl")
   public String WebUrl;
   @SerializedName("audio")
   public Audio Audio;
   @SerializedName("deleted")
   public Deleted Deleted;
   @SerializedName("file")
   public File File;
   @SerializedName("folder")
   public Folder Folder;
   @SerializedName("image")
   public Image Image;
   @SerializedName("location")
   public Location Location;
   @SerializedName("photo")
   public Photo Photo;
   @SerializedName("specialFolder")
   public SpecialFolder SpecialFolder;
   @SerializedName("video")
   public Video Video;
   @SerializedName("children")
   public List<Item> Children;
   @SerializedName("permissions")
   public List<Permission> Permissions;
   @SerializedName("thumbnails")
   public List<ThumbnailSet> Thumbnails;

}
