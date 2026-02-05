package com.example.camp.security

import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    fun principal(): UserPrincipal =
        SecurityContextHolder.getContext().authentication.principal as UserPrincipal
}
