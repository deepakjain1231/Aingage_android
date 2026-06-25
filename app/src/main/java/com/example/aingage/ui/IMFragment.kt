package com.example.aingage.ui

import com.example.aingage.network.ApiConstants

class IMFragment : ContactListFragment() {
    override val listEndpoint = ApiConstants.GET_EMPL
    override val searchEndpoint = ApiConstants.SEARCH_EMPL
}
