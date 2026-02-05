package com.example.camp.domain.booking

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "booking_nights",
    uniqueConstraints = [UniqueConstraint(columnNames = ["site_id", "night_date"])]
)
class BookingNight(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "booking_id", nullable = false)
    var bookingId: Long,

    @Column(name = "site_id", nullable = false)
    var siteId: Long,

    @Column(name = "night_date", nullable = false)
    var nightDate: LocalDate
)
