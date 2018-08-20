package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.view.OnApplyWindowInsetsListener
import android.support.v4.view.ViewCompat
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import br.com.mauker.materialsearchview.MaterialSearchView
import com.bilibili.socialize.share.core.shareparam.ShareParamText
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper
import com.github.gnastnosaj.boilerplate.util.keyboard.BaseActivity
import com.github.gnastnosaj.boilerplate.util.keyboard.KeyBoardUtil
import com.github.gnastnosaj.boilerplate.util.textdrawable.TextDrawable
import com.github.gnastnosaj.filter.dsl.groovy.GrooidClassLoader
import com.github.gnastnosaj.filter.dsl.groovy.api.Project
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.KaleidoscopeRetrofit
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.PluginApi
import com.github.gnastnosaj.filter.kaleidoscope.api.search.search
import com.github.gnastnosaj.filter.kaleidoscope.net.PluginInterceptor
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.adblockWebView
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.materialSearchView
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.tabSwitcher
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.trello.rxlifecycle2.android.ActivityEvent
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment
import com.yalantis.contextmenu.lib.MenuObject
import com.yalantis.contextmenu.lib.MenuParams
import de.mrapp.android.tabswitcher.*
import de.mrapp.android.tabswitcher.TabSwitcher
import de.mrapp.android.util.DisplayUtil.getDisplayWidth
import de.mrapp.android.util.ThemeUtil
import groovy.lang.Script
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.adblockplus.libadblockplus.android.settings.AdblockHelper
import org.adblockplus.libadblockplus.android.webview.AdblockWebView
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.design.themedAppBarLayout
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class KaleidoscopeActivity : BaseActivity(), TabSwitcherListener {
    private var projects: MutableMap<String, Project>? = null

    private var searchView: MaterialSearchView? = null
    private var progressBar: ProgressBar? = null
    private var contextMenuDialogFragment: ContextMenuDialogFragment? = null

    private var tabSwitcher: TabSwitcher? = null
    private var snackbar: Snackbar? = null

    private var searchDisposable: Disposable? = null
    private var pluginDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            projects = Kaleidoscope.restoreInstanceState(it.getInt("projects"))
        }
        projects = projects ?: HashMap()

        frameLayout {
            lparams(matchParent, matchParent)
            tabSwitcher = tabSwitcher().lparams(matchParent, matchParent)
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
                        searchDisposable?.apply {
                            if (!this.isDisposed) {
                                this.dispose()
                            }
                        }
                        query?.apply {
                            search(this)
                        }
                        return false
                    }
                })
                setOnItemClickListener { _, _, position, _ ->
                    if (position == 0) {
                        searchView?.clearAll()
                    } else {
                        search(getSuggestionAtPosition(position - 1))
                    }
                    closeSearch()
                }
            }.lparams(matchParent, matchParent)
        }

        tabSwitcher?.apply {
            emptyView = createEmptyView()
            clearSavedStatesWhenRemovingTabs(false)
            ViewCompat.setOnApplyWindowInsetsListener(this, createWindowInsetsListener())
            decorator = Decorator()
            addListener(this@KaleidoscopeActivity)
            showToolbars(true)
            showAddTabButton(createAddTabButtonListener())
            setToolbarNavigationIcon(R.drawable.ic_plus_box_24dp, createAddTabListener())
            TabSwitcher.setupWithMenu(this, createTabSwitcherButtonListener())

            inflateMenu()
        }

        createContextMenu()
    }

    override fun onBackPressed() {
        searchView?.apply {
            if (isOpen) {
                closeSearch()
                return
            }
        }
        tabSwitcher?.apply {
            if (isSwitcherShown) {
                hideSwitcher()
                return
            }
            val adblockWebView = findViewById<AdblockWebView>(R.id.kaleidoscope_adblock_web_view)
            adblockWebView?.apply {
                if (canGoBack()) {
                    goBack()
                    return
                }
            }
            selectedTab?.apply {
                removeTab(this)
                return
            }
        }
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        projects?.let {
            outState.putInt("projects", Kaleidoscope.saveInstanceState(it))
        }
    }

    override fun hideSoftByEditViewIds(): IntArray {
        return intArrayOf(R.id.kaleidoscope_keyword)
    }

    private fun createContextMenu() {
        PluginApi.plugins().retry(3)
                .flatMap { plugins ->
                    Observable.just(plugins)
                            .map {
                                it.map {
                                    val menuObject = MenuObject()
                                    menuObject.bgDrawable = TextDrawable.builder()
                                            .buildRect(it.id?.substring(0, 1)?.toUpperCase()
                                                    ?: "", resources.getColor(R.color.colorPrimary))
                                    menuObject
                                }
                            }
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                            .doOnNext {
                                val menuParams = MenuParams()
                                menuParams.apply {
                                    actionBarSize = obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize)).getDimension(0, 0f).toInt()
                                    menuObjects = it
                                    isClosableOutside = true
                                }
                                contextMenuDialogFragment = ContextMenuDialogFragment.newInstance(menuParams)
                                contextMenuDialogFragment?.setItemClickListener { _, i ->
                                    pluginDisposable?.apply {
                                        if (!isDisposed) {
                                            dispose()
                                        }
                                    }
                                    pluginDisposable = start(plugins[i]).subscribe()
                                }
                            }
                }
                .subscribeOn(Schedulers.io())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe()
    }

    private fun createEmptyView(): View {
        return with(AnkoContext.create(this)) {
            linearLayout {
                lparams(matchParent, matchParent)
                orientation = LinearLayout.VERTICAL
                view().lparams(matchParent, obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize)).getDimension(0, 0f).toInt())
                frameLayout {
                    createDynamicBox(frameLayout {
                        backgroundColor = Color.WHITE
                        linearLayout {
                            orientation = LinearLayout.VERTICAL
                            linearLayout {
                                orientation = LinearLayout.VERTICAL
                                linearLayout {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = Gravity.CENTER_VERTICAL
                                    val editText = editText {
                                        id = R.id.kaleidoscope_keyword
                                        backgroundDrawable = null
                                        textSize = 18f
                                        hint = "Kaleidoscope"
                                        singleLine = true
                                    }.lparams {
                                        width = 0
                                        height = wrapContent
                                        weight = 1f
                                    }
                                    frameLayout {
                                        imageView {
                                            image = IconicsDrawable(context)
                                                    .icon(MaterialDesignIconic.Icon.gmi_search)
                                                    .colorRes(R.color.colorPrimary).sizeDp(18)
                                        }.lparams(wrapContent, wrapContent) {
                                            leftMargin = dip(8)
                                            rightMargin = dip(8)
                                        }
                                        setOnClickListener {
                                            val keyword = editText.text.toString()
                                            tabSwitcher?.apply {
                                                addTab(createTab("https://www.baidu.com/s?wd=${URLEncoder.encode(keyword, "utf-8")}"))
                                            }
                                            editText.setText("")
                                        }
                                    }.lparams(wrapContent, wrapContent)
                                    textView {
                                        text = "|"
                                        textSize = 12f
                                    }.lparams(wrapContent, wrapContent)
                                    frameLayout {
                                        imageView {
                                            image = IconicsDrawable(context)
                                                    .icon(CommunityMaterial.Icon.cmd_magnet)
                                                    .colorRes(R.color.colorPrimary).sizeDp(18)
                                        }.lparams(wrapContent, wrapContent) {
                                            leftMargin = dip(8)
                                            rightMargin = dip(8)
                                        }
                                        setOnClickListener {
                                            search(editText.text.toString())
                                            editText.setText("")
                                        }
                                    }.lparams(wrapContent, wrapContent)
                                }.lparams(matchParent, wrapContent)
                                view {
                                    backgroundColorResource = R.color.colorPrimary
                                }.lparams(matchParent, 1)
                            }.lparams(matchParent, wrapContent) {
                                leftMargin = dip(64)
                                rightMargin = dip(64)
                            }
                            linearLayout {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER_VERTICAL
                                textView {
                                    text = "{"
                                    textSize = 64f
                                }
                                linearLayout {
                                    orientation = LinearLayout.VERTICAL
                                    gravity = Gravity.CENTER_HORIZONTAL
                                    var time: TextView? = null
                                    var am: TextView? = null
                                    var date: TextView? = null
                                    linearLayout {
                                        orientation = LinearLayout.HORIZONTAL
                                        time = textView {
                                            textSize = 18f
                                            textColorResource = R.color.colorPrimary
                                        }
                                        am = textView {
                                            textColorResource = R.color.colorPrimary
                                        }.lparams {
                                            leftMargin = dip(4)
                                        }
                                    }.lparams(wrapContent, wrapContent)
                                    date = textView {
                                        textColorResource = R.color.colorPrimary
                                    }.lparams(wrapContent, wrapContent)
                                    val timeFormat = SimpleDateFormat("h:m:s")
                                    val amFormat = SimpleDateFormat("aa")
                                    val dateFormat = SimpleDateFormat("E, MMM d, yyyy")
                                    Observable.interval(0, 1, TimeUnit.SECONDS)
                                            .compose(RxHelper.rxSchedulerHelper())
                                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                                            .subscribe {
                                                val current = Date()
                                                time?.text = timeFormat.format(current)
                                                am?.text = amFormat.format(current)
                                                date?.text = dateFormat.format(current)
                                            }
                                }.lparams {
                                    width = 0
                                    height = wrapContent
                                    weight = 1f
                                }
                                textView {
                                    text = "}"
                                    textSize = 64f
                                }
                            }.lparams(matchParent, wrapContent) {
                                topMargin = dip(16)
                                leftMargin = dip(32)
                                rightMargin = dip(32)
                            }
                        }.lparams(matchParent, wrapContent) {
                            gravity = Gravity.CENTER_VERTICAL
                        }
                    }.lparams(matchParent, matchParent))
                    progressBar = horizontalProgressBar(R.style.Widget_AppCompat_ProgressBar_Horizontal) {
                        scaleY = 0.5f
                        isIndeterminate = true
                        visibility = View.GONE
                    }.lparams(matchParent, wrapContent)
                }.lparams {
                    width = matchParent
                    height = 0
                    weight = 1.0f
                }
            }
        }
    }

    private fun createAddTabButtonListener(): AddTabButtonListener {
        return AddTabButtonListener { tabSwitcher ->
            tabSwitcher.addTab(createTab(), 0)
        }
    }

    private fun createAddTabListener(): View.OnClickListener {
        return View.OnClickListener {
            val animation = createRevealAnimation()
            tabSwitcher?.addTab(createTab(), 0, animation)
        }
    }

    override fun onSwitcherHidden(tabSwitcher: TabSwitcher) {
        snackbar?.dismiss()
    }

    override fun onAllTabsRemoved(tabSwitcher: TabSwitcher, tabs: Array<out Tab>, animation: Animation) {
        val text = getString(R.string.cleared_tabs_snackbar)
        showUndoSnackbar(text, 0, *tabs)
        inflateMenu()
        TabSwitcher.setupWithMenu(tabSwitcher, createTabSwitcherButtonListener())
    }

    override fun onSwitcherShown(tabSwitcher: TabSwitcher) {
    }

    override fun onTabRemoved(tabSwitcher: TabSwitcher, index: Int, tab: Tab, animation: Animation) {
        val text = getString(R.string.removed_tab_snackbar, tab.title)
        showUndoSnackbar(text, index, tab)
        inflateMenu()
        TabSwitcher.setupWithMenu(tabSwitcher, createTabSwitcherButtonListener())
    }

    override fun onSelectionChanged(tabSwitcher: TabSwitcher, selectedTabIndex: Int, selectedTab: Tab?) {
    }

    override fun onTabAdded(tabSwitcher: TabSwitcher, index: Int, tab: Tab, animation: Animation) {
        inflateMenu()
        TabSwitcher.setupWithMenu(tabSwitcher, createTabSwitcherButtonListener())
    }

    private fun createTabSwitcherButtonListener(): View.OnClickListener {
        return View.OnClickListener { tabSwitcher?.toggleSwitcherVisibility() }
    }

    private fun inflateMenu() {
        tabSwitcher?.inflateToolbarMenu(if (tabSwitcher!!.count > 0) R.menu.menu_tab_switcher else R.menu.menu_tab_empty, createToolbarMenuListener())
        setupMenuItem()
    }

    private fun setupMenuItem() {
        val menu = tabSwitcher?.toolbarMenu
        if (menu != null) {
            menu.findItem(R.id.action_kaleido)?.icon = IconicsDrawable(this)
                    .icon(MaterialDesignIconic.Icon.gmi_github_box)
                    .color(Color.WHITE).sizeDp(18)
        } else {
            tabSwitcher?.apply {
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        tabSwitcher?.toolbarMenu?.let {
                            setupMenuItem()
                        }
                    }
                })
            }
        }
    }

    private fun createToolbarMenuListener(): Toolbar.OnMenuItemClickListener {
        return Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_tab -> {
                    tabSwitcher?.apply {
                        if (isSwitcherShown) {
                            addTab(createTab(), 0, createRevealAnimation())
                        } else {
                            addTab(createTab(), 0, createPeekAnimation())
                        }
                    }
                    true
                }
                R.id.action_clear_tabs -> {
                    tabSwitcher?.clear()
                    true
                }
                R.id.action_kaleido -> {
                    if (fragmentManager.findFragmentByTag(ContextMenuDialogFragment.TAG) == null) {
                        contextMenuDialogFragment?.show(supportFragmentManager, ContextMenuDialogFragment.TAG)
                    }
                    true
                }
                R.id.action_share -> {
                    ShareHelper.share(this, ShareParamText(resources.getString(R.string.action_share), resources.getString(R.string.share_kaleidoscope)))
                    true
                }
                R.id.action_settings -> {
                    startActivity(intentFor<SettingsActivity>())
                    true
                }
                R.id.action_about -> {
                    startActivity(intentFor<AboutActivity>())
                    true
                }
                R.id.action_exit -> {
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun createTab(url: String = "https://www.baidu.com/"): Tab {
        val tab = Tab(this, R.string.tab_title)
        val parameters = Bundle()
        parameters.putString("url", url)
        tab.parameters = parameters
        return tab
    }

    private fun createRevealAnimation(): Animation {
        var x = 0f
        var y = 0f
        getNavigationMenuItem()?.let {
            val location = IntArray(2)
            it.getLocationInWindow(location)
            x = location[0] + it.width / 2f
            y = location[1] + it.height / 2f
        }
        return RevealAnimation.Builder().setX(x).setY(y).create()
    }

    private fun createPeekAnimation(): Animation {
        return PeekAnimation.Builder().setX(tabSwitcher?.width?.minus(2f) ?: 0f).create()
    }

    private fun getNavigationMenuItem(): View? {
        tabSwitcher?.toolbars?.let {
            val toolbar = if (it.size > 1) it[1] else it[0]
            val size = toolbar.childCount

            for (i in 0 until size) {
                val child = toolbar.getChildAt(i)

                if (child is ImageButton) {
                    return child
                }
            }
        }
        return null
    }

    private fun showUndoSnackbar(text: CharSequence, index: Int, vararg tabs: Tab) {
        tabSwitcher?.let {
            snackbar = Snackbar.make(it, text, Snackbar.LENGTH_LONG)
                    .setActionTextColor(ContextCompat.getColor(this, R.color.light_blue_400))
        }
        snackbar?.apply {
            setAction(R.string.undo, createUndoSnackbarListener(this, index, *tabs))
            addCallback(createUndoSnackbarCallback(*tabs))
            show()
        }
    }

    private fun createUndoSnackbarListener(snackbar: Snackbar, index: Int, vararg tabs: Tab): View.OnClickListener {
        return View.OnClickListener {
            snackbar.setAction(null, null)

            tabSwitcher?.apply {
                if (isSwitcherShown) {
                    addAllTabs(tabs, index)
                } else if (tabs.size == 1) {
                    addTab(tabs[0], 0, createPeekAnimation())
                }
            }
        }
    }

    private fun createUndoSnackbarCallback(vararg tabs: Tab): BaseTransientBottomBar.BaseCallback<Snackbar> {
        return object : BaseTransientBottomBar.BaseCallback<Snackbar>() {

            override fun onDismissed(snackbar: Snackbar?, event: Int) {
                if (event != BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION) {
                    for (tab in tabs) {
                        tabSwitcher?.clearSavedState(tab)
                        //decorator.clearState(tab)
                    }
                }
            }
        }
    }

    private fun createWindowInsetsListener(): OnApplyWindowInsetsListener {
        return OnApplyWindowInsetsListener { _, insets ->
            val left = insets.systemWindowInsetLeft
            val top = insets.systemWindowInsetTop
            val right = insets.systemWindowInsetRight
            val bottom = insets.systemWindowInsetBottom
            tabSwitcher?.setPadding(left, top, right, bottom)
            var touchableAreaTop = top.toFloat()

            if (tabSwitcher?.layout === Layout.TABLET) {
                touchableAreaTop += resources.getDimensionPixelSize(R.dimen.tablet_tab_container_height).toFloat()
            }

            val touchableArea = RectF(left.toFloat(), touchableAreaTop,
                    (getDisplayWidth(this) - right).toFloat(), touchableAreaTop + ThemeUtil.getDimensionPixelSize(this, R.attr.actionBarSize))
            tabSwitcher?.addDragGesture(SwipeGesture.Builder().setTouchableArea(touchableArea).create())
            tabSwitcher?.addDragGesture(PullDownGesture.Builder().setTouchableArea(touchableArea).create())
            insets
        }
    }

    private inner class State(tab: Tab) : AbstractState(tab), TabPreviewListener {
        var url: String? = null
        var state: Bundle? = null
        var screenshot: Bitmap? = null

        override fun saveInstanceState(outState: Bundle) {
            url?.let {
                outState.putString("url", it)
            }
            state?.let {
                outState.putBundle("state", it)
            }
            screenshot?.let {
                outState.putParcelable("screenshot", it)
            }
        }

        override fun restoreInstanceState(savedInstanceState: Bundle?) {
            url = savedInstanceState?.getString("url")
            state = savedInstanceState?.getBundle("state")
            screenshot = savedInstanceState?.getParcelable("screenshot")
        }

        override fun onLoadTabPreview(tabSwitcher: TabSwitcher, tab: Tab): Boolean {
            return true
        }
    }

    private inner class Decorator : StatefulTabSwitcherDecorator<State>() {
        override fun onShowTab(context: Context, tabSwitcher: TabSwitcher, view: View, tab: Tab, index: Int, viewType: Int, state: State?, savedInstanceState: Bundle?) {
            val appBarLayout = view.findViewById<AppBarLayout>(R.id.kaleidoscope_app_bar_layout)
            val address = view.findViewById<EditText>(R.id.kaleidoscope_address)
            val progressBar = view.findViewById<ProgressBar>(R.id.kaleidoscope_progress_bar)
            val adblockWebView = view.findViewById<AdblockWebView>(R.id.kaleidoscope_adblock_web_view)
            val screenshot = view.findViewById<ImageView>(R.id.kaleidoscope_screenshot)

            appBarLayout?.apply {
                val lp = layoutParams
                lp.height = if (tabSwitcher.isSwitcherShown) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
                layoutParams = lp
            }

            if (savedInstanceState == null) {
                tab.title = resources.getString(R.string.tab_title)
                progressBar?.apply {
                    visibility = View.GONE
                    progress = 0
                }
                adblockWebView?.apply {
                    clearView()
                    clearHistory()

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            address?.setText(url)
                            state?.let {
                                saveWebViewState(it, view, url)
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            address?.setText(url)
                            state?.let {
                                saveWebViewState(it, view, url, true)
                            }
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            progressBar?.visibility = View.GONE
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progressBar?.apply {
                                if (newProgress != 100) {
                                    visibility = View.VISIBLE
                                    progress = newProgress
                                } else {
                                    visibility = View.GONE
                                }
                            }
                        }

                        override fun onReceivedTitle(view: WebView, title: String) {
                            super.onReceivedTitle(view, title)
                            tab.title = title
                        }
                    }

                    tab.parameters?.getString("url")?.let {
                        if (it.isNotBlank()) {
                            loadUrl(it)
                            address?.setText(it)
                        }
                    }
                    visibility = View.VISIBLE
                }
                screenshot?.apply {
                    visibility = View.GONE
                    setImageBitmap(null)
                }
            } else if (state != null) {
                if (tabSwitcher.isSwitcherShown) {
                    adblockWebView?.visibility = View.GONE
                    screenshot?.visibility = View.VISIBLE
                    screenshot?.setImageBitmap(state.screenshot)
                } else {
                    address?.setText(state.url)
                    adblockWebView?.apply {
                        if (state.url != url) {
                            clearView()
                            clearHistory()

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    address?.setText(url)
                                    saveWebViewState(state, view, url)
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    address?.setText(url)
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    saveWebViewState(state, view, url, true)
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    progressBar?.visibility = View.GONE
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progressBar?.apply {
                                        if (newProgress != 100) {
                                            visibility = View.VISIBLE
                                            progress = newProgress
                                        } else {
                                            visibility = View.GONE
                                        }
                                    }
                                }

                                override fun onReceivedTitle(view: WebView, title: String) {
                                    super.onReceivedTitle(view, title)
                                    tab.title = title
                                }
                            }

                            state.state?.let {
                                restoreState(it)
                            }
                            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                            loadUrl(state.url)
                        }
                        visibility = View.VISIBLE
                    }
                    screenshot?.apply {
                        visibility = View.GONE
                        setImageBitmap(null)
                    }
                }
            }
        }

        override fun onInflateView(inflater: LayoutInflater, parent: ViewGroup?, viewType: Int): View {
            return with(AnkoContext.create(parent?.context ?: inflater.context, parent)) {
                frameLayout {
                    lparams(matchParent, matchParent)
                    backgroundColor = Color.WHITE
                    coordinatorLayout {
                        var adblockWebView: AdblockWebView? = null
                        themedAppBarLayout(R.style.AppTheme_AppBarOverlay) {
                            id = R.id.kaleidoscope_app_bar_layout
                            frameLayout {
                                toolbar {
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    popupTheme = R.style.AppTheme_PopupOverlay
                                    inflateMenu(R.menu.menu_tab)
                                    setOnMenuItemClickListener(createToolbarMenuListener())
                                    tabSwitcher?.apply {
                                        TabSwitcher.setupWithMenu(this, menu, createTabSwitcherButtonListener())
                                    }
                                    editText {
                                        id = R.id.kaleidoscope_address
                                        backgroundResource = R.drawable.rounded_edittext
                                        textSize = 16f
                                        textColorResource = R.color.colorPrimary
                                        singleLine = true
                                        setPadding(dip(8), dip(8), dip(8), dip(8))
                                        setOnEditorActionListener { _, actionId, _ ->
                                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                                adblockWebView?.loadUrl(text.toString())
                                            }
                                            KeyBoardUtil.closeKeybord(this, context)
                                            clearFocus()
                                            true
                                        }
                                    }.lparams(matchParent, wrapContent)
                                }.lparams(matchParent, wrapContent)
                                horizontalProgressBar(R.style.Widget_AppCompat_ProgressBar_Horizontal) {
                                    id = R.id.kaleidoscope_progress_bar
                                    scaleY = 0.5f
                                    visibility = View.GONE
                                }.lparams(matchParent, wrapContent) {
                                    gravity = Gravity.BOTTOM
                                    bottomMargin = -dip(8)
                                }
                            }.lparams(matchParent, wrapContent) {
                                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                            }
                        }.lparams(matchParent, wrapContent)
                        swipeRefreshLayout {
                            frameLayout {
                                adblockWebView = adblockWebView {
                                    id = R.id.kaleidoscope_adblock_web_view
                                    setProvider(AdblockHelper.get().provider)
                                    settings.userAgentString = "${settings.userAgentString} SearchCraft/2.6.2 (Baidu; P1 7.0)"
                                    settings.setAppCacheEnabled(true)
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.setSupportZoom(true)
                                    settings.loadWithOverviewMode = true
                                }.lparams(matchParent, matchParent)
                                imageView {
                                    id = R.id.kaleidoscope_screenshot
                                    visibility = View.GONE
                                }.lparams(matchParent, matchParent)
                            }
                            setOnRefreshListener {
                                adblockWebView?.reload()
                                isRefreshing = false
                            }
                        }.lparams(matchParent, matchParent) {
                            behavior = AppBarLayout.ScrollingViewBehavior()
                        }
                    }.lparams(matchParent, matchParent)
                }
            }
        }

        override fun onCreateState(context: Context, tabSwitcher: TabSwitcher, view: View, tab: Tab, index: Int, viewType: Int, savedInstanceState: Bundle?): State? {
            val state = State(tab)

            tabSwitcher.addTabPreviewListener(state)

            savedInstanceState?.let {
                state.restoreInstanceState(it)
            }

            return state
        }

        override fun onClearState(state: State) {
            tabSwitcher?.removeTabPreviewListener(state)
            state.url = null
            state.state = null
            state.screenshot?.recycle()
            state.screenshot = null
        }

        override fun onSaveInstanceState(view: View, tab: Tab, index: Int, viewType: Int, state: State?, outState: Bundle) {
            state?.saveInstanceState(outState)
        }

        private fun saveWebViewState(webViewState: State, webView: WebView, url: String, takeScreenshot: Boolean = false) {
            webViewState.url = url

            webViewState.state = Bundle()
            webView.saveState(webViewState.state)

            if (takeScreenshot) {
                webViewState.screenshot?.recycle()
                webViewState.screenshot = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(webViewState.screenshot)
                webView.draw(canvas)
            }
        }
    }

    private fun search(keyword: String) {
        searchDisposable?.apply {
            if (!this.isDisposed) {
                this.dispose()
            }
        }

        if (keyword.isEmpty() && searchView?.isOpen != true) {
            searchView?.openSearch()
        } else {
            searchDisposable = Observable
                    .defer {
                        progressBar?.visibility = View.VISIBLE
                        showDynamicBoxCustomView(DYNAMIC_BOX_LT_BIKING_IS_COOL, this)
                        PluginApi.plugins().compose(RxHelper.rxSchedulerHelper())
                    }
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .flatMap {
                        PluginInterceptor.plugins(it)

                        it.firstOrNull {
                            it.id == keyword
                        }?.let { plugin ->
                            return@flatMap start(plugin)
                        }

                        (this as BaseActivity).search(keyword).map { it.isNotEmpty() }
                    }
                    .doOnNext {
                        progressBar?.visibility = View.GONE
                        dismissDynamicBox(this)
                    }
                    .doOnError {
                        progressBar?.visibility = View.GONE
                        dismissDynamicBox(this)
                    }
                    .doOnDispose {
                        progressBar?.visibility = View.GONE
                        dismissDynamicBox(this)
                    }
                    .subscribe()
        }
    }

    private fun initialize(plugin: Plugin): Observable<Project> {
        return Observable.just(plugin)
                .switchMap {
                    if (projects?.containsKey(plugin.id!!) == true) {
                        Observable.just(projects!![plugin.id!!])
                    } else {
                        Observable
                                .defer {
                                    progressBar?.visibility = View.VISIBLE
                                    showDynamicBoxCustomView(DYNAMIC_BOX_LT_BIKING_IS_COOL, this)
                                    Snackbar.make(findViewById(android.R.id.content), R.string.plugin_initializing, Snackbar.LENGTH_SHORT).show()
                                    Observable
                                            .just(plugin)
                                            .switchMap {
                                                if (plugin.script != null) {
                                                    Observable.create<Script> { emitter ->
                                                        emitter.onNext(GrooidClassLoader.loadAndCreateGroovyObject(this, plugin.script) as Script)
                                                        emitter.onComplete()
                                                    }
                                                } else {
                                                    KaleidoscopeRetrofit.instance.service.plugin(plugin.id!!)
                                                }
                                            }
                                            .subscribeOn(Schedulers.io())
                                }
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .observeOn(Schedulers.io())
                                .map { script ->
                                    val project = Project(script)
                                    project.execute("area", resources.getString(R.string.area))
                                    project.execute("build")
                                    projects?.put(plugin.id!!, project)
                                    project
                                }
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext {
                                    progressBar?.visibility = View.GONE
                                    dismissDynamicBox(this)
                                }.doOnError {
                                    progressBar?.visibility = View.GONE
                                    dismissDynamicBox(this)
                                    Snackbar.make(findViewById(android.R.id.content), R.string.plugin_error, Snackbar.LENGTH_SHORT).show()
                                }.doOnDispose {
                                    progressBar?.visibility = View.GONE
                                    dismissDynamicBox(this)
                                }
                    }
                }
    }

    private fun start(plugin: Plugin): Observable<Boolean> {
        return initialize(plugin)
                .compose(RxHelper.rxSchedulerHelper())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .map { project ->
                    project.catalog?.connections?.let {
                        if (it.isNotEmpty()) {
                            if (plugin.args?.get("adult_warning") as? Boolean == true) {
                                AlertDialog.Builder(this)
                                        .setMessage(R.string.adult_warning)
                                        .setNegativeButton(R.string.adult_warning_not_18, { dialog, _ -> dialog.dismiss() })
                                        .setPositiveButton(R.string.adult_warning_continue, { dialog, _ ->
                                            startActivity(intentFor<CatalogActivity>(CatalogActivity.EXTRA_PLUGIN to plugin, CatalogActivity.EXTRA_CATALOG_HASH_CODE to Kaleidoscope.saveInstanceState(project.catalog!!)))
                                            dialog.dismiss()
                                        }).show()
                            } else {
                                startActivity(intentFor<CatalogActivity>(CatalogActivity.EXTRA_PLUGIN to plugin, CatalogActivity.EXTRA_CATALOG_HASH_CODE to Kaleidoscope.saveInstanceState(project.catalog!!)))
                            }
                            return@map true
                        }
                    }
                    false
                }
    }
}