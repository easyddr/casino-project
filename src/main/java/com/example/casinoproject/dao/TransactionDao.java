package com.example.casinoproject.dao;

import com.example.casinoproject.domain.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Журнал руху грошей. Реалізація на JdbcTemplate.
 */
public interface TransactionDao {

    long create(TransactionRecord record);

    Optional<TransactionRecord> findById(long id);

    int update(TransactionRecord record);

    int delete(long id);

    List<TransactionRecord> findByUser(long userId, TransactionType type, int limit, int offset);

    long countByUser(long userId, TransactionType type);

    BigDecimal sumByType(TransactionType type);
}
