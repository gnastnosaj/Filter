/*
 * Copyright (c) 2018, Jason Tsang.(https://github.com/gnastnosaj) All Rights Reserved.
 */

package com.github.gnastnosaj.filter.kaleidoscope.util

import android.os.Bundle
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.sina.weibo.sdk.utils.MD5
import okio.buffer
import okio.sink
import okio.source
import java.io.File

object HistoryManager {
    private val cacheDir = File(Boilerplate.getInstance().externalCacheDir, "history")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdir()
        }
        cacheDir.listFiles { file: File ->
            System.currentTimeMillis() - file.lastModified() > 30 * 24 * 60 * 60 * 1000
        }.forEach {
            it.delete()
        }
    }

    fun save(keyword: String, history: Bundle) {
        val md5 = MD5.hexdigest(keyword)
        val cacheFile = File(cacheDir, md5)
        val byteArray = ParcelableUtil.marshall(history)
        val bufferedSink = cacheFile.sink().buffer()
        bufferedSink.write(byteArray)
        bufferedSink.flush()
    }

    fun restore(keyword: String): Bundle? {
        val md5 = MD5.hexdigest(keyword)
        val cacheFile = File(cacheDir, md5)
        return if (cacheFile.exists()) {
            val bufferedSource = cacheFile.source().buffer()
            val byteArray = bufferedSource.readByteArray()
            ParcelableUtil.unmarshall(byteArray, Bundle.CREATOR)
        } else {
            null
        }
    }
}