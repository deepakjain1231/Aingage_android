package com.example.aingage.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aingage.R
import com.example.aingage.model.ContactItem

class ContactAdapter(
    private var items: List<ContactItem>,
    private val onItemClick: (ContactItem) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPhone: TextView = view.findViewById(R.id.tvPhone)
        val tvUnread: TextView = view.findViewById(R.id.tvUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPhone.text = item.mobile

        if (item.unreadCount > 0) {
            holder.tvUnread.visibility = View.VISIBLE
            holder.tvUnread.text = item.unreadCount.toString()
        } else {
            holder.tvUnread.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<ContactItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
