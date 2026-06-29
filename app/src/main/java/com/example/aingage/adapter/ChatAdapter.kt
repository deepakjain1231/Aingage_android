package com.example.aingage.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.aingage.R
import com.example.aingage.model.MessageItem

class ChatAdapter(
    private var items: List<MessageItem>,
    private val myParticipantId: Int
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.messageCard)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val container: LinearLayout = view.findViewById(R.id.messageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isMine = item.fromParticipantId == myParticipantId
        val ctx = holder.itemView.context

        // Message text (show filename for FILE type)
        holder.tvMessage.text = if (item.messageType == "FILE") item.filename else item.message
        holder.tvTime.text = formatTime(item.addedOn)

        when {
            item.messageType == "SYS-COPY" -> {
                // Orange centered system message
                holder.container.gravity = Gravity.CENTER
                holder.card.setCardBackgroundColor(ctx.getColor(R.color.aingage_orange))
                holder.tvMessage.setTextColor(ctx.getColor(R.color.white))
                holder.tvMessage.gravity = Gravity.CENTER
                holder.tvTime.visibility = View.GONE
                setMargins(holder.card, 16, 4, 16, 4)
            }
            isMine -> {
                // My message — teal, right-aligned
                holder.container.gravity = Gravity.END
                holder.card.setCardBackgroundColor(ctx.getColor(R.color.aingage_teal))
                holder.tvMessage.setTextColor(ctx.getColor(R.color.white))
                holder.tvMessage.gravity = Gravity.START
                holder.tvTime.visibility = View.VISIBLE
                holder.tvTime.setTextColor(ctx.getColor(R.color.white))
                setMargins(holder.card, 80, 4, 16, 4)
            }
            else -> {
                // Their message — white, left-aligned
                holder.container.gravity = Gravity.START
                holder.card.setCardBackgroundColor(ctx.getColor(R.color.white))
                holder.tvMessage.setTextColor(ctx.getColor(android.R.color.black))
                holder.tvMessage.gravity = Gravity.START
                holder.tvTime.visibility = View.VISIBLE
                holder.tvTime.setTextColor(ctx.getColor(android.R.color.darker_gray))
                setMargins(holder.card, 16, 4, 80, 4)
            }
        }
    }

    private fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        val density = view.context.resources.displayMetrics.density
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        params.setMargins(
            (left * density).toInt(),
            (top * density).toInt(),
            (right * density).toInt(),
            (bottom * density).toInt()
        )
        view.layoutParams = params
    }

    private fun formatTime(addedOn: String): String {
        // iOS format: "2026-05-28 15:18:39 PM" → "05/28/2026 15:18:39 PM"
        val parts = addedOn.split(" ")
        if (parts.size >= 2) {
            val dateParts = parts[0].split("-")
            if (dateParts.size == 3) {
                val formatted = "${dateParts[1]}/${dateParts[2]}/${dateParts[0]}"
                return "$formatted ${parts[1]}${if (parts.size > 2) " ${parts[2]}" else ""}"
            }
        }
        return addedOn
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<MessageItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun appendMessage(item: MessageItem) {
        items = items + item
        notifyItemInserted(items.size - 1)
    }
}
