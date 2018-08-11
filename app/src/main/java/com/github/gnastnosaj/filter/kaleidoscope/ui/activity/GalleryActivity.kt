package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.view.ViewPager
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import com.bilibili.socialize.share.core.shareparam.ShareImage
import com.bilibili.socialize.share.core.shareparam.ShareParamImage
import com.github.gnastnosaj.boilerplate.mvchelper.ViewPagerViewHandler
import com.github.gnastnosaj.boilerplate.rxbus.RxBus
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.dsl.groovy.api.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.datasource.ConnectionDataSource
import com.github.gnastnosaj.filter.kaleidoscope.api.event.TagEvent
import com.github.gnastnosaj.filter.kaleidoscope.api.event.ToolbarEvent
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Star
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.StarApi
import com.github.gnastnosaj.filter.kaleidoscope.api.search.search
import com.github.gnastnosaj.filter.kaleidoscope.ui.adapter.GalleryAdapter
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.tagGroup
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.shizhefei.mvc.ILoadViewFactory
import com.shizhefei.mvc.MVCHelper
import com.shizhefei.mvc.MVCNormalHelper
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import me.gujun.android.taggroup.TagGroup
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.themedAppBarLayout
import org.jetbrains.anko.support.v4.viewPager
import java.lang.Exception


class GalleryActivity : BaseActivity() {
    private var appBar: AppBarLayout? = null
    private var menu: Menu? = null
    private var progressBar: ProgressBar? = null
    private var viewPager: ViewPager? = null
    private var tagGroup: TagGroup? = null

    private var isAppBarHidden: Boolean = false
    private var searchDisposable: Disposable? = null
    private var galleryAdapter: GalleryAdapter? = null
    private var tagEventObservable: Observable<TagEvent>? = null

    private var id: String? = null
    private var plugin: Plugin? = null
    private var connection: Connection? = null

    private var entrance: String? = null
    private var starApi: StarApi? = null
    private var star: Boolean = false

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PLUGIN = "plugin"
        const val EXTRA_CONNECTION_HASH_CODE = "connectionHashCode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getStringExtra(EXTRA_ID)
        title = intent.getStringExtra(EXTRA_TITLE)
        plugin = intent.getParcelableExtra(EXTRA_PLUGIN)
        plugin?.let {
            starApi = StarApi(it)
        }
        connection = Kaleidoscope.restoreInstanceState(intent.getIntExtra(EXTRA_CONNECTION_HASH_CODE, -1))
        connection?.let {
            entrance = it.execute("entrance") as? String
            tagEventObservable = RxBus.getInstance().register(it, TagEvent::class.java)
        }

        frameLayout {
            backgroundColorResource = R.color.colorPrimaryDark
            fitsSystemWindows = true
            frameLayout {
                galleryAdapter = GalleryAdapter(context)
                viewPager = viewPager().lparams(matchParent, matchParent)
            }
            appBar = themedAppBarLayout(R.style.AppTheme_AppBarOverlay) {
                setSupportActionBar(toolbar {
                    popupTheme = R.style.AppTheme_PopupOverlay
                }.lparams(matchParent, wrapContent))
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                progressBar = horizontalProgressBar(R.style.Widget_AppCompat_ProgressBar_Horizontal) {
                    scaleY = 0.5f
                    isIndeterminate = true
                    visibility = View.GONE
                }.lparams(matchParent, wrapContent)
            }.lparams(matchParent, wrapContent)
            tagGroup = tagGroup(theme = R.style.TagGroup_Beauty_Red_Inverse) {
                tagEventObservable?.apply {
                    compose(RxHelper.rxSchedulerHelper())
                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                            .subscribe { tagEvent ->
                                setTags(tagEvent.tags.keys.toList())
                                setOnTagClickListener { tag ->
                                    tagEvent.tags[tag]?.apply {
                                        startActivity(Intent(intentFor<WaterfallActivity>(WaterfallActivity.EXTRA_TITLE to tag, WaterfallActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(this))))
                                    }
                                }
                            }
                }
            }.lparams {
                width = matchParent
                height = wrapContent
                gravity = Gravity.BOTTOM
            }
        }

