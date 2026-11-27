package com.example.casinoproject.web;

import com.example.casinoproject.security.CasinoUserPrincipal;
import com.example.casinoproject.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal;

/**
 * Додає в модель кожної сторінки поточного користувача (me) і його баланс, щоб шапка сайту завжди їх показувала.
 * Для гостя обидва значення null, у шаблонах це перевіряється через th:if="${me}".
 */
@ControllerAdvice(basePackages = "com.example.casinoproject.web")
public class GlobalModelAdvice {

    @Autowired
    private WalletService wallet;

    @ModelAttribute("me")
    public CasinoUserPrincipal me(@AuthenticationPrincipal CasinoUserPrincipal me) {
        return me;
    }

    @ModelAttribute("balance")
    public BigDecimal balance(@AuthenticationPrincipal CasinoUserPrincipal me) {
        return me == null ? null : wallet.balanceOf(me.getId());
    }
}
