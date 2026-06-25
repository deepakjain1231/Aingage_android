package com.example.aingage.network

object ApiConstants {

    // Base URLs
    const val BASE_URL = "https://texting.iconnectgroup.com/api/"
    const val BASE_URL2 = "https://reviews.iconnectgroup.com/index.php/itextapi/"

    // Auth
    const val USER_LOGIN = "login/login"

    // Customers
    const val GET_CUSTOMERS = "ChatParticipant/GetCustomers"
    const val GET_CLIENTS = "values/checkclient?client="

    // Participants
    const val GET_EMPL = "ChatParticipant/GetParticipants?code=EMPL"       // Internal Members
    const val GET_LEAD = "ChatParticipant/GetParticipants?code=LEAD"       // Leads
    const val GET_CUST = "ChatParticipant/GetParticipants?code=CUST"       // Customers
    const val GET_ALL_PARTICIPANTS = "ChatParticipant/GetAllParticipants?code=EMPL"
    const val GET_PARTICIPANT_BY_MOBILE = "ChatParticipant/GetChatParticipantByMobile?mobile=%s"

    // Search
    const val SEARCH_EMPL = "ChatParticipant/Search/EMPL/"
    const val SEARCH_LEAD = "ChatParticipant/Search/LEAD/"
    const val SEARCH_CUST = "ChatParticipant/Search/CUST/"

    // Conversation / Messages
    const val MESSAGE_WITH_PARTICIPANT = "Conversation/MessagesBetween?"
    const val POST_MESSAGE = "Conversation/PostMessage/"
    const val COPY_MESSAGES = "Conversation/CopyMessages?"

    // Files
    const val POST_FILES = "conversation/postfiles/"
    const val GET_FILES = "Files/Get/"

    // Notifications
    const val GET_NOTIFICATION_CUSTOMER_DETAIL = "Notification/GetCustomerNotificationDetails"
    const val SEND_NOTIFICATION_DELIVERY = "Notification/SendDeliveryDateNotification"
    const val SEND_NOTIFICATION_PAYMENT = "Notification/SendPendingPaymentNotification"

    // Google Reviews
    const val GOOGLE_REVIEW_BASE = "https://mybusiness.googleapis.com/v4/"
    const val GET_ALL_REVIEWS = "getAllReview?"
    const val REVIEW_TEXT_DETAIL = "reviewTextDetail?"
    const val REPLY_GOOGLE_REVIEWS = "https://reviews.iconnectgroup.com/itextapi/replytoGoogleReviews?"
}
