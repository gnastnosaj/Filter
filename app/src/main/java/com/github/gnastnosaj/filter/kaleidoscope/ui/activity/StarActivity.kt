package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.SharedElementCallback
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import com.facebook.drawee.view.SimpleDraweeView
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.dsl.core.Catalog
import com.github.gnastnosaj.filter.dsl.core.Category
import com.github.gnastnosaj.filter.dsl.core.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.datasource.StarDataSource
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.ui.adapter.WaterfallAdapter
import com.shizhefei.mvc.MVCSwipeRefreshHelper
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.themedAppBarLayout
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import org.jetbrains.anko.wrapContent


class StarActivity : BaseActivity() {
    private var plugin: Plugin? = null
    private var catalog: Catalog? = null

    companion object {
        const val EXTRA_PLUGIN = "plugin"
        const val EXTRA_CATALOG_HASH_CODE = "catalog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.action_favourite)
        plugin = intent.getParcelableExtra(EXTRA_PLUGIN)
        catalog = Kaleidoscope.restoreInstanceState(intent.getIntExtra(EXTRA_CATALOG_HASH_CODE, -1))
        if (catalog == null) {
            savedInstanceState?.apply {
                val hashCode = getInt(EXTRA_CATALOG_HASH_CODE)
                catalog = Kaleidoscope.restoreInstanceState(hashCode)
            }
        }

        frameLayout {
            fitsSystemWindows = true
            coordinatorLayout {
                themedAppBarLayout(R.style.AppTheme_AppBarOverlay) {
                    setSupportActionBar(toolbar {
                        popupTheme = R.style.AppTheme_PopupOverlay
                    }.lparams(matchParent, wrapContent) {
                        scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    })
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                }.lparams(matchParent, wrapContent)
                frameLayout {
                    val waterfallAdapter = WaterfallAdapter()
                    val swipeRefreshLayout = swipeRefreshLayout {
                        recyclerView {
                            lparams(matchParent, matchParent)
                            val staggeredGridLayoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
                            layoutManager = staggeredGridLayoutManager

                            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                                    return true
                                }
                            })
                            addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                                override fun onInterceptTouchEvent(rv: RecyclerView?, e: MotionEvent?): Boolean {
                                    return if (gestureDetector.onTouchEvent(e)) {
                                        e?.let { event ->
                                            val childView = findChildViewUnder(event.x, event.y)
                                            val position = getChildAdapterPosition(childView)
                                            if (-1 < position && position < waterfallAdapter.data.size) {
                                                val data = waterfallAdapter.data[position]
                                                connect(data["entrance"]!!)?.execute("page", data["href"]!!)?.let {
                                                    when ((it as? com.github.gnastnosaj.filter.dsl.core.Connection)?.execute("layout")
                                                            ?: "gallery") {
                                                        "gallery" -> {
                                                            val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                                                    this@StarActivity, childView, GalleryActivity.TRANSITION_NAME
                                                            )
                                                            ActivityCompat.startActivity(context as Activity, intentFor<GalleryActivity>(
                                                                    GalleryActivity.EXTRA_ID to (data["id"]
                                                                            ?: data["title"]),
                                                                    GalleryActivity.EXTRA_TITLE to data["title"],
                                                                    GalleryActivity.EXTRA_PLUGIN to plugin,
                                                                    GalleryActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                                                            ), optionsCompat.toBundle())
                                                        }
                                                        "detail" -> {
                                                            ActivityCompat.startActivity(context as Activity, intentFor<DetailActivity>(
                                                                    DetailActivity.EXTRA_ID to (data["id"]
                                                                            ?: data["title"]),
                                                                    DetailActivity.EXTRA_TITLE to data["title"],
                                                                    DetailActivity.EXTRA_PLUGIN to plugin,
                                                                    DetailActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                                                            ), null)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        true
                                    } else false
                                }
                            })
                        }
                    }.lparams(matchParent, matchParent)
                    val mvcHelper = MVCSwipeRefreshHelper<List<Map<String, String>>>(swipeRefreshLayout)
                    mvcHelper.adapter = waterfallAdapter
                    plugin?.let {
                        val dataSource = StarDataSource(context, it)
                        mvcHelper.setDataSource(dataSource)
                        mvcHelper.refresh()
                    }
                }.lparams(matchParent, matchParent) {
                    behavior = AppBarLayout.ScrollingViewBehavior()
                }
            }.lparams(matchParent, matchParent)
        }

        ActivityCompat.setExitSharedElementCallback(this, object : SharedElementCallback() {
            override fun onSharedElementEnd(sharedElementNames: MutableList<String>?, sharedElements: MutableList<View>?, sharedElementSnapshots: MutableList<View>?) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
                sharedElements?.forEach {
                    it.findViewById<SimpleDraweeView>(R.id.thumbnail)?.apply {
                        drawable.setVisible(true, true)
                    }
                }
            }
        })
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
        catalog?.let {
            outState?.putInt(EXTRA_CATALOG_HASH_CODE, Kaleidoscope.saveInstanceState(it))
        }
    }

    private fun connect(entrance: String): Connection? {
        catalog?.apply {
            connections?.values?.firstOrNull {
                it.url == entrance
            }?.let {
                return it
            }
            fun search(category: Category): Connection? {
                if (category.connection?.url == entrance) {
                    return category.connection
                }
                category.children?.forEach {
                    search(it)?.let {
                        return it
                    }
                }
                return null
            }
            categories?.forEach {
                search(it)?.let {
                    return it
                }
            }
            return connections?.values?.firstOrNull()
        }
        return null
    }
}