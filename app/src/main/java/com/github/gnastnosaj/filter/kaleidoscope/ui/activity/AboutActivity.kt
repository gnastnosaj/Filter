package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.widget.ImageView
import android.widget.TextView
import com.github.gnastnosaj.filter.kaleidoscope.BuildConfig
import com.github.gnastnosaj.filter.kaleidoscope.R
import me.drakeet.multitype.Items
import me.drakeet.support.about.AbsAboutActivity
import me.drakeet.support.about.Category
import me.drakeet.support.about.Contributor
import me.drakeet.support.about.License


class AboutActivity : AbsAboutActivity() {
    override fun onItemsCreated(items: Items) {
        items.add(Category("Developers"))
        items.add(Contributor(R.drawable.avatar_jasontsang, "Jason Tsang", "Developer & designer", "https://github.com/gnastnosaj"))

        items.add(Category("Open Source Licenses"))
        items.add(License("leakcanary", "square", License.APACHE_2, "https://github.com/square/leakcanary"))
        items.add(License("Cockroach", "wanjian", License.MIT, "https://github.com/android-notes/Cockroach"))
        items.add(License("timber", "JakeWharton", License.APACHE_2, "https://github.com/JakeWharton/timber"))
        items.add(License("okio", "square", License.APACHE_2, "https://github.com/square/okio"))
        items.add(License("RxJava", "ReactiveX", License.APACHE_2, "https://github.com/ReactiveX/RxJava"))
        items.add(License("RxAndroid", "ReactiveX", License.APACHE_2, "https://github.com/ReactiveX/RxAndroid"))
        items.add(License("RxLifecycle", "trello", License.APACHE_2, "https://github.com/trello/RxLifecycle"))
        items.add(License("fresco", "facebook", License.APACHE_2, "https://github.com/facebook/fresco"))
        items.add(License("MVCHelper", "LuckyJayce", License.APACHE_2, "https://github.com/LuckyJayce/MVCHelper"))
        items.add(License("status-bar-compat", "msdx", License.APACHE_2, "https://github.com/msdx/status-bar-compat"))
        items.add(License("Android-Iconics", "mikepenz", License.APACHE_2, "https://github.com/mikepenz/Android-Iconics"))
        items.add(License("lottie-android", "airbnb", License.APACHE_2, "https://github.com/airbnb/lottie-android"))
        items.add(License("gson", "google", License.APACHE_2, "https://github.com/google/gson"))
        items.add(License("retrofit", "square", License.APACHE_2, "https://github.com/square/retrofit"))
        items.add(License("anko", "Kotlin", License.APACHE_2, "https://github.com/Kotlin/anko"))
        items.add(License("okhttp", "square", License.APACHE_2, "https://github.com/square/okhttp"))
        items.add(License("BiliShare", "Bilibili", License.APACHE_2, "https://github.com/Bilibili/BiliShare"))
        items.add(License("RxCache", "VictorAlbertos", License.APACHE_2, "https://github.com/VictorAlbertos/RxCache"))
        items.add(License("MaterialSearchView", "MiguelCatalan", License.APACHE_2, "https://github.com/MiguelCatalan/MaterialSearchView"))
        items.add(License("Context-Menu.Android", "Yalantis", License.APACHE_2, "https://github.com/Yalantis/Context-Menu.Android"))
        items.add(License("libadblockplus-android", "adblockplus", License.GPL_V3, "https://github.com/adblockplus/libadblockplus-android"))
        items.add(License("AgentWeb", "Justson", License.APACHE_2, "https://github.com/Justson/AgentWeb"))
        items.add(License("RecyclerView-FlexibleDivider", "yqritc", License.APACHE_2, "https://github.com/yqritc/RecyclerView-FlexibleDivider"))
        items.add(License("BigImageViewer", "Piasy", License.MIT, "https://github.com/Piasy/BigImageViewer"))
        items.add(License("AndroidTagGroup", "2dxgujun", License.APACHE_2, "https://github.com/2dxgujun/AndroidTagGroup"))
        items.add(License("Crescento", "developer-shivam", License.MIT, "https://github.com/developer-shivam/Crescento"))
        items.add(License("Anko-ExpandableTextView", "arslancharyev31", License.MIT, "https://github.com/arslancharyev31/Anko-ExpandableTextView"))
        items.add(License("NineGridImageView", "laobie", License.APACHE_2, "https://github.com/laobie/NineGridImageView"))
        items.add(License("ImageWatcher", "iielse", "", "https://github.com/iielse/ImageWatcher"))
        items.add(License("Genius-Android", "qiujuer", License.APACHE_2, "https://github.com/qiujuer/Genius-Android"))
        items.add(License("epic", "tiann", License.APACHE_2, "https://github.com/tiann/epic"))
        items.add(License("update", "czy1121", License.APACHE_2, "https://github.com/czy1121/update"))
        items.add(License("jlog", "JiongBull", License.APACHE_2, "https://github.com/JiongBull/jlog"))
        items.add(License("RxPermissions", "tbruyelle", License.APACHE_2, "https://github.com/tbruyelle/RxPermissions"))
        items.add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
        items.add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
    }

    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.setImageResource(R.drawable.ic_launcher)
        slogan.setText(R.string.slogan)
        version.text = "v${BuildConfig.VERSION_NAME}"
    }
}