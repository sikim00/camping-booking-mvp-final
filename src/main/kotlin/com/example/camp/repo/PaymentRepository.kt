package com.example.camp.repo

import com.example.camp.domain.payment.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long>
