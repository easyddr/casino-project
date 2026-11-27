package com.example.casinoproject.service;

import com.example.casinoproject.dao.HouseBankDao;
import com.example.casinoproject.domain.*;
import com.example.casinoproject.dto.BetView;
import com.example.casinoproject.dto.PageResponse;
import com.example.casinoproject.dto.RoundResult;
import com.example.casinoproject.dto.WinView;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.game.DiceGame;
import com.example.casinoproject.game.GameOutcome;
import com.example.casinoproject.game.HouseGuard;
import com.example.casinoproject.game.SlotMachine;
import com.example.casinoproject.repository.BetRepository;
import com.example.casinoproject.repository.GameRepository;
import com.example.casinoproject.repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Спільна логіка ставок для всіх ігор. Раунд складається з двох кроків: openBet списує ставку і кладе її в банк
 * казино, settle виплачує виграш і закриває ставку. Миттєві ігри (слоти, кубик) роблять обидва кроки
 * в одній транзакції, крипто-гра відкриває ставку на старті і закриває при продажу або обвалі.
 */
@Service
public class PlayService {

    private final UserAccountRepository users;
    private final GameRepository games;
    private final BetRepository bets;
    private final WalletService wallet;
    private final HouseBankDao bank;
    private final HouseGuard guard;
    private final SlotMachine slots;
    private final DiceGame dice;
    private final ObjectMapper json;

    public PlayService(UserAccountRepository users, GameRepository games, BetRepository bets, WalletService wallet,
                       HouseBankDao bank, HouseGuard guard, SlotMachine slots, DiceGame dice, ObjectMapper json) {
        this.users = users;
        this.games = games;
        this.bets = bets;
        this.wallet = wallet;
        this.bank = bank;
        this.guard = guard;
        this.slots = slots;
        this.dice = dice;
        this.json = json;
    }

    /**
     * Відкрита ставка і ліміт чистого виграшу, який порахував HouseGuard від банку на момент ставки.
     */
    public record OpenedBet(Long betId, BigDecimal amount, BigDecimal balance, BigDecimal maxWin) {
    }

    /**
     * Один спін слотів. Транзакційний: якщо щось впаде посередині, не буде ні списання, ні виграшу.
     */
    @Transactional
    public RoundResult spinSlots(Long userId, Long gameId, BigDecimal amount) {
        OpenedBet opened = openBet(userId, gameId, GameType.SLOTS, amount);
        GameOutcome outcome = slots.spin(opened.amount(), opened.maxWin());
        return settle(opened.betId(), outcome.payout(), outcome.details());
    }

    /**
     * Один кидок кубика з обраним шансом (2..95 відсотків).
     */
    @Transactional
    public RoundResult rollDice(Long userId, Long gameId, BigDecimal amount, int chance) {
        OpenedBet opened = openBet(userId, gameId, GameType.DICE, amount);
        GameOutcome outcome = dice.roll(opened.amount(), chance, opened.maxWin());
        return settle(opened.betId(), outcome.payout(), outcome.details());
    }

    /**
     * Перевіряє гру і ліміти ставки, блокує гравця, списує ставку і кладе її в банк казино.
     * Порядок блокувань завжди однаковий (спочатку гравець, потім банк), щоб не було дедлоків.
     */
    @Transactional
    public OpenedBet openBet(Long userId, Long gameId, GameType expectedType, BigDecimal rawAmount) {
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.DOWN);
        Game game = games.findById(gameId).orElseThrow(() -> CasinoException.notFound("Гру не знайдено"));
        if (game.getType() != expectedType) {
            throw CasinoException.badRequest("Ця гра має інший тип: " + game.getType());
        }
        if (!game.isActive()) {
            throw CasinoException.conflict("Гра зараз вимкнена");
        }
        if (amount.compareTo(game.getMinBet()) < 0 || amount.compareTo(game.getMaxBet()) > 0) {
            throw CasinoException.badRequest("Ставка має бути від " + game.getMinBet() + " до " + game.getMaxBet());
        }

