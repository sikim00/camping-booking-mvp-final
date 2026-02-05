package com.example.camp.domain.camp

import jakarta.persistence.*

@Entity
@Table(name = "camps")
class Camp(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "owner_id", nullable = false)
    var ownerId: Long,

    @Column(nullable = false)
    var name: String,

    var address: String? = null,
    var phone: String? = null,
    var description: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)
