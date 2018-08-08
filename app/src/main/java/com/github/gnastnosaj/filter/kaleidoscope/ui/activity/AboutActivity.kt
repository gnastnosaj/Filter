package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.widget.ImageView
import android.widget.TextView
import com.github.gnastnosaj.filter.kaleidoscope.BuildConfig
import com.github.gnastnosaj.filter.kaleidoscope.R
import me.drakeet.multitype.Items
import me.drakeet.support.about.*


class AboutActivity: AbsAboutActivity() {
    override fun onItemsCreated(items: Items) {
        items.add(Category("Developers"))
        items.add(Contributor(R.drawable.avatar_jasontsang, "Jason Tsang", "Developer & designer", "https://github.com/gnastnosaj"))

        items.add(Category("Open Source Licenses"))
        items.add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
        items.add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
    }

    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.setImageResource(R.drawable.ic_launcher)
        slogan.setText(R.string.slogan)
        version.text = "v${BuildConfig.VERSION_NAME}"
    }
}