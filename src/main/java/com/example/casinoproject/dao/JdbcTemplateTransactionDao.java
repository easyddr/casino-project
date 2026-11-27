package com.example.casinoproject.dao;

import com.example.casinoproject.domain.TransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTemplateTransactionDao implements TransactionDao {

    private static final RowMapper<TransactionRecord> MAPPER = (rs, n) -> new TransactionRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getObject("bet_id", Long.class),
            TransactionType.valueOf(rs.getString("type")),
            rs.getBigDecimal("amount"),
            rs.getBigDecimal("balance_after"),
            rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public JdbcTemplateTransactionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Записує новий рух грошей і повертає id, який згенерувала база (через KeyHolder).
     * Сума завжди додатна, напрямок зрозумілий з типу: BET і WITHDRAWAL списують, решта нараховує.
     */
    @Override
    public long create(TransactionRecord r) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO transactions (user_id, bet_id, type, amount, balance_after) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setLong(1, r.userId());
            if (r.betId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, r.betId());
            }
            ps.setString(3, r.type().name());
            ps.setBigDecimal(4, r.amount());
            ps.setBigDecimal(5, r.balanceAfter());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    @Override
    public Optional<TransactionRecord> findById(long id) {
        List<TransactionRecord> list = jdbc.query("SELECT * FROM transactions WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    /**
     * Оновлює тип, суму і баланс після операції. У звичайній роботі казино журнал не правиться,
     * метод є для адміністративних виправлень і для повного CRUD.
     */
    @Override
    public int update(TransactionRecord r) {
        return jdbc.update("UPDATE transactions SET type = ?, amount = ?, balance_after = ? WHERE id = ?",
                r.type().name(), r.amount(), r.balanceAfter(), r.id());
    }

    @Override
    public int delete(long id) {
        return jdbc.update("DELETE FROM transactions WHERE id = ?", id);
    }

    /**
     * Сторінка транзакцій гравця від новіших до старіших. Якщо type = null, повертаються всі типи.
     */
    @Override
    public List<TransactionRecord> findByUser(long userId, TransactionType type, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE user_id = ?");
        args.add(userId);
        if (type != null) {
            sql.append(" AND type = ?");
            args.add(type.name());
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    @Override
    public long countByUser(long userId, TransactionType type) {
        Long count = type == null
                ? jdbc.queryForObject("SELECT count(*) FROM transactions WHERE user_id = ?", Long.class, userId)
                : jdbc.queryForObject("SELECT count(*) FROM transactions WHERE user_id = ? AND type = ?",
                Long.class, userId, type.name());
        return count == null ? 0 : count;
    }

    @Override
    public BigDecimal sumByType(TransactionType type) {
        return jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM transactions WHERE type = ?",
                BigDecimal.class, type.name());
    }
}
