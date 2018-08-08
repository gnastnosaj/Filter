package com.github.gnastnosaj.filter.magneto.util

import android.content.Context
import android.net.ConnectivityManager

class NetworkUtil {
    companion object {
        fun isAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetworkInfo
            return network?.isAvailable ?: false
        }
    }
}