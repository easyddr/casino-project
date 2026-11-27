package com.example.casinoproject.web;

import com.example.casinoproject.domain.GameType;
import com.example.casinoproject.dto.GameRequest;
import com.example.casinoproject.dto.PromotionRequest;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.service.AdminService;
import com.example.casinoproject.service.GameCatalogService;
import com.example.casinoproject.service.PromotionService;
import com.example.casinoproject.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Мінімальна адмінка на сторінках. Все те саме і більше доступно через /api/admin та /api/games.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService admin;
    private final UserService users;
    private final GameCatalogService catalog;
    private final PromotionService promotions;

    public AdminController(AdminService admin, UserService users, GameCatalogService catalog, PromotionService promotions) {
        this.admin = admin;
        this.users = users;
        this.catalog = catalog;
        this.promotions = promotions;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", admin.stats());
        model.addAttribute("maxWin", admin.currentMaxWin());
        model.addAttribute("users", users.all());
        model.addAttribute("games", catalog.allGames());
        model.addAttribute("promotions", promotions.all());
        model.addAttribute("gameTypes", GameType.values());
        return "admin/dashboard";
    }

    @PostMapping("/games")
    public String createGame(@RequestParam String name, @RequestParam GameType type,
                             @RequestParam(required = false) String description,
                             @RequestParam BigDecimal minBet, @RequestParam BigDecimal maxBet,
                             RedirectAttributes flash) {
        return run(flash, "Гру створено",
                () -> catalog.create(new GameRequest(name, type, description, minBet, maxBet, true)));
    }

    @PostMapping("/games/{id}/toggle")
    public String toggleGame(@PathVariable Long id, RedirectAttributes flash) {
        return run(flash, "Статус гри змінено", () -> catalog.toggleActive(id));
    }

    @PostMapping("/users/{id}/block")
    public String block(@PathVariable Long id, @RequestParam boolean blocked, RedirectAttributes flash) {
        return run(flash, blocked ? "Користувача заблоковано" : "Користувача розблоковано",
                () -> users.setBlocked(id, blocked));
    }

    /**
     * Створення промокоду з форми. Термін дії задається в днях від сьогодні, порожнє поле означає безстроковий код.
     */
    @PostMapping("/promotions")
    public String createPromotion(@RequestParam String code, @RequestParam String title,
                                  @RequestParam BigDecimal bonusAmount, @RequestParam(required = false) Integer days,
                                  RedirectAttributes flash) {
        PromotionRequest r = new PromotionRequest();
        r.setCode(code);
        r.setTitle(title);
        r.setBonusAmount(bonusAmount);
        r.setValidUntil(days == null ? null : OffsetDateTime.now().plusDays(days));
        return run(flash, "Промокод створено", () -> promotions.create(r));
    }

    @PostMapping("/promotions/{id}/delete")
    public String deletePromotion(@PathVariable long id, RedirectAttributes flash) {
        return run(flash, "Промокод видалено", () -> promotions.delete(id));
    }

    @PostMapping("/house")
    public String adjustBank(@RequestParam BigDecimal delta, RedirectAttributes flash) {
        return run(flash, "Банк казино оновлено", () -> admin.adjustBank(delta));
    }

    private String run(RedirectAttributes flash, String successMessage, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("success", successMessage);
        } catch (CasinoException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}
