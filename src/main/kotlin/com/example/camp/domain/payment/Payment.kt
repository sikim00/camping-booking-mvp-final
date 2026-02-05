package com.example.camp.domain.payment

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_tx_id"])]
)
class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "booking_id", nullable = false)
    var bookingId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus,

    var provider: String? = null,

    @Column(name = "provider_tx_id")
    var providerTxId: String? = null,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    @Column(nullable = false)
    var currency: String = "KRW",

    @Column(name = "approved_at")
    var approvedAt: OffsetDateTime? = null
)
