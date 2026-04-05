package com.beackers.dumbhome

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleTextAdapter(
    private var items: List<String>,
    private val onClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<SimpleTextAdapter.TextVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_row, parent, false) as TextView
        return TextVH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TextVH, position: Int) {
        holder.text.text = items[position]
        holder.text.setOnClickListener { onClick?.invoke(position) }
    }

    fun submit(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    class TextVH(val text: TextView) : RecyclerView.ViewHolder(text)
}
