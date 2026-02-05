package com.example.camp.repo

import com.example.camp.domain.camp.Camp
import org.springframework.data.jpa.repository.JpaRepository

interface CampRepository : JpaRepository<Camp, Long>
