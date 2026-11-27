package com.example.casinoproject.web;

import com.example.casinoproject.service.GameCatalogService;
import com.example.casinoproject.service.PlayService;
import com.example.casinoproject.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final GameCatalogService catalog;
    private final PlayService play;
    private final PromotionService promotions;

    public HomeController(GameCatalogService catalog, PlayService play, PromotionService promotions) {
        this.catalog = catalog;
        this.play = play;
        this.promotions = promotions;
    }

    /**
     * Головна: список активних ігор, топ виграшів і діючі акції.
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("games", catalog.activeGames());
        model.addAttribute("wins", play.biggestWins(5));
        model.addAttribute("promotions", promotions.active());
        return "index";
    }
}
