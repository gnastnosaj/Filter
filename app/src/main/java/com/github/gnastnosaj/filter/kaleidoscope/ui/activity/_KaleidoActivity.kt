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
import com.google.gson.Gson
import groovy.lang.Closure
import org.jetbrains.anko.intentFor

fun BaseActivity.preview(page: Connection?, parent: Connection?, data: List<Map<String, String>>) {
    (page as? com.github.gnastnosaj.filter.dsl.groovy.api.Connection)?.task("preview", object : Closure<MutableMap<String, Any>?>(page) {
        override fun call(vararg args: Any?): MutableMap<String, Any>? {
            var previous = false

            if (args.size > 1) {
                previous = args[1] as? Boolean ?: false
            }

            val preview = mutableMapOf<String, Any>()

            parent?.let {
                preview["parent"] = it
            }

            if (previous) {
                for (i in 1 until data.size) {
                    if (data[i]["href"] == args[0]) {
                        (parent?.execute("page", data[i - 1]["href"] as String) as? Connection)?.let {
                            preview(it, parent, data)
                            preview["page"] = it
                        }
                        preview["data"] = data[i - 1]
                        return preview
                    }
                }
            } else {
                for (i in 0 until data.size - 1) {
                    if (data[i]["href"] == args[0]) {
                        (parent?.execute("page", data[i + 1]["href"] as String) as? Connection)?.let {
                            preview(it, parent, data)
                            preview["page"] = it
                        }
                        preview["data"] = data[i + 1]
                        return preview
                    }
                }
            }

            return null
        }
    })
}

fun BaseActivity.show(view: View, data: Map<String, String>, plugin: Plugin?, connection: Connection?): Connection? {
    connection?.execute("page", data["href"]!!)?.let {
        when ((it as? com.github.gnastnosaj.filter.dsl.core.Connection)?.execute("layout")
                ?: "gallery") {
            "gallery" -> {
                val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this, view, GalleryActivity.TRANSITION_NAME
                )
                ActivityCompat.startActivity(this, intentFor<GalleryActivity>(
                        GalleryActivity.EXTRA_DATA to Gson().toJson(data),
                        GalleryActivity.EXTRA_PLUGIN to plugin,
                        GalleryActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                ), optionsCompat.toBundle())
            }
            "detail" -> {
                val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this, view.findViewById(R.id.thumbnail), DetailActivity.TRANSITION_NAME
                )
                ActivityCompat.startActivity(this, intentFor<DetailActivity>(
                        DetailActivity.EXTRA_DATA to Gson().toJson(data),
                        DetailActivity.EXTRA_PLUGIN to plugin,
                        DetailActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                ), optionsCompat.toBundle())
            }
            "webview" -> {
                ActivityCompat.startActivity(this, intentFor<WebViewPageActivity>(
                        WebViewPageActivity.EXTRA_DATA to Gson().toJson(data),
                        WebViewPageActivity.EXTRA_PLUGIN to plugin,
                        WebViewPageActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                ), null)
            }
        }

        return it as? com.github.gnastnosaj.filter.dsl.core.Connection
    }

    return null
}