package com.github.gnastnosaj.filter.kaleidoscope.ui.adapter

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.bilibili.socialize.share.download.IImageDownloader
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.gnastnosaj.boilerplate.rxbus.RxBus
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.event.ToolbarEvent
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.github.piasy.biv.loader.ImageLoader
import com.github.piasy.biv.view.BigImageView
import com.github.piasy.biv.view.FrescoImageViewFactory
import com.shizhefei.mvc.IDataAdapter
import java.io.File


class GalleryAdapter(private val context: Context) : PagerAdapter(), IDataAdapter<List<Map<String, String>>> {
    private val data = arrayListOf<Map<String, String>>()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val data = data[position]
        var uri = data["thumbnail"]

        val bigImageView = BigImageView(context)
        val thumbnailError: (throwable: Throwable) -> Unit = {
            data["thumbnail_error"]?.let {
                if (uri != it) {
                    uri = it
                    bigImageView.showImage(Uri.parse(uri))
                }
            }
        }
        bigImageView.setImageViewFactory(object : FrescoImageViewFactory() {
            override fun createStillImageView(context: Context): SubsamplingScaleImageView {
                val ssiv = object : SubsamplingScaleImageView(context) {
                    override fun setOnImageEventListener(onImageEventListener: OnImageEventListener?) {
                        super.setOnImageEventListener(object : OnImageEventListener {
                            override fun onImageLoaded() {
                                onImageEventListener?.onImageLoaded()
                            }

                            override fun onReady() {
                                onImageEventListener?.onReady()
                            }

                            override fun onTileLoadError(e: Exception) {
                                thumbnailError(e)
                                onImageEventListener?.onTileLoadError(e)
                            }

                            override fun onPreviewReleased() {
                                onImageEventListener?.onPreviewReleased()
                            }

                            override fun onImageLoadError(e: Exception) {
                                thumbnailError(e)
                                onImageEventListener?.onImageLoadError(e)
                            }

                            override fun onPreviewLoadError(e: Exception) {
                                onImageEventListener?.onPreviewLoadError(e)
                            }
                        })
                    }
                }
                ssiv.maxScale = 10f
                return ssiv
            }
        })
        bigImageView.setImageLoaderCallback(object : ImageLoader.Callback {
            override fun onStart() {
            }

            override fun onFinish() {
            }

            override fun onSuccess(image: File?) {
            }

            override fun onCacheHit(imageType: Int, image: File?) {
            }

            override fun onCacheMiss(imageType: Int, image: File?) {
            }

            override fun onProgress(progress: Int) {
            }

            override fun onFail(error: Exception) {
                thumbnailError(error)
            }
        })
        bigImageView.setOnClickListener { _ -> RxBus.getInstance().post(ToolbarEvent::class.java, ToolbarEvent) }
        bigImageView.setOnLongClickListener {
            AlertDialog.Builder(context)
                    .setMessage(R.string.save_to_phone)
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.action_save) { dialog, _ ->
                        dialog.dismiss()
                        ShareHelper.configuration?.imageDownloader?.download(context, data["thumbnail"], ShareHelper.configuration!!.getImageCachePath(context), object : IImageDownloader.OnImageDownloadListener {
                            override fun onSuccess(path: String) {
                                Snackbar.make(bigImageView, context.resources.getString(R.string.save_picture_success, path), Snackbar.LENGTH_SHORT).show()
                                MediaStore.Images.Media.insertImage(context.contentResolver, path, File(path).name, "")
                            }

                            override fun onFailed(error: String) {
                                Snackbar.make(bigImageView, R.string.save_picture_fail, Snackbar.LENGTH_SHORT).show()
                            }

                            override fun onStart() {

                            }
                        })
                    }
                    .show()
            true
        }
        bigImageView.setThumbnailScaleType(BigImageView.scaleType(BigImageView.DEFAULT_IMAGE_SCALE_TYPE))

        bigImageView.showImage(Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_placeholder_dark}"), Uri.parse(uri))

        container.addView(bigImageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        return bigImageView
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