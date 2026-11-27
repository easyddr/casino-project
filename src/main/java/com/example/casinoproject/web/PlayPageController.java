package com.example.casinoproject.web;

import com.example.casinoproject.dto.GameResponse;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.service.GameCatalogService;
import com.example.casinoproject.service.PlayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlayPageController {

    private final GameCatalogService catalog;
    private final PlayService play;

    public PlayPageController(GameCatalogService catalog, PlayService play) {
        this.catalog = catalog;
        this.play = play;
    }

    /**
     * Сторінка гри. Шаблон обирається за типом гри, сама гра йде через JS і /api/play.
     * Для типів без движка (рулетка, блекджек) показуємо повідомлення на головній.
     */
    @GetMapping("/play/{id}")
    public String play(@PathVariable Long id, Model model, RedirectAttributes flash) {
        GameResponse game;
        try {
            game = catalog.get(id);
        } catch (CasinoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
        if (!game.active()) {
            flash.addFlashAttribute("error", "Гра зараз вимкнена");
            return "redirect:/";
        }
        model.addAttribute("game", game);
        return switch (game.type()) {
            case SLOTS -> {
                model.addAttribute("paytable", play.slotsPaytable());
                yield "play/slots";
            }
            case DICE -> "play/dice";
            case CRYPTO -> "play/crypto";
            default -> {
                flash.addFlashAttribute("error", "Ця гра ще в розробці");
                yield "redirect:/";
            }
        };
    }
}
