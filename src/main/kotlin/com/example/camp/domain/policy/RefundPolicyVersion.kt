package com.example.camp.domain.policy

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "refund_policy_versions",
    uniqueConstraints = [UniqueConstraint(name = "UK_REFUND_POLICY_CAMP_VERSION", columnNames = ["camp_id", "version"])],
)
class RefundPolicyVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "camp_id", nullable = false)
    val campId: Long,

    @Column(name = "version", nullable = false)
    val version: Int,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    /**
     * Stored as Postgres jsonb.
     * Keep the Kotlin type as String to avoid Hibernate/Jackson JSON mapper variance across versions.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_json", columnDefinition = "jsonb", nullable = false)
    val ruleJson: String,
)
