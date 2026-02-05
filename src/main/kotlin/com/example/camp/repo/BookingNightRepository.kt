package com.example.camp.repo

import com.example.camp.domain.booking.BookingNight
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface BookingNightRepository : JpaRepository<BookingNight, Long> {

    @Query(
        "select bn.nightDate from BookingNight bn " +
        "where bn.siteId in :siteIds and bn.nightDate >= :from and bn.nightDate < :to"
    )
    fun findBookedDates(siteIds: List<Long>, from: LocalDate, to: LocalDate): List<LocalDate>

    @Modifying
    @Query("delete from BookingNight bn where bn.bookingId = :bookingId")
    fun deleteByBookingId(@Param("bookingId") bookingId: Long)
}
