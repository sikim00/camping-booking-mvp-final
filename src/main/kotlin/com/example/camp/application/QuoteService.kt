package com.example.camp.application

import com.example.camp.repo.SiteRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class QuoteResult(
    val nights: Int,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val total: BigDecimal,
    val currency: String
)

@Service
class QuoteService(
    private val siteRepository: SiteRepository
) {
    fun quote(siteId: Long, checkIn: LocalDate, checkOut: LocalDate): QuoteResult {
        require(checkIn.isBefore(checkOut)) { "checkInDate must be before checkOutDate" }
        val nights = ChronoUnit.DAYS.between(checkIn, checkOut).toInt()
        require(nights > 0) { "nights must be > 0" }

        val site = siteRepository.findById(siteId).orElseThrow { IllegalArgumentException("site not found") }
        val subtotal = site.basePrice.multiply(BigDecimal(nights))
        val discount = BigDecimal.ZERO
        val total = subtotal.subtract(discount)

        return QuoteResult(nights, subtotal, discount, total, site.currency)
    }
}
