package com.example.camp.customer.service

import com.example.camp.application.BookingCodeGenerator
import com.example.camp.application.QuoteService
import com.example.camp.application.RefundCalculator
import com.example.camp.common.AlreadyBookedException
import com.example.camp.customer.dto.CancelRefundRequest
import com.example.camp.customer.dto.ConfirmBookingRequest
import com.example.camp.customer.dto.ConfirmBookingResponse
import com.example.camp.customer.dto.RefundResponse
import com.example.camp.domain.booking.Booking
import com.example.camp.domain.booking.BookingNight
import com.example.camp.domain.booking.BookingStatus
import com.example.camp.domain.payment.Payment
import com.example.camp.domain.payment.PaymentStatus
import com.example.camp.domain.refund.Refund
import com.example.camp.domain.refund.RefundStatus
import com.example.camp.repo.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class CustomerBookingService(
    private val bookingRepository: BookingRepository,
    private val bookingNightRepository: BookingNightRepository,
    private val paymentRepository: PaymentRepository,
    private val refundRepository: RefundRepository,
    private val campRepository: CampRepository,
    private val siteRepository: SiteRepository,
    private val refundPolicyVersionRepository: RefundPolicyVersionRepository,
    private val quoteService: QuoteService,
    private val bookingCodeGenerator: BookingCodeGenerator,
    private val refundCalculator: RefundCalculator
) {

    @Transactional
    fun confirmBooking(
        customerId: Long,
        campId: Long,
        siteId: Long,
        checkIn: LocalDate,
        checkOut: LocalDate,
        headCount: Int = 1,
        provider: String?,
        providerTxId: String?
    ): ConfirmBookingResponse {
        val camp = campRepository.findById(campId).orElseThrow { IllegalArgumentException("camp not found") }
        if (!camp.isActive) throw IllegalStateException("camp inactive")

        val site = siteRepository.findById(siteId).orElseThrow { IllegalArgumentException("site not found") }
        if (!site.isActive || site.campId != campId) throw IllegalStateException("site invalid")
        require(headCount in 1..site.capacity) { "HEAD_COUNT_EXCEEDS_CAPACITY" }

        val quote = quoteService.quote(siteId, checkIn, checkOut)

        val policy = refundPolicyVersionRepository.findFirstByCampIdAndIsActiveTrueOrderByVersionDesc(campId)

        val booking = Booking(
            bookingCode = bookingCodeGenerator.generate(),
            customerId = customerId,
            campId = campId,
            siteId = siteId,
            headCount = headCount,
            checkInDate = checkIn,
            checkOutDate = checkOut,
            nightsCount = quote.nights,
            status = BookingStatus.CONFIRMED,
            subtotal = quote.subtotal,
            discount = quote.discount,
            total = quote.total,
            currency = quote.currency,
            refundPolicyVersionId = policy?.id,
            refundRuleSnapshotJson = policy?.ruleJson
        )

        val savedBooking = bookingRepository.save(booking)

        // booking_nights insert (unique: site_id + night_date)
        try {
            var d = checkIn
            while (d.isBefore(checkOut)) {
                bookingNightRepository.save(
                    BookingNight(
                        bookingId = savedBooking.id!!,
                        siteId = siteId,
                        nightDate = d
                    )
                )
                d = d.plusDays(1)
            }
            bookingNightRepository.flush()
        } catch (e: DataIntegrityViolationException) {
            // rollback will remove booking; rethrow as duplicate booking
            throw AlreadyBookedException("이미 예약된 날짜가 포함되어 있습니다.")
        }

        val payment = Payment(
            bookingId = savedBooking.id!!,
            status = PaymentStatus.APPROVED,
            provider = provider,
            providerTxId = providerTxId,
            amount = quote.total,
            currency = quote.currency,
            approvedAt = OffsetDateTime.now()
        )
        paymentRepository.save(payment)

        return ConfirmBookingResponse(
            bookingId = savedBooking.id!!,
            bookingCode = savedBooking.bookingCode,
            status = savedBooking.status.name,
            total = savedBooking.total.toPlainString(),
            currency = savedBooking.currency
        )
    }

    @Transactional
    fun cancelAndRefund(
        customerId: Long,
        bookingId: Long,
        idempotencyKey: String,
        reason: String?,
        cancelDate: LocalDate = LocalDate.now()
    ): RefundResponse {
        val booking = bookingRepository.findById(bookingId).orElseThrow { IllegalArgumentException("booking not found") }
if (booking.customerId != customerId) throw IllegalStateException("forbidden")

// idempotency (must be checked before status check)
val existing = refundRepository.findByIdempotencyKey(idempotencyKey)
if (existing != null) {
    return RefundResponse(
        refundId = existing.id!!,
        status = existing.status.name,
        requestedAmount = existing.requestedAmount.toPlainString(),
        approvedAmount = existing.approvedAmount?.toPlainString(),
        currency = existing.currency
    )
}

if (booking.status != BookingStatus.CONFIRMED) throw IllegalStateException("not refundable")

val snapshotJson = booking.refundRuleSnapshotJson ?: throw IllegalStateException("refund policy missing")
        val approvedAmount = refundCalculator.calculateRefundAmount(
            policyJson = snapshotJson,
            bookingTotal = booking.total,
            cancelDate = cancelDate,
            checkInDate = booking.checkInDate,
        )

        val refund = Refund(
            bookingId = bookingId,
            idempotencyKey = idempotencyKey,
            reason = reason,
            requestedAmount = booking.total,
            approvedAmount = approvedAmount,
            currency = booking.currency,
            status = RefundStatus.APPROVED,
            providerRefundId = "mock-${idempotencyKey}"
        )
        val saved = refundRepository.save(refund)

        booking.status = BookingStatus.CANCELLED
        bookingRepository.save(booking)

        // Free nights
        bookingNightRepository.deleteByBookingId(bookingId)

        return RefundResponse(
            refundId = saved.id!!,
            status = saved.status.name,
            requestedAmount = saved.requestedAmount.toPlainString(),
            approvedAmount = saved.approvedAmount?.toPlainString(),
            currency = saved.currency
        )
    }
}
