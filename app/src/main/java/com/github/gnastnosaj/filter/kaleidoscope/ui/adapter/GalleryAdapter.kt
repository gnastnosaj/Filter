package com.github.gnastnosaj.filter.kaleidoscope.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.support.annotation.Nullable
import android.support.design.widget.Snackbar
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.bilibili.socialize.share.download.IImageDownloader
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.imagepipeline.image.ImageInfo
import com.github.gnastnosaj.boilerplate.rxbus.RxBus
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.event.ToolbarEvent
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.shizhefei.mvc.IDataAdapter
import me.relex.photodraweeview.PhotoDraweeView
import timber.log.Timber
import java.io.File


class GalleryAdapter(private val context: Context) : PagerAdapter(), IDataAdapter<List<Map<String, String>>> {
    private val data = arrayListOf<Map<String, String>>()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val data = data[position]

        val photoDraweeView = PhotoDraweeView(context)

        photoDraweeView.hierarchy.setPlaceholderImage(R.drawable.ic_placeholder_dark, ScalingUtils.ScaleType.FIT_CENTER)

        photoDraweeView.controller = Fresco.newDraweeControllerBuilder()
                .setUri(data["thumbnail"])
                .setOldController(photoDraweeView.controller)
                .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                    override fun onFinalImageSet(id: String?, @Nullable imageInfo: ImageInfo?, @Nullable anim: Animatable?) {
                        photoDraweeView.isEnableDraweeMatrix = true
                        imageInfo?.let {
                            photoDraweeView.update(it.width, it.height)
                        }
                    }

                    override fun onFailure(id: String?, throwable: Throwable?) {
                        Timber.e(throwable)
                        photoDraweeView.isEnableDraweeMatrix = false
                        data["thumbnail_error"]?.let {
                            photoDraweeView.controller = Fresco.newDraweeControllerBuilder()
                                    .setUri(it)
                                    .setOldController(photoDraweeView.controller)
                                    .setControllerListener(this)
                                    .build()
                        }
                    }
                }).build()

        photoDraweeView.setOnViewTapListener { _, _, _ -> RxBus.getInstance().post(ToolbarEvent::class.java, ToolbarEvent) }

        photoDraweeView.setOnLongClickListener {
            AlertDialog.Builder(context)
                    .setMessage(R.string.save_to_phone)
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.action_save) { dialog, _ ->
                        dialog.dismiss()
                        ShareHelper.configuration?.imageDownloader?.download(context, data["thumbnail"], ShareHelper.configuration!!.getImageCachePath(context), object : IImageDownloader.OnImageDownloadListener {
                            override fun onSuccess(path: String?) {
                                Snackbar.make(photoDraweeView, context.resources.getString(R.string.save_picture_success, path), Snackbar.LENGTH_SHORT).show()
                                val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(path)))
                                context.sendBroadcast(scannerIntent)
                            }

                            override fun onFailed(error: String?) {
                                Snackbar.make(photoDraweeView, R.string.save_picture_fail, Snackbar.LENGTH_SHORT).show()
                            }

                            override fun onStart() {

                            }
                        })
                    }
                    .show()
            true
        }

        container.addView(photoDraweeView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        return photoDraweeView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun notifyDataChanged(data: List<Map<String, String>>?, isRefresh: Boolean) {
        if (isRefresh) {
            this.data.clear()
        }
        data?.let {
            this.data.addAll(it)
        }
        notifyDataSetChanged()
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    override fun getData(): List<Map<String, String>> {
        return data
    }
}