/*
 * Copyright (c) 2018, Jason Tsang.(https://github.com/gnastnosaj) All Rights Reserved.
 */

package com.github.gnastnosaj.filter.kaleidoscope.util

import android.os.Parcel
import android.os.Parcelable

object ParcelableUtil {
    fun marshall(parcelable: Parcelable): ByteArray {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        parcelable.writeToParcel(parcel, 0)
        val byteArray = parcel.marshall()
        parcel.recycle()
        return byteArray
    }

    fun <T : Parcelable> unmarshall(byteArray: ByteArray, creator: Parcelable.Creator<T>): T {
        val parcel = Parcel.obtain()
        parcel.unmarshall(byteArray, 0, byteArray.size)
        parcel.setDataPosition(0)
        val parcelable = creator.createFromParcel(parcel)
        parcel.recycle()
        return parcelable
    }
}