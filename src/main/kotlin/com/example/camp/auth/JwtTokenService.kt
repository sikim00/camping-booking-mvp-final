package com.example.camp.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class JwtTokenService(
    @Value("\${app.jwt.secret}") private val secret: String
) {
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8)) }

    fun issueAccessToken(userId: Long, role: String, ttl: Duration = Duration.ofMinutes(15)): String {
        val now = Instant.now()
        val exp = now.plus(ttl)
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(exp))
            .claim("userId", userId)
            .claim("role", role)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
}
