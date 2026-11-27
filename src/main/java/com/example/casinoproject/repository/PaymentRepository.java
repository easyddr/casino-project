package com.example.casinoproject.repository;

import com.example.casinoproject.domain.Payment;
import com.example.casinoproject.domain.PaymentStatus;
import com.example.casinoproject.domain.PaymentType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends CrudRepository<Payment, Long> {

    List<Payment> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Payment> findByIdAndUserIdAndStatus(Long id, Long userId, PaymentStatus status);

    /**
     * Успішні поповнення гравця, з них беремо "збережені картки". Дублікати по масці прибирає сервіс.
     */
    @Query("select p from Payment p where p.user.id = :userId and p.status = :status and p.type = :type " +
            "order by p.createdAt desc")
    List<Payment> findByUserStatusAndType(@Param("userId") Long userId,
                                          @Param("status") PaymentStatus status,
                                          @Param("type") PaymentType type);
}
