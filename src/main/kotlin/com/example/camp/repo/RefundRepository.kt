package com.example.camp.repo

import com.example.camp.domain.refund.Refund
import org.springframework.data.jpa.repository.JpaRepository

interface RefundRepository : JpaRepository<Refund, Long> {
    fun findByIdempotencyKey(idempotencyKey: String): Refund?
}
