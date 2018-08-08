package com.github.gnastnosaj.filter.kaleidoscope.ui.viewbinder

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.github.gnastnosaj.filter.kaleidoscope.R
import me.drakeet.multitype.ItemViewBinder
import org.jetbrains.anko.*

class HeadViewBinder : ItemViewBinder<Map<*, *>, HeadViewBinder.ViewHolder>() {
    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(
                with(AnkoContext.create(parent.context, parent)) {
                    linearLayout {
                        lparams(matchParent, dip(32))
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        view {
                            backgroundColorResource = R.color.colorAccent
                        }.lparams {
                            width = dip(4)
                            height = matchParent
                            topMargin = dip(4)
                            bottomMargin = dip(4)
                        }
                        textView {
                            id = R.id.title
                        }.lparams {
                            leftMargin = dip(8)

                        }
                    }
                }
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Map<*, *>) {
        viewHolder.titleView?.text = (item as? Map<String, String>)?.get("title")
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var titleView: TextView? = null

        init {
            titleView = itemView.findViewById(R.id.title)
        }
    }
}
