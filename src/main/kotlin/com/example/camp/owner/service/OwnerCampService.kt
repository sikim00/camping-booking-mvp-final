package com.example.camp.owner.service

import com.example.camp.domain.camp.Camp
import com.example.camp.domain.camp.Site
import com.example.camp.domain.policy.RefundPolicyVersion
import com.example.camp.repo.CampRepository
import com.example.camp.repo.RefundPolicyVersionRepository
import com.example.camp.repo.SiteRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class OwnerCampService(
    private val campRepository: CampRepository,
    private val siteRepository: SiteRepository,
    private val refundPolicyVersionRepository: RefundPolicyVersionRepository,
    private val objectMapper: ObjectMapper,
) {


    @Transactional
    fun createCamp(
        ownerId: Long,
        name: String,
        address: String? = null,
        phone: String? = null,
        description: String? = null
    ): Camp {
        val camp = Camp(
            ownerId = ownerId,
            name = name,
            address = address,
            phone = phone,
            description = description,
            isActive = true
        )
        return campRepository.save(camp)
    }

    @Transactional
    fun createSite(ownerId: Long, campId: Long, name: String, basePrice: BigDecimal, currency: String = "KRW"): Site {
        // ownerId reserved for authorization (not enforced in MVP)
        val site = Site(
            campId = campId,
            name = name,
            basePrice = basePrice,
            currency = currency,
            isActive = true,
        )
        return siteRepository.save(site)
    }

    @Transactional
    fun createRefundPolicyVersion(ownerId: Long, campId: Long, ruleJson: String): RefundPolicyVersion {
        // ownerId reserved for authorization (not enforced in MVP)

        // validate JSON early
        val ruleNode = try {
            objectMapper.readTree(ruleJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("ruleJson must be a valid JSON")
        }

        val current = refundPolicyVersionRepository.findFirstByCampIdAndIsActiveTrueOrderByVersionDesc(campId)
        if (current != null) {
            current.isActive = false
            refundPolicyVersionRepository.save(current)
        }

        val nextVersion = (refundPolicyVersionRepository.findMaxVersion(campId) ?: 0) + 1
        val pv = RefundPolicyVersion(
            campId = campId,
            version = nextVersion,
            isActive = true,
            ruleJson = ruleJson,
        )
        return refundPolicyVersionRepository.save(pv)
    }
}
