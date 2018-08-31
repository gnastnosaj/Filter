package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.view.ViewCompat
import android.view.MenuItem
import com.facebook.drawee.drawable.ScalingUtils
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.dsl.groovy.api.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.datasource.ConnectionDataSource
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.StarApi
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.RatioImageView
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.crescentoContainer
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.ratioImageView
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.collapsingToolbarLayout
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.themedAppBarLayout
import org.jetbrains.anko.support.v4.nestedScrollView

class DetailActivity : BaseActivity() {

    private var id: String? = null
    private var plugin: Plugin? = null
    private var connection: Connection? = null

    private var entrance: String? = null
    private var starApi: StarApi? = null
    private var star: Boolean = false

    private var cover: RatioImageView? = null

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PLUGIN = "plugin"
        const val EXTRA_CONNECTION_HASH_CODE = "connectionHashCode"

        const val TRANSITION_NAME = "transitionName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getStringExtra(GalleryActivity.EXTRA_ID)
        title = intent.getStringExtra(GalleryActivity.EXTRA_TITLE)
        plugin = intent.getParcelableExtra(GalleryActivity.EXTRA_PLUGIN)
        plugin?.let {
            starApi = StarApi(it)
        }
        connection = Kaleidoscope.restoreInstanceState(intent.getIntExtra(GalleryActivity.EXTRA_CONNECTION_HASH_CODE, -1))
        if (connection == null) {
            savedInstanceState?.apply {
                val hashCode = getInt(GalleryActivity.EXTRA_CONNECTION_HASH_CODE)
                connection = Kaleidoscope.restoreInstanceState(hashCode)
            }
        }
        connection?.let {
            entrance = it.execute("entrance") as? String
            //tagEventObservable = RxBus.getInstance().register(it, TagEvent::class.java)
        }

        coordinatorLayout {
            themedAppBarLayout(R.style.AppTheme_AppBarOverlay) {
                backgroundColorResource = R.color.transparent
                collapsingToolbarLayout {
                    isTitleEnabled = false
                    setContentScrimResource(R.color.colorPrimary)
                    relativeLayout {
                        crescentoContainer {
                            cover = ratioImageView {
                                hierarchy.setPlaceholderImage(R.drawable.slogan_dark)
                                hierarchy.actualImageScaleType = ScalingUtils.ScaleType.CENTER_CROP
                            }.lparams(matchParent, wrapContent)
                            cover?.setOriginalSize(1, 1)
                            ViewCompat.setElevation(this, dip(5).toFloat())
                        }.lparams(matchParent, wrapContent) {
                            bottomMargin = dip(10)
                        }
                    }.lparams(matchParent, wrapContent) {
                        collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PARALLAX
                        parallaxMultiplier = 0.7f
                    }
                    setSupportActionBar(toolbar {
                        popupTheme = R.style.AppTheme_PopupOverlay
                    }.lparams(matchParent, wrapContent) {
                        collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PIN
                    })
                }.lparams(matchParent, wrapContent) {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                }
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }.lparams(matchParent, wrapContent)
            nestedScrollView {

            }.lparams(matchParent, matchParent)
        }

        connection?.let {
            val connectionDataSource = ConnectionDataSource(this, it)
            connectionDataSource
                    .refresh()
                    .compose(RxHelper.rxSchedulerHelper())
                    .compose(bindToLifecycle())
                    .subscribe {
                        it.firstOrNull()?.let {
                            it["cover"]?.let {
                                cover?.setImageURI(it)
                            }
                        }
                    }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        connection?.let {
            outState?.putInt(EXTRA_CONNECTION_HASH_CODE, Kaleidoscope.saveInstanceState(it))
        }
    }
}