package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuItem
import com.bilibili.socialize.share.core.shareparam.ShareImage
import com.bilibili.socialize.share.core.shareparam.ShareParamImage
import com.bilibili.socialize.share.download.IImageDownloader
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.MosaicView
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.mosaicView
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.tbruyelle.rxpermissions2.RxPermissions
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.Observable
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.design.themedAppBarLayout
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.wrapContent
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


class MosaicActivity : BaseActivity() {
    private var mosaicView: MosaicView? = null

    private var url: String? = null
    private var cache: String? = null

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_URL = "url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = intent.getStringExtra(EXTRA_TITLE)
        url = intent.getStringExtra(EXTRA_URL)

        frameLayout {
            backgroundColorResource = R.color.colorPrimaryDark
            fitsSystemWindows = true
            mosaicView = mosaicView {
                url?.let {
                    ShareHelper.configuration?.imageDownloader?.download(context, it, ShareHelper.configuration!!.getImageCachePath(context), object : IImageDownloader.OnImageDownloadListener {
                        override fun onSuccess(filePath: String?) {
                            cache = filePath
                            cache?.let {
                                initMosaicView(BitmapFactory.decodeFile(it))
                                clear()
                                setEffect(MosaicView.Effect.GRID)
                                setMode(MosaicView.Mode.PATH)
                            }
                        }

                        override fun onFailed(error: String?) {
                            Timber.e(error)
                        }

                        override fun onStart() {

                        }
                    })
                }
            }.lparams(matchParent, matchParent)
            themedAppBarLayout(R.style.AppTheme_AppBarOverlay) {
                setSupportActionBar(toolbar {
                    popupTheme = R.style.AppTheme_PopupOverlay
                }.lparams(matchParent, wrapContent))
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }.lparams(matchParent, wrapContent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mosaic, menu)
        menu.findItem(R.id.action_share)?.icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18)
        menu.findItem(R.id.action_save).icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_download)
                .color(Color.WHITE).sizeDp(18)
        menu.findItem(R.id.action_clear).icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_undo)
                .color(Color.WHITE).sizeDp(18)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_share -> {
                save()?.subscribe { path ->
                    val shareParamImage = ShareParamImage(title.toString(), title.toString(), url)
                    shareParamImage.image = ShareImage(File(path))
                    ShareHelper.share(this, shareParamImage)
                }
                return true
            }
            R.id.action_save -> {
                save()?.subscribe { path ->
                    Snackbar.make(mosaicView!!, resources.getString(R.string.save_picture_success, path), Snackbar.LENGTH_SHORT).show()
                    val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(path)))
                    sendBroadcast(scannerIntent)
                }
                return true
            }
            R.id.action_clear -> {
                mosaicView?.clear()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun save(): Observable<String>? {
        mosaicView?.apply {
            return RxPermissions(this@MosaicActivity)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .flatMap { granted ->
                        if (granted) {
                            Observable.create<String> { emitter ->
                                cache?.let {
                                    val cache = File(it)
                                    if (bitmapOutput != null) {
                                        val pic = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "${cache.name}.png")
                                        val fos = FileOutputStream(pic)
                                        bitmapOutput.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                        fos.flush()
                                        fos.close()
                                        emitter.onNext(pic.absolutePath)
                                    } else {
                                        emitter.onNext(it)
                                    }
                                }
                                emitter.onComplete()
                            }.compose(RxHelper.rxSchedulerHelper())
                        } else {
                            throw IllegalStateException()
                        }
                    }
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
        }
        return null
    }
}