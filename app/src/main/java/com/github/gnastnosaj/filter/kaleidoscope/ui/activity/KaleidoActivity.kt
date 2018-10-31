package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import br.com.mauker.materialsearchview.MaterialSearchView
import com.bilibili.socialize.share.core.shareparam.ShareParamText
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper
import com.github.gnastnosaj.boilerplate.util.keyboard.BaseActivity
import com.github.gnastnosaj.boilerplate.util.textdrawable.TextDrawable
import com.github.gnastnosaj.filter.dsl.groovy.api.Project
import com.github.gnastnosaj.filter.kaleidoscope.BuildConfig
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.PluginApi
import com.github.gnastnosaj.filter.kaleidoscope.api.search.search
import com.github.gnastnosaj.filter.kaleidoscope.ui.view.materialSearchView
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.google.gson.Gson
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.tbruyelle.rxpermissions2.RxPermissions
import com.trello.rxlifecycle2.android.ActivityEvent
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment
import com.yalantis.contextmenu.lib.MenuObject
import com.yalantis.contextmenu.lib.MenuParams
import ezy.boost.update.UpdateInfo
import ezy.boost.update.UpdateManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.themedAppBarLayout
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class KaleidoActivity : BaseActivity() {
    private var projects: MutableMap<String, Project>? = null

    private var searchView: MaterialSearchView? = null
    private var progressBar: ProgressBar? = null
    private var contextMenuDialogFragment: ContextMenuDialogFragment? = null

    private var searchMode = 0
    private var searchDisposable: Disposable? = null
    private var pluginDisposable: Disposable? = null
    private var contextMenuDisposable: Disposable? = null

    private val kaleidoHits = LongArray(5)
    private var donateDialog: AlertDialog? = null
    private var donateDisposable: Disposable? = null
    private var crazy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe()

        crazy = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("crazy", false)

        savedInstanceState?.let {
            projects = Kaleidoscope.restoreInstanceState(it.getInt("projects"))
        }
        projects = projects ?: HashMap()

        frameLayout {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                themedAppBarLayout(R.style.AppTheme_AppBarOverlay) {
                    setSupportActionBar(toolbar {
                        popupTheme = R.style.AppTheme_PopupOverlay
                        imageView {
                            setImageResource(R.drawable.slogan)
                        }
                    })
                    supportActionBar?.apply {
                        setDisplayHomeAsUpEnabled(true)
                        val old = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
                        val new = Bitmap.createScaledBitmap(old, dip(25), dip(25), true)
                        val drawable = BitmapDrawable(resources, new)
                        setHomeAsUpIndicator(drawable)
                    }
                }
                frameLayout {
                    backgroundColorResource = R.color.colorPrimary
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
                                            searchMode = 0
                                            search(editText.text.toString())
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
                                                    .icon(MaterialDesignIconic.Icon.gmi_bike)
                                                    .colorRes(R.color.colorPrimary).sizeDp(20)
                                        }.lparams(wrapContent, wrapContent) {
                                            leftMargin = dip(8)
                                            rightMargin = dip(8)
                                        }
                                        setOnClickListener {
                                            searchMode = 1
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

        createContextMenu()

        Observable.timer(30, TimeUnit.SECONDS)
                .subscribe {
                    UpdateManager
                            .create(this)
                            .setParser {
                                val updateInfo = Gson().fromJson(it, UpdateInfo::class.java)
                                updateInfo.hasUpdate = updateInfo.versionCode > Boilerplate.versionCode
                                updateInfo.isSilent = true
                                return@setParser updateInfo
                            }
                            .setOnFailureListener {
                                Timber.w(it.message)
                            }
                            .check()
                }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_kaleido, menu)
        menu?.findItem(R.id.action_kaleido)?.icon = IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_github_box)
                .color(Color.WHITE).sizeDp(18)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                donateDisposable?.dispose()
                donateDisposable = Observable
                        .timer(300, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            donate {
                                Snackbar.make(findViewById(android.R.id.content), resources.getString(R.string.secret, resources.getString(if (crazy) R.string.secret_normal else R.string.secret_crazy)), Snackbar.LENGTH_SHORT).show()
                            }
                        }
                if (!crazy) {
                    System.arraycopy(kaleidoHits, 1, kaleidoHits, 0, kaleidoHits.size - 1)
                    kaleidoHits[kaleidoHits.size - 1] = SystemClock.uptimeMillis()
                    if (kaleidoHits[0] >= (SystemClock.uptimeMillis() - 1000)) {
                        donateDisposable?.dispose()
                        donate {
                            crazy = true
                            createContextMenu()
                            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("crazy", crazy).apply()
                            Snackbar.make(findViewById(android.R.id.content), R.string.crazy_mode, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }
            R.id.action_kaleido -> {
                if (fragmentManager.findFragmentByTag(ContextMenuDialogFragment.TAG) == null) {
                    contextMenuDialogFragment?.show(supportFragmentManager, ContextMenuDialogFragment.TAG)
                }
                true
            }
            R.id.action_share -> {
                ShareHelper.share(this, ShareParamText(resources.getString(R.string.action_share), resources.getString(R.string.share_kaleidoscope, BuildConfig.SHARE_URI)))
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
            else -> {
                super.onOptionsItemSelected(item)
            }
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
        contextMenuDisposable?.apply {
            if (!isDisposed) {
                dispose()
            }
        }
        contextMenuDisposable = PluginApi.plugins()
                .retry(3)
                .map {
                    if (!crazy) {
                        it.filter {
                            (it.args?.get("adult_warning") as? Boolean)?.let {
                                return@filter !it
                            }
                            return@filter true
                        }
                    } else {
                        it
                    }
                }
                .flatMap { plugins ->
                    Observable.just(plugins)
                            .map {
                                it.map {
                                    val menuObject = MenuObject()
                                    menuObject.bgDrawable = TextDrawable.builder()
                                            .buildRect(it.id?.substring(0, 1)?.toUpperCase()
                                                    ?: "", resources.getColor(R.color.colorPrimary))
                                    menuObject.title = it.name
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

                                val kaleido = findViewById<View>(R.id.action_kaleido)
                                kaleido.setOnLongClickListener {
                                    if (crazy) {
                                        crazy = false
                                        createContextMenu()
                                        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("crazy", crazy).apply()
                                        Snackbar.make(findViewById(android.R.id.content), R.string.normal_mode, Snackbar.LENGTH_SHORT).show()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                val tapTarget = TapTarget
                                        .forView(kaleido, resources.getString(R.string.filter), resources.getString(R.string.filter_description))
                                        .outerCircleAlpha(0.8f)
                                TapTargetView.showFor(this, tapTarget, object : TapTargetView.Listener() {
                                    override fun onTargetClick(view: TapTargetView) {
                                        view.dismiss(true)
                                        if (fragmentManager.findFragmentByTag(ContextMenuDialogFragment.TAG) == null) {
                                            contextMenuDialogFragment?.show(supportFragmentManager, ContextMenuDialogFragment.TAG)
                                        }
                                    }
                                })
                            }
                }
                .subscribeOn(Schedulers.io())
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe()
    }

    private fun search(keyword: String) {
        searchDisposable?.apply {
            if (!this.isDisposed) {
                this.dispose()
            }
        }

        if (keyword.isBlank() && searchView?.isOpen != true) {
            searchView?.openSearch()
        } else {
            if (searchMode == 0) {
                searchView?.saveQueryToDb(keyword, System.currentTimeMillis())
                ActivityCompat.startActivity(this, intentFor<WebViewActivity>(WebViewActivity.EXTRA_URL to "https://www.baidu.com/s?wd=${URLEncoder.encode(keyword, "utf-8")}", WebViewActivity.EXTRA_KEYWORD to keyword).setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK), null)
            } else {
                searchDisposable = Observable
                        .defer {
                            progressBar?.visibility = View.VISIBLE
                            showDynamicBoxCustomView(DYNAMIC_BOX_LT_BIKING_IS_COOL, this)
                            PluginApi.plugins().compose(RxHelper.rxSchedulerHelper())
                        }
                        .compose(bindUntilEvent(ActivityEvent.DESTROY))
                        .flatMap {
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
                                    PluginApi.initialize(plugin).subscribeOn(Schedulers.io())
                                }
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .retry(3)
                                .timeout(1, TimeUnit.MINUTES)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext { project ->
                                    projects?.put(plugin.id!!, project)
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
                                        .setNegativeButton(R.string.adult_warning_not_18) { dialog, _ -> dialog.dismiss() }
                                        .setPositiveButton(R.string.adult_warning_continue) { dialog, _ ->
                                            ActivityCompat.startActivity(this, intentFor<CatalogActivity>(CatalogActivity.EXTRA_PLUGIN to plugin, CatalogActivity.EXTRA_PROJECT_HASH_CODE to Kaleidoscope.saveInstanceState(project)).setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK), null)
                                            dialog.dismiss()
                                        }.show()
                            } else {
                                ActivityCompat.startActivity(this, intentFor<CatalogActivity>(CatalogActivity.EXTRA_PLUGIN to plugin, CatalogActivity.EXTRA_PROJECT_HASH_CODE to Kaleidoscope.saveInstanceState(project)).setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK), null)
                            }
                            return@map true
                        }
                    }
                    false
                }
    }

    private fun donate(ok: (() -> Unit)? = null) {
        donateDialog?.dismiss()
        val qrcode = ImageView(this)
        qrcode.setImageResource(R.drawable.donate_qrcode)
        qrcode.setOnLongClickListener {
            Observable
                    .create<String> { emitter ->
                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.donate_alipay)
                        val path = "${ShareHelper.configuration!!.getImageCachePath(this)}/donate.jpg"
                        val fos = FileOutputStream(path)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                        fos.flush()
                        fos.close()
                        emitter.onNext(path)
                        emitter.onComplete()
                    }
                    .compose(RxHelper.rxSchedulerHelper())
                    .subscribe({ path ->
                        Snackbar.make(findViewById(android.R.id.content), resources.getString(R.string.save_picture_success, path), Snackbar.LENGTH_SHORT).show()
                        MediaStore.Images.Media.insertImage(contentResolver, path, File(path).name, "")
                    }, {
                        Snackbar.make(findViewById(android.R.id.content), R.string.save_picture_fail, Snackbar.LENGTH_SHORT).show()
                    })
            true
        }
        donateDialog = AlertDialog.Builder(this)
                .setMessage(R.string.donate)
                .setView(qrcode)
                .setCancelable(false)
                .setPositiveButton(R.string.like) { dialog, _ ->
                    dialog.dismiss()
                    ok?.invoke()
                }
                .setNegativeButton(R.string.dislike) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        val layoutParams = qrcode.layoutParams as FrameLayout.LayoutParams
        layoutParams.width = dip(256)
        layoutParams.height = dip(256)
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        qrcode.layoutParams = layoutParams
    }
}