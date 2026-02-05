package com.example.camp.domain.camp

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "sites",
    uniqueConstraints = [UniqueConstraint(columnNames = ["camp_id", "name"])]
)
class Site(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "camp_id", nullable = false)
    var campId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    var basePrice: BigDecimal,

    @Column(nullable = false)
    var currency: String = "KRW",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)
