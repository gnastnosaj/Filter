package com.github.gnastnosaj.filter.kaleidoscope.ui.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import com.github.gnastnosaj.filter.dsl.core.Connection
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.api.datasource.ConnectionDataSource
import com.github.gnastnosaj.filter.kaleidoscope.ui.activity.GalleryActivity
import com.github.gnastnosaj.filter.kaleidoscope.ui.adapter.WaterfallAdapter
import com.shizhefei.mvc.MVCHelper
import com.shizhefei.mvc.MVCSwipeRefreshHelper
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.swipeRefreshLayout

class WaterfallFragment : Fragment() {
    private var connection: Connection? = null
    private var rootView: View? = null

    private var mvcHelper: MVCHelper<List<Map<String, String>>>? = null
    private var dataSource: ConnectionDataSource? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (connection == null) {
            savedInstanceState?.apply {
                val hashCode = getInt(CONNECTION_HASH_CODE)
                connection = Kaleidoscope.restoreInstanceState(hashCode)
            }
        }

        return rootView ?: context?.let {
            rootView = with(AnkoContext.create(it, container)) {
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
                                                when (data["type"] ?: "thumbnail") {
                                                    "thumbnail" -> connection?.execute("page", data["href"]!!)?.let {
                                                        when ((it as? Connection)?.execute("layout") ?: "gallery") {
                                                            "gallery" -> startActivity(intentFor<GalleryActivity>(
                                                                    GalleryActivity.EXTRA_ID to (data["id"]
                                                                            ?: data["title"]),
                                                                    GalleryActivity.EXTRA_TITLE to data["title"],
                                                                    GalleryActivity.EXTRA_CONNECTION_HASH_CODE to Kaleidoscope.saveInstanceState(it)
                                                            ))
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
                    connection?.let {
                        mvcHelper = MVCSwipeRefreshHelper(swipeRefreshLayout)
                        dataSource = ConnectionDataSource(context, it)
                        mvcHelper?.setDataSource(dataSource)
                        mvcHelper?.adapter = waterfallAdapter
                        mvcHelper?.refresh()
                    }
                }
            }

            return rootView
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        connection?.let {
            outState.putInt(CONNECTION_HASH_CODE, Kaleidoscope.saveInstanceState(it))
        }
    }

    companion object {
        const val CONNECTION_HASH_CODE = "connectionHashCode"
        fun newInstance(connection: Connection): WaterfallFragment {
            val instance = WaterfallFragment()
            instance.connection = connection
            return instance
        }
    }
}