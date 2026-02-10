// 파일 생성: src/test/kotlin/com/example/camp/Gate3CancelRefundTests.kt
package com.example.camp

import com.example.camp.customer.service.CustomerBookingService
import com.example.camp.domain.camp.Camp
import com.example.camp.domain.camp.Site
import com.example.camp.domain.policy.RefundPolicyVersion
import com.example.camp.domain.booking.BookingStatus
import com.example.camp.repo.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test") // Gate3는 H2(test)로 돌리는 게 정답
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class Gate3CancelRefundTests(
    private val customerBookingService: CustomerBookingService,
    private val campRepository: CampRepository,
    private val siteRepository: SiteRepository,
    private val refundPolicyVersionRepository: RefundPolicyVersionRepository,
    private val bookingRepository: BookingRepository,
    private val bookingNightRepository: BookingNightRepository,
    private val paymentRepository: PaymentRepository,
    private val refundRepository: RefundRepository,
) : IntegrationTestBase() {

    @BeforeEach
    fun cleanup() {
        refundRepository.deleteAll()
        paymentRepository.deleteAll()
        bookingNightRepository.deleteAll()
        bookingRepository.deleteAll()
        refundPolicyVersionRepository.deleteAll()
        siteRepository.deleteAll()
        campRepository.deleteAll()
    }

    private fun seedCampSitePolicy(capacity: Int = 4): Pair<Long, Long> {
        val camp = campRepository.save(
            Camp(
                name = "캠프A",
                ownerId = 100L,
                isActive = true,
            )
        )

        val site = siteRepository.save(
            Site(
                campId = camp.id!!,
                name = "사이트1",
                basePrice = BigDecimal("100000.00"),
                currency = "KRW",
                capacity = capacity,
                isActive = true,
            )
        )

        refundPolicyVersionRepository.save(
            RefundPolicyVersion(
                campId = camp.id!!,
                version = 1,
                isActive = true,
                ruleJson = """{"version":1,"rules":[{"daysBefore":7,"refundRate":1.0},{"daysBefore":0,"refundRate":0.0}]}""",
            )
        )

        return camp.id!! to site.id!!
    }

    @Test
    fun `Gate3 - 취소 및 환불 성공`() {
        val (campId, siteId) = seedCampSitePolicy()

        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = checkIn.plusDays(2) // 2박

        val bookingRes = customerBookingService.confirmBooking(
            customerId = 1L,
            campId = campId,
            siteId = siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        val refundRes = customerBookingService.cancelAndRefund(
            customerId = 1L,
            bookingId = bookingRes.bookingId,
            idempotencyKey = "idem-1",
            reason = "개인 사정",
            cancelDate = LocalDate.now(),
        )

        val booking = bookingRepository.findById(bookingRes.bookingId).orElseThrow()
        assertThat(booking.status).isEqualTo(BookingStatus.CANCELLED)

        assertThat(refundRes.status).isEqualTo("APPROVED")
        assertThat(refundRepository.count()).isEqualTo(1)
        assertThat(bookingNightRepository.count()).isEqualTo(0) // nights free
    }

    @Test
    fun `Gate3 - 같은 idempotencyKey 는 멱등하게 동일 Refund 반환`() {
        val (campId, siteId) = seedCampSitePolicy()

        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = checkIn.plusDays(2)

        val bookingRes = customerBookingService.confirmBooking(
            customerId = 1L,
            campId = campId,
            siteId = siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        val r1 = customerBookingService.cancelAndRefund(
            customerId = 1L,
            bookingId = bookingRes.bookingId,
            idempotencyKey = "idem-1",
            reason = "A",
            cancelDate = LocalDate.now(),
        )

        val r2 = customerBookingService.cancelAndRefund(
            customerId = 1L,
            bookingId = bookingRes.bookingId,
            idempotencyKey = "idem-1",
            reason = "B",
            cancelDate = LocalDate.now(),
        )

        assertThat(r2.refundId).isEqualTo(r1.refundId)
        assertThat(refundRepository.count()).isEqualTo(1)
    }

    @Test
    fun `Gate3 - 다른 idempotencyKey 로 중복 취소 시도하면 실패`() {
        val (campId, siteId) = seedCampSitePolicy()

        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = checkIn.plusDays(2)

        val bookingRes = customerBookingService.confirmBooking(
            customerId = 1L,
            campId = campId,
            siteId = siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        customerBookingService.cancelAndRefund(
            customerId = 1L,
            bookingId = bookingRes.bookingId,
            idempotencyKey = "idem-1",
            reason = "A",
            cancelDate = LocalDate.now(),
        )

        assertThatThrownBy {
            customerBookingService.cancelAndRefund(
                customerId = 1L,
                bookingId = bookingRes.bookingId,
                idempotencyKey = "idem-2",
                reason = "B",
                cancelDate = LocalDate.now(),
            )
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(refundRepository.count()).isEqualTo(1)
    }

    @Test
    fun `Gate3 - 타인 예약 취소 시도는 forbidden`() {
        val (campId, siteId) = seedCampSitePolicy()

        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = checkIn.plusDays(2)

        val bookingRes = customerBookingService.confirmBooking(
            customerId = 1L,
            campId = campId,
            siteId = siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        assertThatThrownBy {
            customerBookingService.cancelAndRefund(
                customerId = 2L, // 다른 고객
                bookingId = bookingRes.bookingId,
                idempotencyKey = "idem-x",
                reason = "B",
                cancelDate = LocalDate.now(),
            )
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(refundRepository.count()).isEqualTo(0)
    }

    @Test
    fun `Gate3 - 취소 후 동일 날짜 재예약 가능`() {
        val (campId, siteId) = seedCampSitePolicy()

        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = checkIn.plusDays(2)

        val b1 = customerBookingService.confirmBooking(
            customerId = 1L,
            campId = campId,
            siteId = siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        customerBookingService.cancelAndRefund(
            customerId = 1L,
            bookingId = b1.bookingId,
            idempotencyKey = "idem-1",
            reason = "A",
            cancelDate = LocalDate.now(),
        )

        // nights freed 되었으므로 동일 구간 재예약 성공해야 함
        val b2 = customerBookingService.confirmBooking(
            customerId = 2L,
            campId = campId,
            siteId = siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-2",
        )

        assertThat(b2.bookingId).isNotNull
        assertThat(bookingRepository.count()).isEqualTo(2)
        assertThat(bookingNightRepository.count()).isEqualTo(2) // 2박
    }
}
