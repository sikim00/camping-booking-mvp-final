package com.example.camp

import com.example.camp.domain.camp.Camp
import com.example.camp.domain.camp.Site
import com.example.camp.domain.policy.RefundPolicyVersion
import com.example.camp.domain.user.User
import com.example.camp.repo.*
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiIntegrationTests(
    private val mockMvc: MockMvc,
    private val campRepository: CampRepository,
    private val siteRepository: SiteRepository,
    private val refundPolicyVersionRepository: RefundPolicyVersionRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val bookingNightRepository: BookingNightRepository,
    private val paymentRepository: PaymentRepository,
    private val refundRepository: RefundRepository,
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
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

    private fun seed(): Pair<Long, Long> {
        if (userRepository.count() == 0L) {
            userRepository.save(User(email = "user1@test.com", passwordHash = "pw", role = "CUSTOMER"))
            userRepository.save(User(email = "user2@test.com", passwordHash = "pw", role = "CUSTOMER"))
        }
        val camp = campRepository.save(Camp(name = "캠프A", ownerId = 100, isActive = true))
        val site = siteRepository.save(
            Site(
                campId = camp.id!!,
                name = "사이트1",
                basePrice = 100000.toBigDecimal(),
                currency = "KRW",
                capacity = 4,
                isActive = true
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

    private fun bearer(userId: Long = 1L, role: String = "CUSTOMER"): String {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
        val token = Jwts.builder()
            .claim("userId", userId)
            .claim("role", role)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
        return "Bearer $token"
    }

    @Test
    fun `confirm booking API - ok and duplicate returns 409`() {
        val (campId, siteId) = seed()
        val checkIn = LocalDate.now().plusDays(10)
        val checkOut = LocalDate.now().plusDays(12)

        val body = """
            {
              "campId": %d,
              "siteId": %d,
              "checkIn": "%s",
              "checkOut": "%s",
              "headCount": 2,
              "provider": "MOCK",
              "providerTxId": "tx-1"
            }
        """.trimIndent().format(campId, siteId, checkIn, checkOut)

        mockMvc.post("/customer/bookings/confirm") {
            header("Authorization", bearer(1))
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.bookingId") { exists() }
            jsonPath("$.status") { exists() }
        }

        val body2 = body.replace("tx-1", "tx-2")
        mockMvc.post("/customer/bookings/confirm") {
            header("Authorization", bearer(2))
            contentType = MediaType.APPLICATION_JSON
            content = body2
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("ALREADY_BOOKED") }
        }

        assertThat(bookingRepository.count()).isEqualTo(1)
        assertThat(bookingNightRepository.count()).isEqualTo(2)
    }
}
