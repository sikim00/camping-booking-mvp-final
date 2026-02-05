package com.example.camp.repo

import com.example.camp.domain.policy.RefundPolicyVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RefundPolicyVersionRepository : JpaRepository<RefundPolicyVersion, Long> {

    // CustomerBookingService / OwnerCampService가 요구하는 시그니처 그대로 제공
    fun findFirstByCampIdAndIsActiveTrueOrderByVersionDesc(campId: Long): RefundPolicyVersion?

    @Query("select coalesce(max(r.version), 0) from RefundPolicyVersion r where r.campId = :campId")
    fun findMaxVersion(@Param("campId") campId: Long): Int
}
