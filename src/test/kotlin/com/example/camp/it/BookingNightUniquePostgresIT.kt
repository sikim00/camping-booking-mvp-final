package com.example.camp.it

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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import java.time.LocalDate

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingNightUniquePostgresIT(
    private val campRepository: CampRepository,
    private val siteRepository: SiteRepository,
    private val refundPolicyVersionRepository: RefundPolicyVersionRepository,
    private val userRepository: UserRepository,
    private val customerBookingService: CustomerBookingService,
    private val bookingRepository: BookingRepository,
    private val bookingNightRepository: BookingNightRepository,
    private val paymentRepository: PaymentRepository,
    private val refundRepository: RefundRepository,
) : PostgresITBase() {

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

    private fun seed(): Pair<Long, Long> {
        userRepository.save(User(email = "user1@test.com", passwordHash = "pw", role = "CUSTOMER"))
        userRepository.save(User(email = "user2@test.com", passwordHash = "pw", role = "CUSTOMER"))

        val camp = campRepository.save(Camp(name = "캠프A", ownerId = 100, isActive = true))
        val site = siteRepository.save(
            Site(
                campId = camp.id!!,
                name = "사이트1",
                basePrice = 100000.toBigDecimal(),
                currency = "KRW",
                capacity = 4,
                isActive = true,
            )
        )

        refundPolicyVersionRepository.save(
            RefundPolicyVersion(
                campId = camp.id!!,
                version = 1,
                isActive = true,
                ruleJson = "{\"version\":1,\"rules\":[{\"daysBefore\":7,\"refundRate\":1.0}]}"
            )
        )

        return camp.id!! to site.id!!
    }

    @Test
    fun `unique constraint works on postgres - duplicate booking rejected`() {
        val (campId, siteId) = seed()
        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = LocalDate.now().plusDays(12)

        customerBookingService.confirmBooking(
            customerId = 1,
            campId = campId,
            siteId = siteId,
            checkIn = checkIn,
            checkOut = checkOut,
            headCount = 2,
            provider = "MOCK",
            providerTxId = "tx-1",
        )

        try {
            customerBookingService.confirmBooking(
                customerId = 2,
                campId = campId,
                siteId = siteId,
                checkIn = checkIn,
                checkOut = checkOut,
                headCount = 2,
                provider = "MOCK",
                providerTxId = "tx-2",
            )
            throw AssertionError("should fail")
        } catch (_: AlreadyBookedException) {
            // ok
        }

        assertThat(bookingRepository.count()).isEqualTo(1)
        assertThat(bookingNightRepository.count()).isEqualTo(2)
    }
}
