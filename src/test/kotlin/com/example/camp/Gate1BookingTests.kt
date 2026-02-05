package com.example.camp


import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import com.example.camp.domain.booking.BookingStatus
import com.example.camp.domain.camp.Camp
import com.example.camp.domain.camp.Site
import com.example.camp.domain.policy.RefundPolicyVersion
import com.example.camp.domain.user.User
import com.example.camp.repo.BookingNightRepository
import com.example.camp.repo.BookingRepository
import com.example.camp.repo.CampRepository
import com.example.camp.repo.PaymentRepository
import com.example.camp.repo.RefundPolicyVersionRepository
import com.example.camp.repo.RefundRepository
import com.example.camp.repo.SiteRepository
import com.example.camp.repo.UserRepository
import com.example.camp.customer.service.CustomerBookingService
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

class Gate1BookingTests(
    private val campRepository: CampRepository,
    private val siteRepository: SiteRepository,
    private val refundPolicyVersionRepository: RefundPolicyVersionRepository,
    private val userRepository: UserRepository,
    private val customerBookingService: CustomerBookingService,
    private val bookingRepository: BookingRepository,
    private val bookingNightRepository: BookingNightRepository,
    private val paymentRepository: PaymentRepository,
    private val refundRepository: RefundRepository,
    private val objectMapper: ObjectMapper,
) : IntegrationTestBase() {

@BeforeEach
fun cleanupDb() {
    // order matters due to references/constraints
    refundRepository.deleteAll()
    paymentRepository.deleteAll()
    bookingNightRepository.deleteAll()
    bookingRepository.deleteAll()
    refundPolicyVersionRepository.deleteAll()
    siteRepository.deleteAll()
    campRepository.deleteAll()
    userRepository.deleteAll()
}

    private fun seedCampSite(): Seed {
// seed users (ids will start from 1 in fresh schema)
if (userRepository.count() == 0L) {
    (1..8).forEach { i ->
        userRepository.save(
            User(
                email = "user${i}@test.com",
                passwordHash = "pw",
                role = "CUSTOMER"
            )
        )
    }
}

        val camp = campRepository.save(
            Camp(
                name = "캠프A",
                ownerId = 100,
                isActive = true,
            )
        )
        val site = siteRepository.save(
            Site(
                campId = camp.id!!,
                name = "사이트1",
                basePrice = 100000.toBigDecimal(),
                currency = "KRW",
                isActive = true,
            )
        )

        // 간단 정책: 7일 전까지 100% / 그 이후 0% (수수료 0)
        val ruleJsonText = """
            {
              "timezone": "Asia/Seoul",
              "rules": [
                { "daysBefore": 7, "refundRate": 1.0 },
                { "daysBefore": 0, "refundRate": 0.0 }
              ],
              "fee": { "type": "FIXED", "amount": 0 }
            }
        """.trimIndent()

        val policy = refundPolicyVersionRepository.save(
            RefundPolicyVersion(
                campId = camp.id!!,
                version = 1,
                isActive = true,
                ruleJson = ruleJsonText,
            )
        )

        return Seed(camp.id!!, site.id!!, policy.id!!)
    }

    @Test
    fun `A 예약 성공`() {
        val seed = seedCampSite()

        val booking = customerBookingService.confirmBooking(
            customerId = 1,
            campId = seed.campId,
            siteId = seed.siteId,
            checkIn = LocalDate.now().plusDays(10),
            checkOut = LocalDate.now().plusDays(12),
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        assertThat(booking.status).isEqualTo(BookingStatus.CONFIRMED.name)
        assertThat(booking.bookingCode).isNotBlank()
        assertThat(bookingRepository.count()).isEqualTo(1)
        assertThat(bookingNightRepository.count()).isEqualTo(2)
    }

    @Test
    fun `B 중복예약 차단`() {
        val seed = seedCampSite()
        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = LocalDate.now().plusDays(12)

        customerBookingService.confirmBooking(
            customerId = 1,
            campId = seed.campId,
            siteId = seed.siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        assertThrows<RuntimeException> {
            customerBookingService.confirmBooking(
                customerId = 2,
                campId = seed.campId,
                siteId = seed.siteId,
                checkIn = checkIn,
                checkOut = checkOut,
                provider = "MOCK",
                providerTxId = "tx-2",
            )
        }

        assertThat(bookingRepository.count()).isEqualTo(1)
        assertThat(bookingNightRepository.count()).isEqualTo(2)
    }

    @Test
    fun `C 취소-환불 성공 및 D 멱등성`() {
        val seed = seedCampSite()
        val booking = customerBookingService.confirmBooking(
            customerId = 1,
            campId = seed.campId,
            siteId = seed.siteId,
            checkIn = LocalDate.now().plusDays(10),
            checkOut = LocalDate.now().plusDays(12),
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        val idempotencyKey = "idem-1"

        val refund1 = customerBookingService.cancelAndRefund(
            customerId = 1,
            bookingId = booking.bookingId,
            idempotencyKey = idempotencyKey,
            reason = "변경",
        )

        val refund2 = customerBookingService.cancelAndRefund(
            customerId = 1,
            bookingId = booking.bookingId,
            idempotencyKey = idempotencyKey,
            reason = "변경",
        )

        assertThat(refund1.refundId).isEqualTo(refund2.refundId)
        val updated = bookingRepository.findById(booking.bookingId).get()
        assertThat(updated.status).isEqualTo(BookingStatus.CANCELLED)
    }

    @Test
    fun `E 동시 예약 경쟁 - 하나만 성공`() {
        val seed = seedCampSite()
        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = LocalDate.now().plusDays(12)

        val exec = Executors.newFixedThreadPool(8)
        val tasks = (1..8).map { idx ->
            Callable {
                try {
                    customerBookingService.confirmBooking(
                        customerId = idx.toLong(),
                        campId = seed.campId,
                        siteId = seed.siteId,
                        checkIn = checkIn,
                        checkOut = checkOut,
                        provider = "MOCK",
                        providerTxId = "tx-$idx",
                    )
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        val results = exec.invokeAll(tasks).map { it.get() }
        exec.shutdown()

        assertThat(results.count { it }).isEqualTo(1)
        assertThat(bookingRepository.count()).isEqualTo(1)
        assertThat(bookingNightRepository.count()).isEqualTo(2)
    }

    // 테스트 내 간단 DTO
    data class Seed(val campId: Long, val siteId: Long, val policyId: Long)
}