package com.example.casinoproject.api;

import com.example.casinoproject.dao.Promotion;
import com.example.casinoproject.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@Tag(name = "Promotions", description = "Публічний список діючих акцій")
public class PromotionApiController {

    private final PromotionService promotions;

    public PromotionApiController(PromotionService promotions) {
        this.promotions = promotions;
    }

    @GetMapping
    @Operation(summary = "Діючі акції", description = "Промокоди, які зараз увімкнені і не прострочені. Авторизація не потрібна.")
    @ApiResponse(responseCode = "200", description = "Список акцій")
    public List<Promotion> active() {
        return promotions.active();
    }
}
