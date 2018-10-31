/*
 * Copyright (c) 2018, Jason Tsang.(https://github.com/gnastnosaj) All Rights Reserved.
 */

package com.github.gnastnosaj.filter.kaleidoscope.ui.viewbinder

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.ratioImageView
import me.drakeet.multitype.ItemViewBinder
import org.jetbrains.anko.*
import timber.log.Timber

class PostViewBinder : ItemViewBinder<Map<*, *>, PostViewBinder.ViewHolder>() {
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(
                with(AnkoContext.create(parent.context, parent)) {
                    frameLayout {
                        lparams(matchParent, dip(128))
                        backgroundColorResource = R.color.grey_50
                        linearLayout {
                            orientation = LinearLayout.HORIZONTAL
                            linearLayout {
                                orientation = LinearLayout.VERTICAL
                                textView {
                                    id = R.id.title
                                    textSize = 16f
                                    textColorResource = R.color.grey_900
                                    maxLines = 2
                                    ellipsize = TextUtils.TruncateAt.END
                                }.lparams(matchParent, 0) {
                                    weight = 1.0f
                                }
                                textView {
                                    id = R.id.description
                                    textSize = 12f
                                    textColorResource = R.color.grey_500
                                    singleLine = true
                                    ellipsize = TextUtils.TruncateAt.END
                                }.lparams(matchParent, wrapContent)
                            }.lparams(0, matchParent) {
                                weight = 1.0f
                                setMargins(dip(16), dip(16), dip(16), dip(16))
                            }
                            ratioImageView {
                                id = R.id.thumbnail
                                aspectRatio = 1f
                                hierarchy.roundingParams = RoundingParams.fromCornersRadius(dip(5).toFloat()).setOverlayColor(resources.getColor(R.color.grey_50))
                                hierarchy.setPlaceholderImage(R.color.grey_300, ScalingUtils.ScaleType.FIT_XY)
                                hierarchy.actualImageScaleType = ScalingUtils.ScaleType.CENTER_CROP
                            }.lparams(wrapContent, matchParent) {
                                setMargins(dip(16), dip(16), dip(16), dip(16))
                            }
                        }.lparams(matchParent, matchParent)
                        view {
                            backgroundColorResource = R.color.grey_200
                        }.lparams(matchParent, 1) {
                            gravity = Gravity.BOTTOM
                            setMargins(dip(16), 0, dip(16), 0)
                        }
                    }
                }
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Map<*, *>) {
        (item as? Map<String, String>)?.let { data ->
            viewHolder.title?.text = data["title"]

            viewHolder.description?.text = if (data["author"] != null && data["publish"] == null) {
                data["author"]
            } else if (data["author"] == null && data["publish"] != null) {
                data["publish"]
            } else {
                "${data["author"]} · ${data["publish"]}"
            }

            var uri = data["thumbnail"]

            viewHolder.thumbnail?.apply {
                if (!uri.isNullOrBlank()) {
                    visibility = View.VISIBLE
                    controller = Fresco.newDraweeControllerBuilder()
                            .setUri(uri)
                            .setOldController(controller)
                            .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                                override fun onFailure(id: String?, throwable: Throwable?) {
                                    data["thumbnail_error"]?.let {
                                        if (uri != it) {
                                            uri = it
                                            controller = Fresco.newDraweeControllerBuilder()
                                                    .setUri(uri)
                                                    .setOldController(controller)
                                                    .setControllerListener(this)
                                                    .build()
                                            return
                                        }
                                    }
                                    Timber.e(throwable)
                                }
                            }).build()
                } else {
                    visibility = View.GONE
                }
            }

            viewHolder.itemView.apply {
                val lp = layoutParams
                lp.height = if (uri.isNullOrBlank()) dip(100) else dip(128)
                layoutParams = lp
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView? = null
        var description: TextView? = null
        var thumbnail: SimpleDraweeView? = null

        init {
            title = itemView.findViewById(R.id.title)
            description = itemView.findViewById(R.id.description)
            thumbnail = itemView.findViewById(R.id.thumbnail)
        }
    }
}
