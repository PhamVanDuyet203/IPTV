package com.iptv.smart.player.player.streamtv.live.watch.model

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi

data class Channel(
    var name: String,
    var logoUrl: String,
    val streamUrl: String,
    val isVideo:String = "false",
    var isFavorite: Boolean = false,
    var groupTitle: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        groupTitle = parcel.readString() ?: "",
        isVideo = parcel.readString() ?: "",
    ) {
        // Read other fields if needed
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(logoUrl)
        parcel.writeString(streamUrl)
        parcel.writeString(groupTitle)
        parcel.writeString(isVideo)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Channel> {
        override fun createFromParcel(parcel: Parcel): Channel {
            return Channel(parcel)
        }

        override fun newArray(size: Int): Array<Channel?> {
            return arrayOfNulls(size)
        }
    }
}


//package com.samyak2403.iptv.model
//
//import android.os.Parcel
//import android.os.Parcelable
//
//data class Channel(
//    val name: String,
//    val logoUrl: String,
//    val streamUrl: String
//) : Parcelable {
//    constructor(parcel: Parcel) : this(
//        parcel.readString() ?: "",
//        parcel.readString() ?: "",
//        parcel.readString() ?: ""
//    )
//
//    override fun writeToParcel(parcel: Parcel, flags: Int) {
//        parcel.writeString(name)
//        parcel.writeString(logoUrl)
//        parcel.writeString(streamUrl)
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    companion object CREATOR : Parcelable.Creator<Channel> {
//        override fun createFromParcel(parcel: Parcel): Channel {
//            return Channel(parcel)
//        }
//
//        override fun newArray(size: Int): Array<Channel?> {
//            return arrayOfNulls(size)
//        }
//    }
//}
//
