package com.github.gnastnosaj.filter.kaleidoscope.ui.viewbinder

import android.graphics.drawable.Animatable
import android.net.Uri
import android.support.annotation.Nullable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.RatioImageView
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.ratioImageView
import me.drakeet.multitype.ItemViewBinder
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import timber.log.Timber


class ThumbnailViewBinder : ItemViewBinder<Map<*, *>, ThumbnailViewBinder.ViewHolder>() {
    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(
                with(AnkoContext.create(parent.context, parent)) {
                    cardView {
                        lparams {
                            width = matchParent
                            height = wrapContent
                            margin = dip(5)
                            isClickable = true
                            cardElevation = dip(4).toFloat()
                            radius = dip(2).toFloat()
                        }
                        linearLayout {
                            orientation = LinearLayout.VERTICAL
                            ratioImageView {
                                id = R.id.thumbnail
                                hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FIT_CENTER
                            }.lparams(matchParent, wrapContent)
                            frameLayout {
                                padding = dip(10)
                                textView {
                                    id = R.id.title
                                    textSize = 14f
                                }
                            }.lparams {
                                width = matchParent
                                height = wrapContent
                            }
                        }.lparams(matchParent, wrapContent)
                    }
                }
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Map<*, *>) {
        viewHolder.thumbnail?.apply {
            setOriginalSize(1, 1)
            (item as? Map<String, String>)?.let { data ->
                var uri = data["thumbnail"]
                uri?.let {
                    controller = Fresco.newDraweeControllerBuilder()
                            .setLowResImageRequest(ImageRequest.fromUri(Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_placeholder_light}")))
                            .setUri(it)
                            .setOldController(controller)
                            .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                                override fun onFinalImageSet(id: String?, @Nullable imageInfo: ImageInfo?, @Nullable anim: Animatable?) {
                                    imageInfo?.let {
                                        setOriginalSize(it.width, it.height, true)
                                    }
                                }

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
                }
            }
        }
        viewHolder.title?.text = (item as? Map<String, String>)?.get("title")
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var thumbnail: RatioImageView? = null

        var title: TextView? = null

        init {
            thumbnail = itemView.findViewById(R.id.thumbnail)
            title = itemView.findViewById(R.id.title)
        }
    }
}