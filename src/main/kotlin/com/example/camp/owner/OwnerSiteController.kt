package com.example.camp.owner

import com.example.camp.owner.service.OwnerCampService
import com.example.camp.security.SecurityUtils
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

data class CreateSiteRequest(
    @field:NotBlank val name: String,
    @field:NotNull val basePrice: BigDecimal
)

@RestController
@RequestMapping("/owner/camps/{campId}")
class OwnerSiteController(
    private val ownerCampService: OwnerCampService
) {
    @PostMapping("/sites")
    fun createSite(
        @PathVariable campId: Long,
        @Validated @RequestBody req: CreateSiteRequest
    ) = ownerCampService.createSite(
        ownerId = SecurityUtils.principal().userId,
        campId = campId,
        name = req.name,
        basePrice = req.basePrice
    )
}
