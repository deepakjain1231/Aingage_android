package com.example.aingage.model

data class ReviewItem(
    val id: String,
    val displayName: String,
    val comment: String,
    val starRating: Int,
    val createDate: String,
    val profilePhotoUrl: String,
    val phoneNo: String
)
