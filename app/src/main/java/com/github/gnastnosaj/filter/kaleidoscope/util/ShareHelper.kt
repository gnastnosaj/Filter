package com.github.gnastnosaj.filter.kaleidoscope.util

import android.app.Activity
import android.content.Context
import com.bilibili.socialize.share.core.BiliShare
import com.bilibili.socialize.share.core.BiliShareConfiguration
import com.bilibili.socialize.share.core.SocializeListeners
import com.bilibili.socialize.share.core.SocializeMedia
import com.bilibili.socialize.share.core.shareparam.BaseShareParam
import com.bilibili.socialize.share.download.AbsImageDownloader
import com.bilibili.socialize.share.download.IImageDownloader
import com.bilibili.socialize.share.util.FileUtil
import com.facebook.binaryresource.FileBinaryResource
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import timber.log.Timber

import java.io.File
import java.io.IOException

object ShareHelper {
    private val shareListener = ShareListener()

    var configuration: BiliShareConfiguration? = null

    fun initialize(context: Context) {
        configuration = BiliShareConfiguration.Builder(context)
                .imageDownloader(ShareFrescoImageDownloader())
                .build()
        BiliShare.global().config(configuration)
    }

    fun share(activity: Activity, param: BaseShareParam) {
        BiliShare.global().share(activity, SocializeMedia.GENERIC, param, shareListener)
    }

    fun share(activity: Activity, param: BaseShareParam, shareListener: ShareListener) {
        BiliShare.global().share(activity, SocializeMedia.GENERIC, param, shareListener)
    }

    class ShareListener : SocializeListeners.ShareListener {
        override fun onStart(type: SocializeMedia) {

        }

        override fun onProgress(type: SocializeMedia, progressDesc: String) {

        }

        override fun onSuccess(type: SocializeMedia, code: Int) {

        }

        override fun onError(type: SocializeMedia, code: Int, error: Throwable) {
            Timber.e(error)
        }

        override fun onCancel(type: SocializeMedia) {

        }
    }

    class ShareFrescoImageDownloader : AbsImageDownloader() {

        override fun downloadDirectly(imageUrl: String, filePath: String, listener: IImageDownloader.OnImageDownloadListener?) {
            if (listener != null)
                listener.onStart()

            val request = ImageRequest.fromUri(imageUrl)
            val dataSource = Fresco.getImagePipeline().fetchDecodedImage(request, null)

            dataSource.subscribe(object : BaseDataSubscriber<CloseableReference<CloseableImage>>() {

                override fun onNewResultImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                    if (!dataSource.isFinished) {
                        return
                    }
                    val ref = dataSource.result
                    if (ref != null) {
                        try {
                            val imageRequest = ImageRequest.fromUri(imageUrl)
                            val cacheKey = DefaultCacheKeyFactory.getInstance()
                                    .getEncodedCacheKey(imageRequest, null)
                            val resource = Fresco.getImagePipelineFactory()
                                    .mainFileCache
                                    .getResource(cacheKey)
                            if (resource is FileBinaryResource) {
                                val cacheFile = resource.file
                                try {
                                    FileUtil.copyFile(cacheFile, File(filePath))
                                    if (listener != null) {
                                        listener.onSuccess(filePath)
                                    }
                                } catch (e: IOException) {
                                    Timber.e(e, "ShareFrescoImageDownloader exception")
                                }

                            }
                        } finally {
                            CloseableReference.closeSafely(ref)
                        }
                    } else if (listener != null) {
                        listener.onFailed(imageUrl)
                    }
                }

                override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                    if (listener != null) {
                        listener.onFailed(imageUrl)
                    }
                }

            }, UiThreadImmediateExecutorService.getInstance())
        }

    }
}