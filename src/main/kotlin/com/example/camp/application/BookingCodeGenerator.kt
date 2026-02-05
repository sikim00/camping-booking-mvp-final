package com.example.camp.application

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class BookingCodeGenerator {
    private val random = SecureRandom()
    private val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(): String {
        val sb = StringBuilder("B")
        repeat(10) { sb.append(chars[random.nextInt(chars.length)]) }
        return sb.toString()
    }
}
