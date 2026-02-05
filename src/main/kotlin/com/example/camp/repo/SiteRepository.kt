package com.example.camp.repo

import com.example.camp.domain.camp.Site
import org.springframework.data.jpa.repository.JpaRepository

interface SiteRepository : JpaRepository<Site, Long> {
    fun findAllByCampIdAndIsActiveTrue(campId: Long): List<Site>
}
