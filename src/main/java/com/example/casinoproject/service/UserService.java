package com.example.casinoproject.service;

import com.example.casinoproject.domain.UserAccount;
import com.example.casinoproject.dto.RegisterForm;
import com.example.casinoproject.dto.UserView;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserAccountRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /**
     * Реєструє нового гравця з нульовим балансом. Пароль одразу хешується BCrypt, у базу відкритий пароль не потрапляє.
     * Якщо логін або email зайнятий, кидає 409.
     */
    @Transactional
    public UserView register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw CasinoException.badRequest("Паролі не збігаються");
        }
        if (users.existsByUsernameIgnoreCase(form.getUsername())) {
            throw CasinoException.conflict("Такий логін вже зайнятий");
        }
        if (users.existsByEmailIgnoreCase(form.getEmail())) {
            throw CasinoException.conflict("Такий email вже зареєстрований");
        }
        UserAccount user = new UserAccount();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPasswordHash(encoder.encode(form.getPassword()));
        return UserView.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public UserView get(Long id) {
        return users.findById(id).map(UserView::from)
                .orElseThrow(() -> CasinoException.notFound("Користувача не знайдено"));
    }

    @Transactional(readOnly = true)
    public List<UserView> all() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(UserView::from).toList();
    }

    /**
     * Блокує або розблоковує гравця. Заблокований не зможе залогінитись і робити ставки.
     * Адміністратора заблокувати не можна, щоб випадково не лишитись без доступу до адмінки.
     */
    @Transactional
    public UserView setBlocked(Long id, boolean blocked) {
        UserAccount user = users.findById(id).orElseThrow(() -> CasinoException.notFound("Користувача не знайдено"));
        if (user.getRole() == com.example.casinoproject.domain.Role.ADMIN && blocked) {
            throw CasinoException.conflict("Адміністратора блокувати не можна");
        }
        user.setBlocked(blocked);
        return UserView.from(user);
    }
}
