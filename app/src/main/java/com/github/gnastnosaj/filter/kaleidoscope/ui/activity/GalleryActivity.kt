package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import com.github.gnastnosaj.filter.dsl.groovy.api.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.datasource.ConnectionDataSource
import com.github.gnastnosaj.filter.kaleidoscope.api.event.TagEvent
import com.github.gnastnosaj.filter.kaleidoscope.api.event.ToolbarEvent
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Star
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.StarApi
import com.github.gnastnosaj.filter.kaleidoscope.ui.adapter.GalleryAdapter
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.wrapContent


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

    private var data: MutableMap<String, String>? = null
    private var plugin: Plugin? = null
    private var connection: Connection? = null

    private var entrance: String? = null
    private var starApi: StarApi? = null
    private var star: Boolean = false

    companion object {
        const val EXTRA_DATA = "data"
        const val EXTRA_PLUGIN = "plugin"
        const val EXTRA_CONNECTION_HASH_CODE = "connectionHashCode"

        const val TRANSITION_NAME = "transitionName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        data = Gson().fromJson<MutableMap<String, String>>(intent.getStringExtra(EXTRA_DATA), MutableMap::class.java)
        title = data?.get("title")
        plugin = intent.getParcelableExtra(EXTRA_PLUGIN)
        plugin?.let {
            starApi = StarApi(it)
        }
        connection = Kaleidoscope.restoreInstanceState(intent.getIntExtra(EXTRA_CONNECTION_HASH_CODE, -1))
        if (connection == null) {
            savedInstanceState?.apply {
                val hashCode = getInt(EXTRA_CONNECTION_HASH_CODE)
                connection = Kaleidoscope.restoreInstanceState(hashCode)
            }
        }
        connection?.let {
            entrance = it.execute("entrance") as? String
            tagEventObservable = RxBus.getInstance().register(it, TagEvent::class.java)
        }

        frameLayout {
            backgroundColorResource = R.color.colorPrimaryDark
            fitsSystemWindows = true
            ViewCompat.setTransitionName(this, TRANSITION_NAME)
            frameLayout {
                galleryAdapter = GalleryAdapter(context)
                viewPager = viewPager {
                    val MIN_SCALE = 0.85f
                    val MIN_ALPHA = 0.5f
                    setPageTransformer(true) { page, position ->
                        when {
                            position < -1 -> page.alpha = 0f
                            position <= 1 -> {
                                val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                                val vertMargin = page.height * (1 - scaleFactor) / 2
                                val horzMargin = page.width * (1 - scaleFactor) / 2
                                if (position < 0) {
                                    page.translationX = horzMargin - vertMargin / 2
                                } else {
                                    page.translationX = -horzMargin + vertMargin / 2
                                }
                                page.scaleX = scaleFactor
                                page.scaleY = scaleFactor
                                page.alpha = MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA)
                            }
                            else -> page.alpha = 0f
                        }
                    }
                }.lparams(matchParent, matchParent)
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
                                    tagEvent.tags[tag]?.let {
                                        startActivity(Intent(intentFor<WaterfallActivity>(WaterfallActivity.EXTRA_TITLE to tag, WaterfallActivity.EXTRA_PLUGIN to plugin, WaterfallActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it))))
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

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                connection?.url?.let {
                    val preview = when {
                        velocityX < 0 && viewPager?.currentItem == galleryAdapter!!.data.size - 1 -> {
                            connection?.execute("preview", it) as? Map<String, Any>
                        }
                        velocityX > 0 && viewPager?.currentItem == 0 -> {
                            connection?.execute("preview", it, true) as? Map<String, Any>
                        }
                        else -> null
                    }

                    (preview?.get("page") as? Connection)?.let { page ->
                        ActivityCompat.startActivity(this@GalleryActivity, intentFor<GalleryActivity>(EXTRA_DATA to Gson().toJson(preview["data"]), EXTRA_PLUGIN to plugin, EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(page)), null)
                        setPendingTransition(null)
                        finish()
                        if (velocityX < 0) {
                            overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left)
                        } else if (velocityX > 0) {
                            overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right)
                        }
                    }
                }

                return false
            }
        })

        connection?.let {
            val dataSource = ConnectionDataSource(this@GalleryActivity, it)
            mvcHelper.setDataSource(dataSource)
            mvcHelper.setAdapter(galleryAdapter, ViewPagerViewHandler())
            mvcHelper.refresh()

            viewPager?.apply {
                setOnTouchListener { view, event ->
                    if (!dataSource.hasMore() || (view as ViewPager).currentItem == 0) {
                        gestureDetector.onTouchEvent(event)
                    } else {
                        false
                    }
                }
            }
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
                val star = Star(data)
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
                data?.get("id")?.let {
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
                val star = Star(data)
                entrance?.let {
                    star.data["entrance"] = it
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        connection?.let {
            outState?.putInt(EXTRA_CONNECTION_HASH_CODE, Kaleidoscope.saveInstanceState(it))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (connection != null && tagEventObservable != null) {
            RxBus.getInstance().unregister(connection!!, tagEventObservable!!)
        }
    }
}