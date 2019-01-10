package com.github.gnastnosaj.filter.kaleidoscope.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.gnastnosaj.filter.kaleidoscope.ui.viewbinder.HeadViewBinder
import com.github.gnastnosaj.filter.kaleidoscope.ui.viewbinder.PostViewBinder
import com.github.gnastnosaj.filter.kaleidoscope.ui.viewbinder.ThumbnailViewBinder


class WaterfallAdapter : MultiTypeAdapter(), IDataAdapter<List<Map<String, String>>> {
    private val data = arrayListOf<Map<String, String>>()

    init {
        this.typePool = object : MultiTypePool() {
            override fun getItemViewBinder(index: Int): ItemViewBinder<*, *> {
                return if (index < size()) {
                    super.getItemViewBinder(index)
                } else {
                    object : ItemViewBinder<Any, RecyclerView.ViewHolder>() {
                        override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): RecyclerView.ViewHolder {
                            return object : RecyclerView.ViewHolder(View(parent.context)) {}
                        }

                        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Any) {
                        }
                    }
                }
            }
        }
        this.register(Map::class)
                .to(HeadViewBinder(), ThumbnailViewBinder(), PostViewBinder())
                .withKClassLinker { _, data ->
                    when ((data as? Map<String, String>)?.get("type") ?: "thumbnail") {
                        "head" -> HeadViewBinder::class
                        "post" -> PostViewBinder::class
                        else -> ThumbnailViewBinder::class
                    }
                }
    }

    override fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    override fun getData(): List<Map<String, String>> {
        return data
    }

    override fun notifyDataChanged(data: List<Map<String, String>>?, isRefresh: Boolean) {
        if (isRefresh) {
            this.data.clear()
        }
        data?.let {
            this.data.addAll(it)
        }
        items = this.data
        notifyDataSetChanged()
    }
}