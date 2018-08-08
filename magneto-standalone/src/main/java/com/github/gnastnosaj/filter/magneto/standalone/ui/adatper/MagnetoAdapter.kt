package com.github.gnastnosaj.filter.magneto.standalone.ui.adatper

import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.github.gnastnosaj.filter.magneto.standalone.R
import com.github.gnastnosaj.filter.magneto.standalone.ui.adatper.MagnetoAdapter.ViewHolder.Companion.ID_ADD_TIME
import com.github.gnastnosaj.filter.magneto.standalone.ui.adatper.MagnetoAdapter.ViewHolder.Companion.ID_FILES
import com.github.gnastnosaj.filter.magneto.standalone.ui.adatper.MagnetoAdapter.ViewHolder.Companion.ID_POPULARITY
import com.github.gnastnosaj.filter.magneto.standalone.ui.adatper.MagnetoAdapter.ViewHolder.Companion.ID_SIZE
import com.github.gnastnosaj.filter.magneto.standalone.ui.adatper.MagnetoAdapter.ViewHolder.Companion.ID_TITLE
import com.shizhefei.mvc.IDataAdapter
import org.jetbrains.anko.*


class MagnetoAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), IDataAdapter<List<Map<String, String>>> {
    private val data = arrayListOf<Map<String, String>>()

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        val viewHolder = holder as ViewHolder
        viewHolder.sizeView?.text = item["size"]
        viewHolder.filesView?.text = item["files"]
        viewHolder.titleView?.text = item["title"]
        viewHolder.addTimeView?.text = item["addTime"]
        viewHolder.popularityView?.text = item["popularity"]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
                with(AnkoContext.create(parent.context, parent)) {
                    linearLayout {
                        lparams {
                            width = matchParent
                            height = wrapContent
                        }
                        backgroundResource = R.drawable.item_magneto
                        orientation = LinearLayout.VERTICAL
                        isClickable = true
                        minimumHeight = dip(60)
                        linearLayout {
                            orientation = LinearLayout.HORIZONTAL
                            textView {
                                id = ID_SIZE
                                textColorResource = R.color.light_blue_300
                            }.lparams {
                                weight = 1f
                                padding = dip(5)
                            }
                            textView {
                                id = ID_FILES
                                textColorResource = R.color.black
                                gravity = Gravity.END
                            }.lparams {
                                weight = 1f
                                padding = dip(5)
                            }
                        }.lparams(matchParent, wrapContent)
                        textView {
                            id = ID_TITLE
                            textColorResource = R.color.black
                        }.lparams {
                            width = matchParent
                            height = wrapContent
                            setPadding(dip(5), 0, dip(5), 0)
                        }
                        linearLayout {
                            orientation = LinearLayout.HORIZONTAL
                            textView {
                                id = ID_ADD_TIME
                                textColorResource = R.color.black
                            }.lparams {
                                weight = 1f
                                padding = dip(5)
                            }
                            textView {
                                id = ID_POPULARITY
                                textColorResource = R.color.black
                                gravity = Gravity.END
                            }.lparams {
                                weight = 1f
                                padding = dip(5)
                            }
                        }.lparams(matchParent, wrapContent)
                    }
                }
        )
    }

    override fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    override fun getData(): List<Map<String, String>> {
        return data
    }

    override fun notifyDataChanged(data: List<Map<String, String>>?, isRefresh: Boolean) {
        if (isRefresh) {
            this.data.clear()
        }
        data?.apply {
            this@MagnetoAdapter.data.addAll(this)
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        companion object {
            const val ID_SIZE = 0
            const val ID_FILES = 1
            const val ID_TITLE = 2
            const val ID_ADD_TIME = 3
            const val ID_POPULARITY = 4
        }

        var sizeView: TextView? = null

        var filesView: TextView? = null

        var titleView: TextView? = null

        var addTimeView: TextView? = null

        var popularityView: TextView? = null

        init {
            sizeView = itemView.findViewById(ID_SIZE)
            filesView = itemView.findViewById(ID_FILES)
            titleView = itemView.findViewById(ID_TITLE)
            addTimeView = itemView.findViewById(ID_ADD_TIME)
            popularityView = itemView.findViewById(ID_POPULARITY)
        }
    }
}