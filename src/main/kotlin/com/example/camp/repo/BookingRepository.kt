package com.example.camp.repo

import com.example.camp.domain.booking.Booking
import org.springframework.data.jpa.repository.JpaRepository

interface BookingRepository : JpaRepository<Booking, Long> {
    fun findAllByCustomerIdOrderByIdDesc(customerId: Long): List<Booking>
}
