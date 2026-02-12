package com.example.camp.repo

import com.example.camp.domain.auth.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun deleteAllByUserId(userId: Long)
}
