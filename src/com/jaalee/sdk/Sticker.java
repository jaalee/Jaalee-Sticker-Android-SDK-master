 package com.jaalee.sdk;
 
 import android.os.Parcel;
 import android.os.Parcelable;

import com.jaalee.sdk.internal.Objects;

/**
 * @author JAALEE, Inc
 * 
 * @Support dev@jaalee.com
 * @Sales: sales@jaalee.com
 * 
 * @see http://www.jaalee.com/
 */

 public class Sticker
   implements Parcelable
 {
   private final String name;
   private final String macAddress;
   private final int rssi;
   
   
   public static final Parcelable.Creator<Sticker> CREATOR = new Parcelable.Creator<Sticker>()
   {
     public Sticker createFromParcel(Parcel source) {
        return new Sticker(source);
     }
 
     public Sticker[] newArray(int size)
     {
        return new Sticker[size];
     }
    };
 
   public Sticker(String name, String macAddress, int rssi)
   {
      this.name = name;
      this.macAddress = macAddress;
      this.rssi = rssi;
   }
 
   public String getName()
   {
      return this.name;
   }
 
   public String getMacAddress()
   {
      return this.macAddress;
   }
 
   public int getRssi()
   {
	   return this.rssi;
   }
 
   public String toString()
   {
	   return Objects.toStringHelper(this).add("name", this.name).add("macAddress", this.macAddress).add("rssi", this.rssi).toString();
   }
 
   public boolean equals(Object o)
   {
	   if (this == o) return true;
	   if ((o == null) || (getClass() != o.getClass())) return false;
 
	   Sticker sticker = (Sticker)o;
	   
	   return this.macAddress.equals(sticker.macAddress);
   }
 
   private Sticker(Parcel parcel)
   {
	   this.name = parcel.readString();
	   this.macAddress = parcel.readString();
	   this.rssi = parcel.readInt();
   }
 
   public int describeContents()
   {
	   return 0;
   }
 
   public void writeToParcel(Parcel dest, int flags)
   {
	   dest.writeString(this.name);
	   dest.writeString(this.macAddress);
	   dest.writeInt(this.rssi);
   }
 }
