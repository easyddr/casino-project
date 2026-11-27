package com.example.casinoproject.service;

import com.example.casinoproject.dao.TransactionDao;
import com.example.casinoproject.dao.TransactionRecord;
import com.example.casinoproject.domain.TransactionType;
import com.example.casinoproject.domain.UserAccount;
import com.example.casinoproject.dto.PageResponse;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Усі зміни балансу гравця проходять тут, і кожна зміна одразу пишеться в журнал transactions.
 * Методи debit і credit працюють тільки всередині вже відкритої транзакції (MANDATORY),
 * тому гроші не можуть списатися без запису в журнал або навпаки.
 */
@Service
public class WalletService {

    private final UserAccountRepository users;
    private TransactionDao transactionDao;

    public WalletService(UserAccountRepository users) {
        this.users = users;
    }

    @Autowired
    public void setTransactionDao(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /**
     * Списує amount з користувача, якого перед цим заблокували через UserAccountRepository.lockById.
     * Якщо грошей не вистачає, кидає 422 і транзакція відкочується. Повертає новий баланс.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal debit(UserAccount lockedUser, BigDecimal amount, TransactionType type, Long betId) {
        if (lockedUser.getBalance().compareTo(amount) < 0) {
            throw CasinoException.unprocessable("Недостатньо коштів на балансі");
        }
        BigDecimal after = lockedUser.getBalance().subtract(amount);
        lockedUser.setBalance(after);
        transactionDao.create(TransactionRecord.of(lockedUser.getId(), betId, type, amount, after));
        return after;
    }

    /**
     * Нараховує amount (виграш, поповнення, бонус, повернення ставки). Повертає новий баланс.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal credit(UserAccount lockedUser, BigDecimal amount, TransactionType type, Long betId) {
        BigDecimal after = lockedUser.getBalance().add(amount);
        lockedUser.setBalance(after);
        transactionDao.create(TransactionRecord.of(lockedUser.getId(), betId, type, amount, after));
        return after;
    }

    @Transactional(readOnly = true)
    public BigDecimal balanceOf(Long userId) {
        return users.findById(userId)
                .map(UserAccount::getBalance)
                .orElseThrow(() -> CasinoException.notFound("Користувача не знайдено"));
    }

    /**
     * Сторінка журналу транзакцій гравця. type можна не передавати, тоді будуть усі операції.
     */
    @Transactional(readOnly = true)
    public PageResponse<TransactionRecord> transactions(Long userId, TransactionType type, int page, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        int safePage = Math.max(page, 0);
        List<TransactionRecord> rows = transactionDao.findByUser(userId, type, safeSize, safePage * safeSize);
        long total = transactionDao.countByUser(userId, type);
        int pages = (int) Math.ceil(total / (double) safeSize);
        return new PageResponse<>(rows, safePage, safeSize, total, pages);
    }
}
