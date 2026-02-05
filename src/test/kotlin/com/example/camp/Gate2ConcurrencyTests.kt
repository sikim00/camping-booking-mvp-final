package com.example.camp

import com.example.camp.common.AlreadyBookedException
import com.example.camp.customer.service.CustomerBookingService
import com.example.camp.domain.camp.Camp
import com.example.camp.domain.camp.Site
import com.example.camp.domain.policy.RefundPolicyVersion
import com.example.camp.domain.user.User
import com.example.camp.repo.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.TestConstructor
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class Gate2ConcurrencyTests(
    private val campRepository: CampRepository,
    private val siteRepository: SiteRepository,
    private val refundPolicyVersionRepository: RefundPolicyVersionRepository,
    private val userRepository: UserRepository,
    private val customerBookingService: CustomerBookingService,
    private val bookingRepository: BookingRepository,
    private val bookingNightRepository: BookingNightRepository,
    private val paymentRepository: PaymentRepository,
    private val refundRepository: RefundRepository,
) : IntegrationTestBase() {

    @BeforeEach
    fun cleanupDb() {
        refundRepository.deleteAll()
        paymentRepository.deleteAll()
        bookingNightRepository.deleteAll()
        bookingRepository.deleteAll()
        refundPolicyVersionRepository.deleteAll()
        siteRepository.deleteAll()
        campRepository.deleteAll()
        userRepository.deleteAll()
    }

    

    @Test
    fun `A 동시 예약 - 하나만 성공`() {
        val seed = seedCampSite()
        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = LocalDate.now().plusDays(12)

        val exec = Executors.newFixedThreadPool(12)
        val tasks = (1..20).map { idx ->
            Callable {
                try {
                    customerBookingService.confirmBooking(
                        customerId = ((idx % 8) + 1).toLong(),
                        campId = seed.campId,
                        siteId = seed.siteId,
                        checkIn = checkIn,
                        checkOut = checkOut,
                        headCount = 2,
                        provider = "MOCK",
                        providerTxId = "tx-$idx",
                    )
                    true
                } catch (_: AlreadyBookedException) {
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

    @Test
    fun `B 중복예약 - 409 대응 예외`() {
        val seed = seedCampSite()
        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = LocalDate.now().plusDays(12)

        customerBookingService.confirmBooking(
            customerId = 1,
            campId = seed.campId,
            siteId = seed.siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        try {
            customerBookingService.confirmBooking(
                customerId = 2,
                campId = seed.campId,
                siteId = seed.siteId,
                checkIn = checkIn,
                checkOut = checkOut,
                headCount = 2,
                provider = "MOCK",
                providerTxId = "tx-2",
            )
            throw AssertionError("should fail")
        } catch (e: AlreadyBookedException) {
            // ok
        }

        assertThat(bookingRepository.count()).isEqualTo(1)
        assertThat(bookingNightRepository.count()).isEqualTo(2)
    }

    // 테스트 내 간단 DTO
    data class Seed(val campId: Long, val siteId: Long, val policyId: Long)
}
