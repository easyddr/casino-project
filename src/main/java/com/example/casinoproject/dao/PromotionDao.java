package com.example.casinoproject.dao;

import java.util.List;
import java.util.Optional;

/**
 * Промокоди і їх активації. Реалізація на JdbcClient.
 */
public interface PromotionDao {

    long create(Promotion promotion);

    Optional<Promotion> findById(long id);

    int update(Promotion promotion);

    int delete(long id);

    List<Promotion> findAll();

    List<Promotion> findActive();

    Optional<Promotion> findByCode(String code);

    boolean isActivatedBy(long userId, long promotionId);

    void markActivated(long userId, long promotionId);

    long countActivations(long promotionId);
}
