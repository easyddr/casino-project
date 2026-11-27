package com.example.casinoproject.config;

import com.example.casinoproject.domain.Role;
import com.example.casinoproject.domain.UserAccount;
import com.example.casinoproject.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * При старті створює адміністратора, якщо його ще нема. Логін і пароль беруться з casino.admin.*,
 * тому хеш пароля не треба зашивати в SQL-міграцію.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    @Value("${casino.admin.username}")
    private String adminUsername;
    @Value("${casino.admin.email}")
    private String adminEmail;
    @Value("${casino.admin.password}")
    private String adminPassword;

    public DataInitializer(UserAccountRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.existsByUsernameIgnoreCase(adminUsername)) {
            return;
        }
        UserAccount admin = new UserAccount();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        users.save(admin);
        log.info("Створено адміністратора '{}'", adminUsername);
    }
}
