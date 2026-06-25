package com.example.aingage.ui

import com.example.aingage.network.ApiConstants

class LeadsFragment : ContactListFragment() {
    override val listEndpoint = ApiConstants.GET_LEAD
    override val searchEndpoint = ApiConstants.SEARCH_LEAD
}
