package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.os.Bundle
import android.view.MenuItem
import com.github.gnastnosaj.filter.kaleidoscope.R
import org.adblockplus.libadblockplus.android.AdblockEngine
import org.adblockplus.libadblockplus.android.settings.*

class SettingsActivity : AppCompatPreferenceActivity(), BaseSettingsFragment.Provider, GeneralSettingsFragment.Listener, WhitelistedDomainsSettingsFragment.Listener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.action_settings)

        insertGeneralFragment()
    }

    private fun insertGeneralFragment() {
        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, GeneralSettingsFragment.newInstance())
                .commit()
    }

    private fun insertWhitelistedFragment() {
        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, WhitelistedDomainsSettingsFragment.newInstance())
                .addToBackStack(WhitelistedDomainsSettingsFragment::class.java.simpleName)
                .commit()
    }


    override fun getAdblockEngine(): AdblockEngine {
        AdblockHelper.get().provider.waitForReady()
        return AdblockHelper.get().provider.engine
    }

    override fun getAdblockSettingsStorage(): AdblockSettingsStorage {
        return AdblockHelper.get().storage
    }

    override fun onWhitelistedDomainsClicked(p0: GeneralSettingsFragment?) {
        insertWhitelistedFragment()
    }

    override fun onAdblockSettingsChanged(p0: BaseSettingsFragment<*>?) {

    }

    override fun isValidDomain(p0: WhitelistedDomainsSettingsFragment?, p1: String?, p2: AdblockSettings?): Boolean {
        return !p1.isNullOrEmpty()
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
}