        UserAccount user = users.lockById(userId).orElseThrow(() -> CasinoException.notFound("Користувача не знайдено"));
        if (user.isBlocked()) {
            throw CasinoException.forbidden("Акаунт заблоковано");
        }
        if (user.getBalance().compareTo(amount) < 0) {
            throw CasinoException.unprocessable("Недостатньо коштів на балансі");
        }

        Bet bet = new Bet();
        bet.setUser(user);
        bet.setGame(game);
        bet.setAmount(amount);
        bets.save(bet);

        BigDecimal balance = wallet.debit(user, amount, TransactionType.BET, bet.getId());
        BigDecimal bankAfter = bank.lockBalance().add(amount);
        bank.add(amount);
        return new OpenedBet(bet.getId(), amount, balance, guard.maxWin(bankAfter));
    }

    /**
     * Закриває ставку з виплатою payout (0 означає програш). Статус WON ставиться тільки якщо гравець
     * отримав більше, ніж поставив. Повторно закрити ту саму ставку не вийде, буде 409.
     */
    @Transactional
    public RoundResult settle(Long betId, BigDecimal rawPayout, Map<String, Object> details) {
        Bet bet = bets.findById(betId).orElseThrow(() -> CasinoException.notFound("Ставку не знайдено"));
        if (bet.getStatus() != BetStatus.PLACED) {
            throw CasinoException.conflict("Ставку вже розраховано");
        }
        UserAccount user = users.lockById(bet.getUser().getId()).orElseThrow();
        bank.lockBalance();

        BigDecimal payout = rawPayout.setScale(2, RoundingMode.DOWN);
        BigDecimal balance = user.getBalance();
        if (payout.signum() > 0) {
            balance = wallet.credit(user, payout, TransactionType.WIN, betId);
            bank.add(payout.negate());
        }
        bet.setPayout(payout);
        bet.setStatus(payout.compareTo(bet.getAmount()) > 0 ? BetStatus.WON : BetStatus.LOST);
        bet.setResult(json.writeValueAsString(details));
        bet.setSettledAt(OffsetDateTime.now());
        return new RoundResult(bet.getId(), bet.getAmount(), payout, bet.getStatus(), balance, details);
    }

    /**
     * Скасовує завису ставку і повертає гравцю гроші (наприклад, якщо сервер перезапустився посеред крипто-раунду).
     */
    @Transactional
    public void cancel(Long betId, String reason) {
        Bet bet = bets.findById(betId).orElseThrow(() -> CasinoException.notFound("Ставку не знайдено"));
        if (bet.getStatus() != BetStatus.PLACED) {
            return;
        }
        UserAccount user = users.lockById(bet.getUser().getId()).orElseThrow();
        bank.lockBalance();
        wallet.credit(user, bet.getAmount(), TransactionType.REFUND, betId);
        bank.add(bet.getAmount().negate());
        bet.setStatus(BetStatus.CANCELLED);
        bet.setResult(json.writeValueAsString(Map.of("reason", reason)));
        bet.setSettledAt(OffsetDateTime.now());
    }

    /**
     * Історія ставок гравця, status можна не передавати.
     */
    @Transactional(readOnly = true)
    public PageResponse<BetView> history(Long userId, BetStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        var result = status == null
                ? bets.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : bets.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        return PageResponse.from(result, BetView::from);
    }

    @Transactional(readOnly = true)
    public List<WinView> biggestWins(int limit) {
        return bets.findBiggestWins(BetStatus.WON, PageRequest.of(0, limit)).stream()
                .map(b -> new WinView(b.getUser().getUsername(), b.getGame().getName(), b.getAmount(), b.getPayout()))
                .toList();
    }

    public List<Map<String, Object>> slotsPaytable() {
        return slots.paytable();
    }
}
