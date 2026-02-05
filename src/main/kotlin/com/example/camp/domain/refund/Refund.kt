package com.example.camp.domain.refund

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "refunds",
    uniqueConstraints = [UniqueConstraint(columnNames = ["idempotency_key"])]
)
class Refund(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "booking_id", nullable = false)
    var bookingId: Long,

    @Column(name = "idempotency_key", nullable = false)
    var idempotencyKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RefundStatus,

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    var requestedAmount: BigDecimal,

    @Column(name = "approved_amount", precision = 12, scale = 2)
    var approvedAmount: BigDecimal? = null,

    @Column(nullable = false)
    var currency: String = "KRW",

    var reason: String? = null,

    @Column(name = "provider_refund_id")
    var providerRefundId: String? = null
)
