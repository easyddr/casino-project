package com.example.casinoproject.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Запис у каталозі ігор. Тип визначає, який движок рахує раунд,
 * а мінімальна і максимальна ставка перевіряються перед кожною грою.
 */
@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameType type;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "min_bet", nullable = false, precision = 14, scale = 2)
    private BigDecimal minBet;

    @Column(name = "max_bet", nullable = false, precision = 14, scale = 2)
    private BigDecimal maxBet;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
