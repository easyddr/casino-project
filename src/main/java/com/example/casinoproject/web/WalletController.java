package com.example.casinoproject.web;

import com.example.casinoproject.domain.BetStatus;
import com.example.casinoproject.domain.PaymentStatus;
import com.example.casinoproject.dto.DepositRequest;
import com.example.casinoproject.dto.PaymentResponse;
import com.example.casinoproject.dto.WithdrawRequest;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.security.CasinoUserPrincipal;
import com.example.casinoproject.service.PaymentService;
import com.example.casinoproject.service.PlayService;
import com.example.casinoproject.service.PromotionService;
import com.example.casinoproject.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WalletController {

    private final WalletService wallet;
    private final PaymentService payments;
    private final PromotionService promotions;
    private final PlayService play;

    public WalletController(WalletService wallet, PaymentService payments, PromotionService promotions, PlayService play) {
        this.wallet = wallet;
        this.payments = payments;
        this.promotions = promotions;
        this.play = play;
    }

    /**
     * Гаманець: форми поповнення і виведення, збережені картки, промокод, останні платежі і журнал транзакцій.
     */
    @GetMapping("/wallet")
    public String wallet(@AuthenticationPrincipal CasinoUserPrincipal me,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("cards", payments.savedCards(me.getId()));
        model.addAttribute("payments", payments.history(me.getId()));
        model.addAttribute("transactions", wallet.transactions(me.getId(), null, page, 15));
        return "wallet";
    }

    /**
     * Обробка форми поповнення. Результат (успіх чи відмова шлюзу) показується повідомленням після редіректу.
     */
    @PostMapping("/wallet/deposit")
    public String deposit(@AuthenticationPrincipal CasinoUserPrincipal me,
                          @Valid @ModelAttribute DepositRequest form, BindingResult binding, RedirectAttributes flash) {
        if (binding.hasErrors()) {
            flash.addFlashAttribute("error", "Перевірте дані форми поповнення");
            return "redirect:/wallet";
        }
        try {
            PaymentResponse r = payments.deposit(me.getId(), form);
            if (r.status() == PaymentStatus.SUCCESS) {
                flash.addFlashAttribute("success", "Баланс поповнено на " + r.amount());
            } else {
                flash.addFlashAttribute("error", "Платіж відхилено: " + r.errorMessage());
            }
        } catch (CasinoException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wallet";
    }

    @PostMapping("/wallet/withdraw")
    public String withdraw(@AuthenticationPrincipal CasinoUserPrincipal me,
                           @Valid @ModelAttribute WithdrawRequest form, BindingResult binding, RedirectAttributes flash) {
        if (binding.hasErrors()) {
            flash.addFlashAttribute("error", "Перевірте суму виведення");
            return "redirect:/wallet";
        }
        try {
            PaymentResponse r = payments.withdraw(me.getId(), form);
            flash.addFlashAttribute("success", "Виведено " + r.amount() + " на картку " + r.cardMask());
        } catch (CasinoException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wallet";
    }

    @PostMapping("/wallet/promo")
    public String promo(@AuthenticationPrincipal CasinoUserPrincipal me, @RequestParam String code,
                        RedirectAttributes flash) {
        try {
            promotions.activate(me.getId(), code);
            flash.addFlashAttribute("success", "Бонус нараховано!");
        } catch (CasinoException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/wallet";
    }

    /**
     * Історія ставок з фільтром по статусу і пагінацією.
     */
    @GetMapping("/history")
    public String history(@AuthenticationPrincipal CasinoUserPrincipal me,
                          @RequestParam(required = false) BetStatus status,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("bets", play.history(me.getId(), status, page, 20));
        model.addAttribute("status", status);
        model.addAttribute("statuses", BetStatus.values());
        return "history";
    }
}
