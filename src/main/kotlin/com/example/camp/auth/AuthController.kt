package com.example.camp.auth

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

data class RegisterRequestDto(
    @field:Email val email: String,
    @field:NotBlank val password: String,
    @field:NotBlank val role: String
)

data class LoginRequestDto(
    @field:Email val email: String,
    @field:NotBlank val password: String
)

data class RefreshRequestDto(
    @field:NotBlank val refreshToken: String
)

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/register")
    fun register(@RequestBody @Valid req: RegisterRequestDto): Map<String, Any> =
        authService.register(RegisterRequest(req.email, req.password, req.role))

    @PostMapping("/login")
    fun login(@RequestBody @Valid req: LoginRequestDto): TokenResponse =
        authService.login(LoginRequest(req.email, req.password))

    @PostMapping("/refresh")
    fun refresh(@RequestBody @Valid req: RefreshRequestDto): TokenResponse =
        authService.refresh(RefreshRequest(req.refreshToken))

    @PostMapping("/logout")
    fun logout(@RequestBody @Valid req: RefreshRequestDto): Map<String, String> {
        authService.logout(RefreshRequest(req.refreshToken))
        return mapOf("status" to "ok")
    }
}
