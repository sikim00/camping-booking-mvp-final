package com.example.camp.customer

import com.example.camp.customer.dto.CancelRefundRequest
import com.example.camp.customer.dto.ConfirmBookingRequest
import com.example.camp.customer.service.CustomerBookingService
import com.example.camp.security.SecurityUtils
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/customer")
class CustomerBookingController(
    private val bookingService: CustomerBookingService
) {

    @PostMapping("/bookings/confirm")
    fun confirmBooking(@RequestBody req: ConfirmBookingRequest) =
        bookingService.confirmBooking(
            customerId = SecurityUtils.principal().userId,
            campId = req.campId,
            siteId = req.siteId,
            checkIn = req.checkIn,
            checkOut = req.checkOut,
            headCount = req.headCount,
            provider = req.provider,
            providerTxId = req.providerTxId
        )

    @PostMapping("/bookings/{bookingId}/cancel")
    fun cancelAndRefund(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody req: CancelRefundRequest
    ) =
        bookingService.cancelAndRefund(
            customerId = SecurityUtils.principal().userId,
            bookingId = bookingId,
            idempotencyKey = idempotencyKey,
            reason = req.reason,
            cancelDate = req.cancelDate
        )
}
