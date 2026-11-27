package com.example.casinoproject.service;

import com.example.casinoproject.dao.HouseBankDao;
import com.example.casinoproject.dao.TransactionDao;
import com.example.casinoproject.domain.BetStatus;
import com.example.casinoproject.domain.TransactionType;
import com.example.casinoproject.dto.AdminStats;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.game.HouseGuard;
import com.example.casinoproject.repository.BetRepository;
import com.example.casinoproject.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminService {

    private final HouseBankDao bank;
    private final HouseGuard guard;
    private final UserAccountRepository users;
    private final BetRepository bets;
    private final TransactionDao transactions;

    public AdminService(HouseBankDao bank, HouseGuard guard, UserAccountRepository users, BetRepository bets,
                        TransactionDao transactions) {
        this.bank = bank;
        this.guard = guard;
        this.users = users;
        this.bets = bets;
        this.transactions = transactions;
    }

    /**
     * Загальна картина по казино. Різниця між wagered і paidOut у кожній грі це заробіток казино.
     */
    @Transactional(readOnly = true)
    public AdminStats stats() {
        return new AdminStats(
                bank.getBalance(),
                users.totalPlayerBalance(),
                transactions.sumByType(TransactionType.DEPOSIT),
                transactions.sumByType(TransactionType.WITHDRAWAL),
                transactions.sumByType(TransactionType.BONUS),
                bets.gameStats(List.of(BetStatus.WON, BetStatus.LOST)));
    }

    public BigDecimal currentMaxWin() {
        return guard.maxWin(bank.getBalance());
    }

    /**
     * Поповнює або зменшує банк казино вручну (від'ємна сума зменшує). Від банку залежить максимальний виграш,
     * тому якщо банк малий, великі виграші просто не будуть випадати.
     */
    @Transactional
    public BigDecimal adjustBank(BigDecimal delta) {
        BigDecimal current = bank.lockBalance();
        if (current.add(delta).signum() < 0) {
            throw CasinoException.unprocessable("Банк не може стати від'ємним");
        }
        bank.add(delta);
        return current.add(delta);
    }
}
