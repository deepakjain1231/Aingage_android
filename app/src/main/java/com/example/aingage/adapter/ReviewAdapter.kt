package com.example.aingage.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.aingage.R
import com.example.aingage.model.ReviewItem

class ReviewAdapter(
    private var items: List<ReviewItem>,
    private val onItemClick: (ReviewItem) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    // Avatar background colors based on first letter
    private val avatarColors = listOf(
        "#607D8B", "#9C27B0", "#3F51B5", "#009688",
        "#FF5722", "#795548", "#E91E63", "#2196F3"
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvInitial: TextView = view.findViewById(R.id.tvAvatarInitial)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvReview: TextView = view.findViewById(R.id.tvReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.displayName
        holder.ratingBar.rating = item.starRating.toFloat()
        holder.tvDate.text = item.createDate
        holder.tvReview.text = item.comment

        // Avatar: load image if URL present, else show colored initial
        if (item.profilePhotoUrl.isNotEmpty()) {
            holder.ivAvatar.visibility = View.VISIBLE
            holder.tvInitial.visibility = View.GONE
            holder.ivAvatar.load(item.profilePhotoUrl) {
                transformations(CircleCropTransformation())
                error(R.drawable.ic_avatar_placeholder)
            }
        } else {
            holder.ivAvatar.visibility = View.GONE
            holder.tvInitial.visibility = View.VISIBLE
            val initial = item.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            holder.tvInitial.text = initial
            val colorIndex = (initial.firstOrNull()?.code ?: 0) % avatarColors.size
            holder.tvInitial.background.mutate().also {
                (it as? android.graphics.drawable.GradientDrawable)
                    ?.setColor(Color.parseColor(avatarColors[colorIndex]))
            }
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<ReviewItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
