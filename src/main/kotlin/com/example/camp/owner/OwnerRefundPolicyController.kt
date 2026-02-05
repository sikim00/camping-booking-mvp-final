package com.example.camp.owner

import com.example.camp.owner.service.OwnerCampService
import com.example.camp.security.SecurityUtils
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

data class CreateRefundPolicyRequest(
    @field:NotBlank val ruleJson: String
)

@RestController
@RequestMapping("/owner/camps/{campId}")
class OwnerRefundPolicyController(
    private val ownerCampService: OwnerCampService
) {
    @PostMapping("/refund-policies")
    fun createPolicy(@PathVariable campId: Long, @Validated @RequestBody req: CreateRefundPolicyRequest) =
        ownerCampService.createRefundPolicyVersion(
            ownerId = SecurityUtils.principal().userId,
            campId = campId,
            ruleJson = req.ruleJson
        )
}
