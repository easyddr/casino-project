package com.example.casinoproject.web;

import com.example.casinoproject.dto.RegisterForm;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    /**
     * Сторінка логіну. Сам POST /login обробляє Spring Security, тут тільки показ форми.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    /**
     * Реєстрація. Якщо форма невалідна або логін зайнятий, показуємо ту саму сторінку з помилками,
     * при успіху перекидаємо на логін (redirect, щоб F5 не відправив форму вдруге).
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            return "register";
        }
        try {
            users.register(form);
        } catch (CasinoException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
