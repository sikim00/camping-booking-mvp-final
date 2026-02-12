package com.example.camp.auth

import com.example.camp.domain.auth.RefreshToken
import com.example.camp.domain.user.User
import com.example.camp.repo.RefreshTokenRepository
import com.example.camp.repo.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*

data class RegisterRequest(val email: String, val password: String, val role: String)
data class LoginRequest(val email: String, val password: String)
data class TokenResponse(val accessToken: String, val refreshToken: String)
data class RefreshRequest(val refreshToken: String)

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService
) {
    private val random = SecureRandom()

    fun register(req: RegisterRequest): Map<String, Any> {
        val role = req.role.trim().uppercase()
        if (role != "OWNER" && role != "CUSTOMER") {
            throw IllegalArgumentException("role must be OWNER or CUSTOMER")
        }
        val email = req.email.trim().lowercase()
        if (email.isBlank()) throw IllegalArgumentException("email required")
        if (req.password.isBlank()) throw IllegalArgumentException("password required")

        val user = User(
            email = email,
            passwordHash = passwordEncoder.encode(req.password),
            role = role
        )
        val saved = userRepository.save(user)
        return mapOf("id" to saved.id!!, "email" to saved.email, "role" to saved.role)
    }

    fun login(req: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(req.email.trim().lowercase())
            ?: throw IllegalArgumentException("invalid credentials")

        if (!passwordEncoder.matches(req.password, user.passwordHash)) {
            throw IllegalArgumentException("invalid credentials")
        }

        val access = jwtTokenService.issueAccessToken(userId = user.id!!, role = user.role, ttl = Duration.ofMinutes(15))
        val refresh = issueRefreshToken(userId = user.id!!)

        return TokenResponse(accessToken = access, refreshToken = refresh)
    }

    fun refresh(req: RefreshRequest): TokenResponse {
        val raw = req.refreshToken.trim()
        if (raw.isBlank()) throw IllegalArgumentException("refresh token required")

        val tokenHash = sha256Hex(raw)
        val stored = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw IllegalArgumentException("invalid refresh token")

        if (stored.revoked) throw IllegalArgumentException("refresh token revoked")
        if (stored.expiresAt.isBefore(Instant.now())) throw IllegalArgumentException("refresh token expired")

        val user = userRepository.findById(stored.userId).orElseThrow { IllegalArgumentException("user not found") }

        // rotate refresh token
        stored.revoked = true
        refreshTokenRepository.save(stored)

        val newRefresh = issueRefreshToken(userId = user.id!!)
        val access = jwtTokenService.issueAccessToken(userId = user.id!!, role = user.role, ttl = Duration.ofMinutes(15))

        return TokenResponse(accessToken = access, refreshToken = newRefresh)
    }

    fun logout(req: RefreshRequest) {
        val raw = req.refreshToken.trim()
        if (raw.isBlank()) return
        val tokenHash = sha256Hex(raw)
        val stored = refreshTokenRepository.findByTokenHash(tokenHash) ?: return
        stored.revoked = true
        refreshTokenRepository.save(stored)
    }

    private fun issueRefreshToken(userId: Long): String {
        val raw = generateSecureToken()
        val tokenHash = sha256Hex(raw)
        val expiresAt = Instant.now().plus(Duration.ofDays(30))

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                tokenHash = tokenHash,
                expiresAt = expiresAt,
                revoked = false
            )
        )
        return raw
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(48)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Hex(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(s.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
