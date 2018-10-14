package com.github.gnastnosaj.filter.kaleidoscope.ui.activity

import android.os.Bundle
import android.preference.Preference
import android.support.design.widget.Snackbar
import android.view.MenuItem
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.ui.fragment.RepositoriesSettingsFragment
import ezy.boost.update.UpdateManager
import org.adblockplus.libadblockplus.android.AdblockEngine
import org.adblockplus.libadblockplus.android.settings.*

class SettingsActivity : AppCompatPreferenceActivity(), BaseSettingsFragment.Provider, org.adblockplus.libadblockplus.android.settings.GeneralSettingsFragment.Listener, WhitelistedDomainsSettingsFragment.Listener, GeneralSettingsFragment.Listener, RepositoriesSettingsFragment.Listener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.action_settings)

        insertGeneralFragment()
    }

    private fun insertGeneralFragment() {
        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, GeneralSettingsFragment())
                .commit()
    }

    private fun insertWhitelistedFragment() {
        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, WhitelistedDomainsSettingsFragment.newInstance())
                .addToBackStack(WhitelistedDomainsSettingsFragment::class.java.simpleName)
                .commit()
    }

    private fun insertRepositoriesFragment() {
        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, RepositoriesSettingsFragment.newInstance())
                .addToBackStack(RepositoriesSettingsFragment::class.java.simpleName)
                .commit()
    }

    override fun getAdblockEngine(): AdblockEngine {
        AdblockHelper.get().provider.waitForReady()
        return AdblockHelper.get().provider.engine
    }

    override fun getAdblockSettingsStorage(): AdblockSettingsStorage {
        return AdblockHelper.get().storage
    }

    override fun onWhitelistedDomainsClicked(p0: org.adblockplus.libadblockplus.android.settings.GeneralSettingsFragment) {
        insertWhitelistedFragment()
    }

    override fun isValidDomain(fragment: WhitelistedDomainsSettingsFragment, domain: String, settings: AdblockSettings): Boolean {
        return domain.isNotBlank()
    }

    override fun onRepositoriesClicked(fragment: GeneralSettingsFragment) {
        insertRepositoriesFragment()
    }

    override fun onUpdateClicked(fragment: GeneralSettingsFragment) {
        UpdateManager
                .create(this)
                .setManual(true)
                .setOnFailureListener {
                    it.message?.let {
                        Snackbar.make(fragment.view, it, Snackbar.LENGTH_SHORT).show()
                    }
                }
                .check()
    }

    override fun isValidRepository(fragment: RepositoriesSettingsFragment, repository: String): Boolean {
        return repository.isNotBlank()
    }

    override fun onKaleidoSettingsChanged(fragment: RepositoriesSettingsFragment) {

    }

    override fun onAdblockSettingsChanged(fragment: BaseSettingsFragment<*>) {

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

class GeneralSettingsFragment : org.adblockplus.libadblockplus.android.settings.GeneralSettingsFragment() {

    private var SETTINGS_REPOSITORIES_KEY: String? = null
    private var SETTINGS_UPDATE_KEY: String? = null

    private var repositories: Preference? = null
    private var update: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SETTINGS_REPOSITORIES_KEY = getString(R.string.fragment_kaleido_settings_repo_key)
        SETTINGS_UPDATE_KEY = getString(R.string.fragment_kaleido_settings_update_key)
        repositories = findPreference(SETTINGS_REPOSITORIES_KEY)
        update = findPreference(SETTINGS_UPDATE_KEY)
    }

    override fun onResume() {
        super.onResume()

        repositories?.onPreferenceClickListener = this
        update?.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (preference.key == SETTINGS_REPOSITORIES_KEY) {
            (activity as? Listener)?.onRepositoriesClicked(this)
            return true
        } else if (preference.key == SETTINGS_UPDATE_KEY) {
            (activity as? Listener)?.onUpdateClicked(this)
            return true
        }
        return super.onPreferenceClick(preference)
    }

    interface Listener : BaseSettingsFragment.Listener {
        fun onRepositoriesClicked(fragment: GeneralSettingsFragment)
        fun onUpdateClicked(fragment: GeneralSettingsFragment)
    }
}