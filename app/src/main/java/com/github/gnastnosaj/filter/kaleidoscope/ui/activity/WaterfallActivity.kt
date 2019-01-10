package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.SharedElementCallback
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import com.github.gnastnosaj.filter.dsl.core.Page
import com.github.gnastnosaj.filter.dsl.groovy.api.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.datasource.ConnectionDataSource
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.ui.adapter.WaterfallAdapter
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.wrapContent

class WaterfallActivity : BaseActivity() {
    private var plugin: Plugin? = null
    private var connection: Connection? = null
    private var preload: Page? = null

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_PLUGIN = "plugin"
        const val EXTRA_CONNECTION_HASH_CODE = "connectionHashCode"
        const val EXTRA_PRELOAD_HASH_CODE = "preloadHashCode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = intent.getStringExtra(EXTRA_TITLE)
        plugin = intent.getParcelableExtra(EXTRA_PLUGIN)
        connection = Kaleidoscope.restoreInstanceState(intent.getIntExtra(EXTRA_CONNECTION_HASH_CODE, -1))
        if (connection == null) {
            savedInstanceState?.apply {
                val hashCode = getInt(EXTRA_CONNECTION_HASH_CODE)
                connection = Kaleidoscope.restoreInstanceState(hashCode)
            }
        }
        if (intent.hasExtra(EXTRA_PRELOAD_HASH_CODE)) {
            preload = Kaleidoscope.restoreInstanceState(intent.getIntExtra(EXTRA_PRELOAD_HASH_CODE, -1))
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
                                override fun onSingleTapUp(e: MotionEvent): Boolean {
                                    return true
                                }
                            })
                            addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                                    return if (gestureDetector.onTouchEvent(e)) {
                                        val childView = findChildViewUnder(e.x, e.y)
                                        childView?.let {
                                            val position = getChildAdapterPosition(childView)
                                            if (-1 < position && position < waterfallAdapter.data.size) {
                                                val data = waterfallAdapter.data[position]
                                                preview(show(childView, data, plugin, connection), connection, waterfallAdapter.data)
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
                    connection?.let {
                        val dataSource = ConnectionDataSource(context, it, preload)
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
        connection?.let {
            outState?.putInt(EXTRA_CONNECTION_HASH_CODE, Kaleidoscope.saveInstanceState(it))
        }
    }
}