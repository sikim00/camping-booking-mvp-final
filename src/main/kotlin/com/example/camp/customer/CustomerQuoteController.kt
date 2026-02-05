package com.example.camp.customer

import com.example.camp.application.QuoteService
import com.example.camp.customer.dto.QuoteRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/customer")
class CustomerQuoteController(
    private val quoteService: QuoteService
) {
    @PostMapping("/quotes")
    fun quote(@Validated @RequestBody req: QuoteRequest) =
        quoteService.quote(req.siteId, req.checkInDate, req.checkOutDate)
}
