package com.github.gnastnosaj.filter.kaleidoscope.ui.adapter

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.github.gnastnosaj.filter.dsl.core.Catalog
import com.github.gnastnosaj.filter.kaleidoscope.ui.fragment.WaterfallFragment

class CatalogAdapter(context: Context, fm: FragmentManager, catalog: Catalog) : FragmentPagerAdapter(fm) {
    private val connections = catalog.connections?.entries?.flatMap {
        arrayListOf(it)
    }
    private val fragments = arrayListOf<WaterfallFragment>()

    init {
        connections?.forEach {
            fragments.add(WaterfallFragment.newInstance(it.value))
        }
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return connections?.get(position)?.key
    }
}