package com.example.camp.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets

class JwtAuthFilter(
    private val secret: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val auth = req.getHeader("Authorization")
        if (auth != null && auth.startsWith("Bearer ")) {
            val token = auth.removePrefix("Bearer ").trim()
            val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
            val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body

            val userId = (claims["userId"] as Number).toLong()
            val role = claims["role"] as String
            val principal = UserPrincipal(userId, role)

            val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
        }
        chain.doFilter(req, res)
    }
}
