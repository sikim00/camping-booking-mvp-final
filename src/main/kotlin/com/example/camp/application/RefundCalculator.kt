package com.example.camp.application

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class RefundCalculator(
    private val objectMapper: ObjectMapper,
) {

    /**
     * policyJson example:
     * {"timezone":"Asia/Seoul","rules":[{"daysBefore":7,"refundRate":1.0},{"daysBefore":0,"refundRate":0.0}],"fee":{"type":"FIXED","amount":0}}
     */
    fun calculateRefundAmount(
        policyJson: String,
        bookingTotal: BigDecimal,
        cancelDate: LocalDate,
        checkInDate: LocalDate,
    ): BigDecimal {
        val root = try {
            objectMapper.readTree(policyJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("invalid refund policy json")
        }

        val timezone = root.path("timezone").asText("Asia/Seoul")
        val rules = root.path("rules")
        if (!rules.isArray) {
            throw IllegalArgumentException("refund policy rules missing")
        }

        val daysBefore = daysBetween(cancelDate, checkInDate)

        // pick the highest daysBefore <= actual daysBefore (common policy style)
        var selectedRate: BigDecimal? = null
        var selectedThreshold: Int? = null
        for (rule in rules) {
            val threshold = rule.path("daysBefore").asInt(Int.MIN_VALUE)
            val rate = rule.path("refundRate").decimalValueOrNull()
            if (threshold == Int.MIN_VALUE || rate == null) continue

            if (threshold <= daysBefore && (selectedThreshold == null || threshold > selectedThreshold!!)) {
                selectedThreshold = threshold
                selectedRate = rate
            }
        }

        val refundRate = selectedRate ?: BigDecimal.ZERO

        // fee
        val feeNode = root.path("fee")
        val feeType = feeNode.path("type").asText("FIXED")
        val feeAmount = feeNode.path("amount").decimalValueOrNull() ?: BigDecimal.ZERO

        val gross = bookingTotal.multiply(refundRate)
        val net = when (feeType.uppercase()) {
            "FIXED" -> gross.subtract(feeAmount)
            "PERCENT" -> gross.subtract(bookingTotal.multiply(feeAmount))
            else -> gross
        }

        return net.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int {
        return kotlin.math.max(0, java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt())
    }

    private fun com.fasterxml.jackson.databind.JsonNode.decimalValueOrNull(): BigDecimal? {
        return when {
            this.isNumber -> this.decimalValue()
            this.isTextual -> this.asText().toBigDecimalOrNull()
            else -> null
        }
    }
}
