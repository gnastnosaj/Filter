package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.transition.Slide
import android.support.transition.TransitionManager
import android.support.v4.view.ViewCompat
import android.transition.ChangeBounds
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.dsl.core.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.datasource.ConnectionDataSource
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Star
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.StarApi
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.trello.rxlifecycle2.android.ActivityEvent
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.collapsingToolbarLayout
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.themedAppBarLayout
import org.jetbrains.anko.support.v4.nestedScrollView

class DetailActivity : BaseActivity() {

    private var menu: Menu? = null

    private var id: String? = null
    private var plugin: Plugin? = null
    private var connection: Connection? = null

    private var entrance: String? = null
    private var starApi: StarApi? = null
    private var star: Boolean = false

    private var cover: RatioImageView? = null
    private var details: LinearLayout? = null
    private var loading: LottieAnimationView? = null

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PLUGIN = "plugin"
        const val EXTRA_CONNECTION_HASH_CODE = "connectionHashCode"

        const val TRANSITION_NAME = "transitionName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.sharedElementEnterTransition = ChangeBounds()
        }

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
                                ViewCompat.setTransitionName(this, TRANSITION_NAME)
                                aspectRatio = 1f
                                hierarchy.setPlaceholderImage(R.drawable.slogan_dark)
                                hierarchy.actualImageScaleType = ScalingUtils.ScaleType.CENTER_CROP
                            }.lparams(matchParent, wrapContent)
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
            frameLayout {
                loading = lottieAnimationView {
                    setAnimation("lottie/skeleton_frame_loading.json")
                    repeatCount = LottieDrawable.INFINITE
                    playAnimation()
                }.lparams(matchParent, matchParent)
                nestedScrollView {
                    details = linearLayout {
                        orientation = LinearLayout.VERTICAL
                    }.lparams(matchParent, wrapContent)
                }.lparams(matchParent, matchParent)
            }.lparams(matchParent, matchParent) {
                behavior = AppBarLayout.ScrollingViewBehavior()
            }
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
                            (it["details"] as? Map<String, Map<String, Connection>>)?.forEach { k, v ->
                                val item = LinearLayout(this)
                                item.orientation = LinearLayout.HORIZONTAL
                                val key = TextView(this)
                                key.apply {
                                    textSize = 18f
                                    textColorResource = R.color.colorPrimary
                                    text = k
                                }
                                item.addView(key)
                                v.filterKeys {
                                    it.isNotBlank()
                                }.let {
                                    val tagGroup = with(AnkoContext.create(this)) {
                                        tagGroup(theme = R.style.TagGroup_Beauty_Red_Detail) {
                                            setTags(it.keys.toList())
                                            setOnTagClickListener { tag ->
                                                it[tag]?.let {
                                                    startActivity(Intent(intentFor<WaterfallActivity>(WaterfallActivity.EXTRA_TITLE to tag, WaterfallActivity.EXTRA_PLUGIN to plugin, WaterfallActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it))))
                                                }
                                            }
                                        }
                                    }
                                    val layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
                                    layoutParams.apply {
                                        leftMargin = dip(10)
                                        rightMargin = dip(10)
                                        weight = 1f
                                    }
                                    item.addView(tagGroup, layoutParams)
                                }
                                details?.apply {
                                    val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                    layoutParams.apply {
                                        leftMargin = dip(16)
                                        topMargin = dip(6)
                                        rightMargin = dip(16)
                                        bottomMargin = dip(6)
                                    }
                                    val transition = Slide(Gravity.END)
                                    transition.duration = 1000
                                    TransitionManager.beginDelayedTransition(this, transition)
                                    addView(item, layoutParams)
                                }
                            }
                            loading?.apply {
                                animate().alpha(0f).withEndAction {
                                    cancelAnimation()
                                    (parent as ViewGroup).removeView(this)
                                }.start()
                            }
                        }
                    }
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
                                this@DetailActivity.star = it
                            }
                            .subscribe {
                                icon = IconicsDrawable(this@DetailActivity)
                                        .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                                        .colorRes(if (this@DetailActivity.star) R.color.colorAccent else R.color.white)
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