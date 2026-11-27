package com.example.casinoproject.service;

import com.example.casinoproject.domain.BetStatus;
import com.example.casinoproject.domain.GameType;
import com.example.casinoproject.dto.CryptoStateResponse;
import com.example.casinoproject.dto.RoundResult;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.game.PricePathGenerator;
import com.example.casinoproject.repository.BetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Гра "Crypto Trader". Гравець купує монету на суму ставки, ціна змінюється кожні 300 мс, і треба вчасно продати.
 * Весь графік генерується на сервері наперед, а клієнт бачить тільки ціни до поточного моменту,
 * тому підглянути майбутнє неможливо. Якщо не встигнути продати до обвалу, ставка згорає.
 * Під час продажу береться ціна на момент, коли запит дійшов до сервера, плюс комісія біржі 3%.
 */
@Service
public class CryptoTradingService {

    private static final Logger log = LoggerFactory.getLogger(CryptoTradingService.class);

    public static final long TICK_MS = 300;
    public static final int MAX_TICKS = 400;

    private final PlayService play;
    private final WalletService wallet;
    private final BetRepository bets;
    private final ObjectProvider<PricePathGenerator> generators;
    private final Map<Long, Round> rounds = new ConcurrentHashMap<>();
    private final Set<Long> busyUsers = ConcurrentHashMap.newKeySet();

    public CryptoTradingService(PlayService play, WalletService wallet, BetRepository bets,
                                ObjectProvider<PricePathGenerator> generators) {
        this.play = play;
        this.wallet = wallet;
        this.bets = bets;
        this.generators = generators;
    }

    private record Round(Long betId, Long userId, BigDecimal amount, double[] prices, int crashTick, long startedAt) {

        int tickAt(long now) {
            return (int) Math.min((now - startedAt) / TICK_MS, MAX_TICKS);
        }

        boolean crashedAt(int tick) {
            return crashTick > 0 && tick >= crashTick;
        }

        boolean endedAt(int tick) {
            return crashTick < 0 && tick >= prices.length - 1;
        }
    }

    /**
     * Купує монету: списує ставку, генерує графік і запускає таймер. Одночасно в гравця може бути тільки один раунд.
     * Повертає перший стан, після цього фронт опитує state кожні кілька сотень мілісекунд.
     */
    public CryptoStateResponse start(Long userId, Long gameId, BigDecimal amount) {
        if (!busyUsers.add(userId)) {
            throw CasinoException.conflict("У вас вже є відкрита позиція, спочатку продайте її");
        }
        PlayService.OpenedBet opened;
        try {
            opened = play.openBet(userId, gameId, GameType.CRYPTO, amount);
        } catch (RuntimeException e) {
            busyUsers.remove(userId);
            throw e;
        }
        PricePathGenerator.Path path = generators.getObject()
                .generate(opened.amount(), opened.maxWin(), MAX_TICKS);
        Round round = new Round(opened.betId(), userId, opened.amount(), path.prices(), path.crashTick(),
                System.currentTimeMillis());
        rounds.put(round.betId(), round);
        return new CryptoStateResponse(round.betId(), round.amount(), 0, 0, List.of(1.0), 1.0,
                value(round.amount(), 1.0), false, false, BetStatus.PLACED, BigDecimal.ZERO, opened.balance());
    }

    /**
     * Поточний стан раунду. Параметр from каже, з якого тіку віддати ціни (фронт передає, скільки точок вже має).
     * Якщо за цей час монета обвалилась, раунд тут же розраховується як програш.
     */
    public CryptoStateResponse state(Long userId, Long betId, int from) {
        Round round = rounds.get(betId);
        if (round == null) {
            return finishedState(userId, betId);
        }
        requireOwner(round, userId);
        int tick = round.tickAt(System.currentTimeMillis());
        if (round.crashedAt(tick) || round.endedAt(tick)) {
            finish(round, tick);
            return finishedState(userId, betId);
        }
        return liveState(round, tick, from);
    }

