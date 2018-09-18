/*
 * Copyright (c) 2018, Jason Tsang.(https://github.com/gnastnosaj) All Rights Reserved.
 */

package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.dsl.core.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import org.jetbrains.anko.intentFor

fun BaseActivity.start(view: View, data: Map<String, String>, plugin: Plugin?, connection: Connection?) {
    connection?.execute("page", data["href"]!!)?.let {
        when ((it as? com.github.gnastnosaj.filter.dsl.core.Connection)?.execute("layout")
                ?: "gallery") {
            "gallery" -> {
                val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this, view, GalleryActivity.TRANSITION_NAME
                )
                ActivityCompat.startActivity(this, intentFor<GalleryActivity>(
                        GalleryActivity.EXTRA_ID to (data["id"]
                                ?: data["title"]),
                        GalleryActivity.EXTRA_TITLE to data["title"],
                        GalleryActivity.EXTRA_PLUGIN to plugin,
                        GalleryActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                ), optionsCompat.toBundle())
            }
            "detail" -> {
                val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this, view.findViewById(R.id.thumbnail), DetailActivity.TRANSITION_NAME
                )
                ActivityCompat.startActivity(this, intentFor<DetailActivity>(
                        DetailActivity.EXTRA_ID to (data["id"]
                                ?: data["title"]),
                        DetailActivity.EXTRA_TITLE to data["title"],
                        DetailActivity.EXTRA_PLUGIN to plugin,
                        DetailActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                ), optionsCompat.toBundle())
            }
            "webview" -> {
                ActivityCompat.startActivity(this, intentFor<WebViewPageActivity>(
                        WebViewPageActivity.EXTRA_ID to (data["id"]
                                ?: data["title"]),
                        WebViewPageActivity.EXTRA_TITLE to data["title"],
                        WebViewPageActivity.EXTRA_THUMBNAIL to data["thumbnail"],
                        WebViewPageActivity.EXTRA_PLUGIN to plugin,
                        WebViewPageActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                ), null)
            }
        }
    }
}