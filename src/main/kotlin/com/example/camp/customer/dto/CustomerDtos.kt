package com.example.camp.customer.dto

import java.time.LocalDate

data class ConfirmBookingRequest(
    val campId: Long,
    val siteId: Long,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val provider: String? = null,
    val providerTxId: String? = null
)

data class ConfirmBookingResponse(
    val bookingId: Long,
    val bookingCode: String,
    val status: String,
    val total: String,
    val currency: String
)

data class CancelRefundRequest(
    val reason: String? = null,
    val cancelDate: LocalDate
)

data class RefundResponse(
    val refundId: Long,
    val status: String,
    val requestedAmount: String,
    val approvedAmount: String?,
    val currency: String
)


data class QuoteRequest(
    val siteId: Long,
    val checkInDate: LocalDate,
    val checkOutDate: LocalDate
)
