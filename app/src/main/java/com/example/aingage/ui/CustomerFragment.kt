package com.example.aingage.ui

import com.example.aingage.network.ApiConstants

class CustomerFragment : ContactListFragment() {
    override val listEndpoint = ApiConstants.GET_CUST
    override val searchEndpoint = ApiConstants.SEARCH_CUST
}
