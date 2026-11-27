package com.example.casinoproject.repository;

import com.example.casinoproject.domain.Bet;
import com.example.casinoproject.domain.BetStatus;
import com.example.casinoproject.domain.GameType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface BetRepository extends CrudRepository<Bet, Long>, PagingAndSortingRepository<Bet, Long> {

    /**
     * Історія ставок гравця з пагінацією. EntityGraph одразу підтягує гру,
     * щоб на сторінці історії не було N+1 запитів за назвою гри.
     */
    @EntityGraph(attributePaths = "game")
    Page<Bet> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "game")
    Page<Bet> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BetStatus status, Pageable pageable);

    /**
     * Найбільші виграші для головної сторінки. Сам запит описаний через @NamedQuery у класі Bet,
     * Spring Data знаходить його по імені Bet.findBiggestWins. Кількість задається через Pageable.
     */
    List<Bet> findBiggestWins(@Param("status") BetStatus status, Pageable pageable);

    boolean existsByGameId(Long gameId);

    /**
     * Знаходить ставки, які зависли в статусі PLACED (наприклад після перезапуску сервера посеред крипто-раунду).
     */
    List<Bet> findByStatusAndGameTypeAndCreatedAtBefore(BetStatus status, GameType type, OffsetDateTime before);

    /**
     * Статистика по іграх для адмінки: скільки раундів, скільки поставили і скільки виплатили.
     */
    @Query("select g.name as gameName, count(b) as rounds, coalesce(sum(b.amount), 0) as wagered, " +
            "coalesce(sum(b.payout), 0) as paidOut " +
            "from Bet b join b.game g where b.status in :statuses group by g.name order by g.name")
    List<GameStats> gameStats(@Param("statuses") List<BetStatus> statuses);

    interface GameStats {
        String getGameName();
        Long getRounds();
        java.math.BigDecimal getWagered();
        java.math.BigDecimal getPaidOut();
    }
}
