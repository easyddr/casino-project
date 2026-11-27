package com.example.casinoproject.repository;

import com.example.casinoproject.domain.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends CrudRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findAllByOrderByCreatedAtDesc();

    /**
     * Завантажує користувача з блокуванням рядка (SELECT ... FOR UPDATE).
     * Викликати тільки всередині транзакції перед зміною балансу, тоді дві паралельні ставки
     * не зможуть одночасно списати ті самі гроші.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> lockById(@Param("id") Long id);

    /**
     * Сума всіх балансів гравців, тобто скільки казино винне людям. Потрібно для адмінки.
     */
    @Query("select coalesce(sum(u.balance), 0) from UserAccount u where u.role = com.example.casinoproject.domain.Role.PLAYER")
    BigDecimal totalPlayerBalance();
}
