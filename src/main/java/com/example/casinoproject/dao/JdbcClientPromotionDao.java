package com.example.casinoproject.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcClientPromotionDao implements PromotionDao {

    private static final RowMapper<Promotion> MAPPER = (rs, n) -> new Promotion(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("title"),
            rs.getBigDecimal("bonus_amount"),
            rs.getObject("valid_until", OffsetDateTime.class),
            rs.getBoolean("is_active"));

    private final JdbcClient jdbc;

    public JdbcClientPromotionDao(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Створює промокод і повертає згенерований id. Код зберігається у верхньому регістрі,
     * щоб гравець міг вводити його як завгодно.
     */
    @Override
    public long create(Promotion p) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO promotions (code, title, bonus_amount, valid_until, is_active)
                        VALUES (:code, :title, :bonus, :validUntil, :active)""")
                .param("code", p.code().trim().toUpperCase())
                .param("title", p.title())
                .param("bonus", p.bonusAmount())
                .param("validUntil", p.validUntil())
                .param("active", p.active())
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    @Override
    public Optional<Promotion> findById(long id) {
        return jdbc.sql("SELECT * FROM promotions WHERE id = :id")
                .param("id", id)
                .query(MAPPER)
                .optional();
    }

    @Override
    public int update(Promotion p) {
        return jdbc.sql("""
                        UPDATE promotions SET code = :code, title = :title, bonus_amount = :bonus,
                               valid_until = :validUntil, is_active = :active
                        WHERE id = :id""")
                .param("code", p.code().trim().toUpperCase())
                .param("title", p.title())
                .param("bonus", p.bonusAmount())
                .param("validUntil", p.validUntil())
                .param("active", p.active())
                .param("id", p.id())
                .update();
    }

    @Override
    public int delete(long id) {
        return jdbc.sql("DELETE FROM promotions WHERE id = :id").param("id", id).update();
    }

    @Override
    public List<Promotion> findAll() {
        return jdbc.sql("SELECT * FROM promotions ORDER BY id").query(MAPPER).list();
    }

    /**
     * Тільки ті акції, якими зараз можна скористатись (увімкнені і не прострочені).
     */
    @Override
    public List<Promotion> findActive() {
        return jdbc.sql("""
                        SELECT * FROM promotions
                        WHERE is_active = TRUE AND (valid_until IS NULL OR valid_until > now())
                        ORDER BY id""")
                .query(MAPPER)
                .list();
    }

    @Override
    public Optional<Promotion> findByCode(String code) {
        return jdbc.sql("SELECT * FROM promotions WHERE code = :code")
                .param("code", code.trim().toUpperCase())
                .query(MAPPER)
                .optional();
    }

    @Override
    public boolean isActivatedBy(long userId, long promotionId) {
        Long count = jdbc.sql("SELECT count(*) FROM user_promotions WHERE user_id = :u AND promotion_id = :p")
                .param("u", userId)
                .param("p", promotionId)
                .query(Long.class)
                .single();
        return count > 0;
    }

    @Override
    public void markActivated(long userId, long promotionId) {
        jdbc.sql("INSERT INTO user_promotions (user_id, promotion_id) VALUES (:u, :p)")
                .param("u", userId)
                .param("p", promotionId)
                .update();
    }

    @Override
    public long countActivations(long promotionId) {
        return jdbc.sql("SELECT count(*) FROM user_promotions WHERE promotion_id = :p")
                .param("p", promotionId)
                .query(Long.class)
                .single();
    }
}
