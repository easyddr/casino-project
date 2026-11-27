package com.example.casinoproject.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class JdbcTemplateHouseBankDao implements HouseBankDao {

    private final JdbcTemplate jdbc;

    public JdbcTemplateHouseBankDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BigDecimal getBalance() {
        return jdbc.queryForObject("SELECT balance FROM house_bank WHERE id = 1", BigDecimal.class);
    }

    /**
     * Читає баланс казино і блокує рядок до кінця транзакції. Викликається на початку кожного раунду,
     * тому ліміт виграшу рахується від актуальної суми, а не від застарілої.
     */
    @Override
    public BigDecimal lockBalance() {
        return jdbc.queryForObject("SELECT balance FROM house_bank WHERE id = 1 FOR UPDATE", BigDecimal.class);
    }

    /**
     * Додає delta до банку (від'ємне значення списує). Якщо банк піде в мінус, база кине помилку
     * через CHECK, і вся транзакція відкотиться.
     */
    @Override
    public void add(BigDecimal delta) {
        jdbc.update("UPDATE house_bank SET balance = balance + ?, updated_at = now() WHERE id = 1", delta);
    }
}
