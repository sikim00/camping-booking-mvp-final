package com.example.camp.domain.auth

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")],
    uniqueConstraints = [UniqueConstraint(name = "uq_refresh_tokens_token_hash", columnNames = ["token_hash"])]
)
class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
