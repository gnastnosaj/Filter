package com.github.gnastnosaj.filter.magneto.standalone.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.ListView
import br.com.mauker.materialsearchview.MaterialSearchView
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.magneto.standalone.R
import com.github.gnastnosaj.filter.magneto.standalone.api.datasource.MagnetoDataSource
import com.github.gnastnosaj.filter.magneto.standalone.ui.adatper.MagnetoAdapter
import com.github.gnastnosaj.filter.magneto.standalone.ui.view.materialSearchView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.shizhefei.mvc.IDataAdapter
import com.shizhefei.mvc.MVCHelper
import com.shizhefei.mvc.MVCSwipeRefreshHelper
import com.shizhefei.mvc.OnStateChangeListener
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.themedAppBarLayout
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.swipeRefreshLayout

class MagnetoActivity : BaseActivity() {

    private var searchView: MaterialSearchView? = null

    private var mvcHelper: MVCHelper<List<Map<String, String>>>? = null
    private var dataSource: MagnetoDataSource? = null

    companion object {
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        frameLayout {
            lparams {
                width = matchParent
                height = matchParent
            }
            fitsSystemWindows = true
            coordinatorLayout {
                themedAppBarLayout(R.style.AppTheme_AppBarOverlay) {
                    setSupportActionBar(toolbar {
                        popupTheme = R.style.AppTheme_PopupOverlay
                    }.lparams(matchParent, wrapContent) {
                        scrollFlags = SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
                    })
                }.lparams(matchParent, wrapContent)
                frameLayout {
                    val magnetoAdapter = MagnetoAdapter()

                    val swipeRefreshLayout = swipeRefreshLayout {
                        visibility = View.INVISIBLE

                        recyclerView {
                            lparams(matchParent, matchParent)
                            val linearLayoutManager = LinearLayoutManager(context)
                            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
                            layoutManager = linearLayoutManager
                            addItemDecoration(HorizontalDividerItemDecoration.Builder(context).size(1).build())

                            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                                override fun onLongPress(e: MotionEvent?) {
                                    super.onLongPress(e)
                                    e?.let { event ->
                                        val childView = findChildViewUnder(event.x, event.y)
                                        val position = getChildAdapterPosition(childView)
                                        if (-1 < position && position < magnetoAdapter.data.size) {
                                            val data = magnetoAdapter.data[position]
                                            val clipData = ClipData.newPlainText("Magnet Link", data["magnet"])
                                            val clipboardManager = Boilerplate.getInstance().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboardManager.primaryClip = clipData
                                            Snackbar.make(this@recyclerView, R.string.copy_magnet_success, Snackbar.LENGTH_SHORT).show()
                                        }
                                    }
                                }

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
                                            if (-1 < position && position < magnetoAdapter.data.size) {
                                                val data = magnetoAdapter.data[position]
                                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(data["magnet"])))
                                            }
                                        }
                                        true
                                    } else false
                                }
                            })
                        }
                    }.lparams(matchParent, matchParent)

                    val logo = imageView(R.drawable.magneto) {
                        scaleType = ImageView.ScaleType.FIT_XY
                    }.lparams {
                        gravity = Gravity.CENTER
                        width = dip(88)
                        height = dip(88)
                    }

                    mvcHelper = MVCSwipeRefreshHelper(swipeRefreshLayout)
                    mvcHelper?.adapter = magnetoAdapter

                    dataSource = MagnetoDataSource(this@MagnetoActivity)
                    mvcHelper?.setDataSource(dataSource)

                    mvcHelper?.setOnStateChangeListener(object : OnStateChangeListener<List<Map<String, String>>> {
                        override fun onStartLoadMore(adapter: IDataAdapter<List<Map<String, String>>>?) {

                        }

                        override fun onStartRefresh(adapter: IDataAdapter<List<Map<String, String>>>?) {
                            swipeRefreshLayout.visibility = View.VISIBLE
                            logo.visibility = View.GONE
                        }

                        override fun onEndLoadMore(adapter: IDataAdapter<List<Map<String, String>>>?, result: List<Map<String, String>>?) {

                        }

                        override fun onEndRefresh(adapter: IDataAdapter<List<Map<String, String>>>?, result: List<Map<String, String>>?) {

                        }
                    })

                }.lparams(matchParent, matchParent) {
                    behavior = AppBarLayout.ScrollingViewBehavior()
                }
            }.lparams(matchParent, matchParent)
            searchView = materialSearchView {
                val suggestionsListView = findViewById<ListView>(R.id.suggestion_list)
                if (suggestionsListView.headerViewsCount == 0) {
                    suggestionsListView.addHeaderView(with(AnkoContext.create(context)) {
                        frameLayout {
                            imageView {
                                setImageDrawable(IconicsDrawable(context)
                                        .icon(MaterialDesignIconic.Icon.gmi_delete)
                                        .color(Color.WHITE).sizeDp(18))
                            }.lparams {
                                gravity = Gravity.END
                                setMargins(0, dip(12), dip(12), dip(12))
                            }
                        }
                    })
                }
                prepareSuggestions()
                setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
                    override fun onQueryTextChange(newText: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextSubmit(query: String?): Boolean {
                        dataSource?.keyword = query?.trim()
                        mvcHelper?.refresh()
                        return false
                    }
                })
                setOnItemClickListener { _, _, position, _ ->
                    if (position == 0) {
                        searchView?.clearAll()
                    } else {
                        dataSource?.keyword = searchView?.getSuggestionAtPosition(position - 1)
                        mvcHelper?.refresh()
                    }
                    closeSearch()
                }
            }.lparams(matchParent, matchParent)
        }

        try {
            dataSource?.keyword = intent.dataString.replace("magneto:", "")
            title = intent.getStringExtra(EXTRA_TITLE)
        } catch (throwable: Throwable) {
        }
        dataSource?.keyword?.apply {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mvcHelper?.refresh()
            searchView?.saveQueryToDb(this, System.currentTimeMillis())
        }
    }

    override fun onBackPressed() {
        searchView?.apply {
            if (isOpen) {
                closeSearch()
                return
            }
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_magneto, menu)
        menu?.findItem(R.id.action_search)?.icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_search)
                .color(Color.WHITE).sizeDp(18)
        menu?.findItem(R.id.action_share)?.icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_search -> {
                searchView?.openSearch()
                true
            }
            R.id.action_share -> {
                share(resources.getString(R.string.app_link))
                true
            }
            R.id.action_about -> {
                browse(resources.getString(R.string.app_link))
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}