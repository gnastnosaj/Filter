package com.github.gnastnosaj.filter.kaleidoscope.api.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson

class Plugin : Parcelable {
    var id: String? = null
    var name: String? = null
    var ico: String? = null
    var www: String? = null
    var args: Map<String, Any>? = null
    var script: String? = null

    override fun equals(other: Any?): Boolean {
        other?.let {
            if (it is Plugin) {
                return it.id.equals(this.id)
            }
        }
        return false
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        this.writeString(Gson().toJson(this@Plugin))
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Plugin> = object : Parcelable.Creator<Plugin> {
            override fun createFromParcel(source: Parcel): Plugin = Gson().fromJson<Plugin>(source.readString(), Plugin::class.java)
            override fun newArray(size: Int): Array<Plugin?> = arrayOfNulls(size)
        }
    }
}