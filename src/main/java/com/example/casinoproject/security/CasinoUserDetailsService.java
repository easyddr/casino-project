package com.example.casinoproject.security;

import com.example.casinoproject.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security викликає це при логіні (і через форму, і через HTTP Basic у Postman).
 * Заблокований користувач отримає помилку "акаунт заблоковано" ще до перевірки пароля.
 */
@Service
public class CasinoUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public CasinoUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsername(username)
                .map(CasinoUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Користувача не знайдено"));
    }
}
