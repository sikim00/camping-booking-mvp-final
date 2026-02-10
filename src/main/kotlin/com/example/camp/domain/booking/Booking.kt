package com.example.camp.domain.booking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "bookings")
data class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "booking_code", nullable = false, unique = true)
    val bookingCode: String,

    @Column(name = "customer_id", nullable = false)
    val customerId: Long,

    @Column(name = "camp_id", nullable = false)
    val campId: Long,

    @Column(name = "site_id", nullable = false)
    val siteId: Long,

    @Column(name = "head_count", nullable = false)
    val headCount: Int = 1,

    @Column(name = "check_in_date", nullable = false)
    val checkInDate: LocalDate,

    @Column(name = "check_out_date", nullable = false)
    val checkOutDate: LocalDate,

    @Column(name = "nights_count", nullable = false)
    val nightsCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: BookingStatus = BookingStatus.PENDING_PAYMENT,

    // 스냅샷 금액
    @Column(name = "amount_snapshot_subtotal", nullable = false, precision = 12, scale = 2)
    val subtotal: BigDecimal,

    @Column(name = "amount_snapshot_discount", nullable = false, precision = 12, scale = 2)
    val discount: BigDecimal,

    @Column(name = "amount_snapshot_total", nullable = false, precision = 12, scale = 2)
    val total: BigDecimal,

    @Column(name = "currency", nullable = false)
    val currency: String,

    // 적용된 환불정책 버전(스냅샷)
    @Column(name = "refund_policy_version_id")
    val refundPolicyVersionId: Long? = null,

    // Postgres jsonb 스냅샷
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "refund_rule_snapshot_json", columnDefinition = "jsonb")
    val refundRuleSnapshotJson: String? = null,
)