        val mvcHelper = MVCNormalHelper<List<Map<String, String>>>(viewPager, MVCHelper.loadViewFactory.madeLoadView(), object : ILoadViewFactory.ILoadMoreView {
            override fun showFail(e: Exception?) {
                progressBar?.visibility = View.GONE
            }

            override fun showLoading() {
                progressBar?.visibility = View.VISIBLE
            }

            override fun showNomore() {
                progressBar?.visibility = View.GONE
            }

            override fun init(footViewHolder: ILoadViewFactory.FootViewAdder?, onClickLoadMoreListener: View.OnClickListener?) {

            }

            override fun showNormal() {
                progressBar?.visibility = View.GONE
            }

        })
        connection?.let {
            val dataSource = ConnectionDataSource(this@GalleryActivity, it)
            mvcHelper.setDataSource(dataSource)
            mvcHelper.setAdapter(galleryAdapter, ViewPagerViewHandler())
            mvcHelper.refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        ToolbarEvent.observable
                .compose(RxHelper.rxSchedulerHelper())
                .compose(bindToLifecycle())
                .subscribe {
                    appBar?.apply {
                        animate().translationY(if (isAppBarHidden) 0f else -height.toFloat())
                                .setInterpolator(DecelerateInterpolator(2f))
                                .start()
                    }
                    tagGroup?.apply {
                        animate().translationY(if (isAppBarHidden) 0f else height.toFloat())
                                .setInterpolator(DecelerateInterpolator(2f))
                                .start()
                    }
                    isAppBarHidden = !isAppBarHidden
                }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_page, menu)
        menu?.findItem(R.id.action_share)?.icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18)
        menu?.findItem(R.id.action_search)?.icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_search)
                .color(Color.WHITE).sizeDp(18)
        menu?.findItem(R.id.action_favourite)?.apply {
            isVisible = false
            if (entrance != null) {
                val star = Star()
                star.href = connection?.url
                starApi?.contains(star)?.apply {
                    compose(RxHelper.rxSchedulerHelper())
                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                            .doOnNext {
                                this@GalleryActivity.star = it
                            }
                            .subscribe {
                                icon = IconicsDrawable(this@GalleryActivity)
                                        .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                                        .colorRes(if (this@GalleryActivity.star) R.color.colorAccent else R.color.white)
                                        .sizeDp(18)
                                isVisible = true
                            }
                }
            }
        }
        menu?.findItem(R.id.action_mosaic)?.icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_blur_linear)
                .color(Color.WHITE).sizeDp(18)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_share -> {
                val thumbnail = galleryAdapter!!.data[viewPager!!.currentItem]["thumbnail"]
                val shareParamImage = ShareParamImage(title.toString(), thumbnail, connection!!.url)
                shareParamImage.image = ShareImage(thumbnail)
                ShareHelper.share(this, shareParamImage)
                true
            }
            R.id.action_search -> {
                id?.let {
                    progressBar?.visibility = View.VISIBLE
                    searchDisposable?.apply {
                        if (!isDisposed) {
                            dispose()
                        }
                    }
                    searchDisposable = search(it, title.toString()).subscribe({
                        progressBar?.visibility = View.GONE
                    }, {
                        progressBar?.visibility = View.GONE
                    })
                }
                true
            }
            R.id.action_favourite -> {
                val star = Star()
                star.href = connection?.url
                entrance?.let {
                    star.data["entrance"] = it
                }
                star.data["title"] = title.toString()
                val first = galleryAdapter?.data?.firstOrNull()
                first?.get("cover")?.let {
                    star.data["thumbnail"] = it
                }
                first?.get("thumbnail_error")?.let {
                    star.data["thumbnail_error"] = it
                }
                menu?.findItem(R.id.action_favourite)?.apply {
                    val starAction = if (this@GalleryActivity.star) {
                        starApi?.delete(star)
                    } else {
                        starApi?.insertOrUpdate(star)
                    }
                    starAction?.apply {
                        compose(RxHelper.rxSchedulerHelper())
                        compose(bindUntilEvent(ActivityEvent.DESTROY))
                                .subscribe {
                                    this@GalleryActivity.star = !this@GalleryActivity.star
                                    icon = IconicsDrawable(this@GalleryActivity)
                                            .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                                            .colorRes(if (this@GalleryActivity.star) R.color.colorAccent else R.color.white)
                                            .sizeDp(18)
                                }
                    }
                }
                true
            }
            R.id.action_mosaic -> {
                startActivity(Intent(intentFor<MosaicActivity>(MosaicActivity.EXTRA_TITLE to title, MosaicActivity.EXTRA_URL to galleryAdapter!!.data[viewPager!!.currentItem]["thumbnail"])))
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (connection != null && tagEventObservable != null) {
            RxBus.getInstance().unregister(connection!!, tagEventObservable!!)
        }
    }
}