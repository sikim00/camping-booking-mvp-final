package com.example.camp.owner

import com.example.camp.owner.service.OwnerCampService
import com.example.camp.security.SecurityUtils
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

data class CreateCampRequest(
    @field:NotBlank val name: String,
    val address: String? = null,
    val phone: String? = null,
    val description: String? = null
)

@RestController
@RequestMapping("/owner/camps")
class OwnerCampController(
    private val ownerCampService: OwnerCampService
) {
    @PostMapping
    fun createCamp(
        @Validated @RequestBody req: CreateCampRequest
    ) : Any {
        val p = SecurityUtils.principal()
        if (p.role.uppercase() != "OWNER") {
            throw AccessDeniedException("Only OWNER can create camps.")
        }
        return ownerCampService.createCamp(
            ownerId = p.userId,
            name = req.name,
            address = req.address,
            phone = req.phone,
            description = req.description
        )
    }
}