    /**
     * Продає монету за поточною ціною. Якщо обвал стався раніше, ніж запит дійшов до сервера, гравець нічого не отримує.
     */
    public CryptoStateResponse sell(Long userId, Long betId) {
        Round round = rounds.get(betId);
        if (round == null) {
            return finishedState(userId, betId);
        }
        requireOwner(round, userId);
        finish(round, round.tickAt(System.currentTimeMillis()));
        return finishedState(userId, betId);
    }

    /**
     * Раз на 5 секунд закриває раунди, які вже обвалились або дійшли до кінця, навіть якщо гравець закрив вкладку.
     * Також повертає гроші за ставки, що зависли після перезапуску сервера (графік таких раундів втрачено).
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        for (Round round : List.copyOf(rounds.values())) {
            int tick = round.tickAt(now);
            if (round.crashedAt(tick) || round.endedAt(tick)) {
                finish(round, tick);
            }
        }
        OffsetDateTime threshold = OffsetDateTime.now().minusSeconds(MAX_TICKS * TICK_MS / 1000 + 60);
        bets.findByStatusAndGameTypeAndCreatedAtBefore(BetStatus.PLACED, GameType.CRYPTO, threshold).stream()
                .filter(b -> !rounds.containsKey(b.getId()))
                .forEach(b -> {
                    log.warn("Крипто-ставка {} зависла, повертаю гроші", b.getId());
                    play.cancel(b.getId(), "server_restart");
                });
    }

    private void finish(Round round, int tick) {
        if (!rounds.remove(round.betId(), round)) {
            return;
        }
        busyUsers.remove(round.userId());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("soldAtTick", tick);
        BigDecimal payout;
        if (round.crashedAt(tick)) {
            payout = BigDecimal.ZERO;
            details.put("crashed", true);
            details.put("crashTick", round.crashTick());
        } else {
            int index = Math.min(tick, round.prices().length - 1);
            double price = round.prices()[index];
            payout = value(round.amount(), price);
            details.put("crashed", false);
            details.put("price", price);
            details.put("autoSold", round.endedAt(tick));
        }
        details.put("chart", Arrays.copyOf(round.prices(), Math.min(round.prices().length, tick + 1)));
        play.settle(round.betId(), payout, details);
    }

    private CryptoStateResponse liveState(Round round, int tick, int from) {
        int start = Math.clamp(from, 0, tick);
        List<Double> prices = new ArrayList<>(tick - start + 1);
        for (int i = start; i <= tick; i++) {
            prices.add(round.prices()[i]);
        }
        double price = round.prices()[tick];
        return new CryptoStateResponse(round.betId(), round.amount(), start, tick, prices, price,
                value(round.amount(), price), false, false, BetStatus.PLACED, BigDecimal.ZERO, null);
    }

    private CryptoStateResponse finishedState(Long userId, Long betId) {
        var bet = bets.findById(betId).orElseThrow(() -> CasinoException.notFound("Раунд не знайдено"));
        if (!bet.getUser().getId().equals(userId)) {
            throw CasinoException.forbidden("Це не ваш раунд");
        }
        boolean crashed = bet.getResult() != null && bet.getResult().contains("\"crashed\":true");
        return new CryptoStateResponse(betId, bet.getAmount(), 0, 0, List.of(), 0, bet.getPayout(),
                bet.getStatus() != BetStatus.PLACED, crashed, bet.getStatus(), bet.getPayout(),
                wallet.balanceOf(userId));
    }

    private void requireOwner(Round round, Long userId) {
        if (!round.userId().equals(userId)) {
            throw CasinoException.forbidden("Це не ваш раунд");
        }
    }

    private static BigDecimal value(BigDecimal amount, double price) {
        return amount.multiply(BigDecimal.valueOf(price * PricePathGenerator.SELL_FEE)).setScale(2, RoundingMode.DOWN);
    }
}
