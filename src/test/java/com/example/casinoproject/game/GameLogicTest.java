package com.example.casinoproject.game;

import com.example.casinoproject.payment.CardValidator;
import com.example.casinoproject.security.CryptoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Перевірки ігрової математики і шифрування без піднімання Spring-контексту і бази.
 */
class GameLogicTest {

    private final SecureRandom random = new SecureRandom();
    private final HouseGuard guard = new HouseGuard(new BigDecimal("0.05"), new BigDecimal("5000"));

    @Test
    void guardLimitsWinToShareOfBank() {
        assertEquals(new BigDecimal("50.00"), guard.maxWin(new BigDecimal("1000")));
        assertEquals(new BigDecimal("5000.00"), guard.maxWin(new BigDecimal("1000000")));
        assertEquals(BigDecimal.ZERO, guard.maxWin(BigDecimal.ZERO));
    }

    @Test
    void gamesNeverPayMoreThanLimit() {
        BigDecimal bet = new BigDecimal("100");
        BigDecimal limit = guard.maxWin(new BigDecimal("1000"));
        SlotMachine slots = new SlotMachine(random);
        DiceGame dice = new DiceGame(random);
        for (int i = 0; i < 5000; i++) {
            assertTrue(slots.spin(bet, limit).payout().subtract(bet).compareTo(limit) <= 0);
            assertTrue(dice.roll(bet, 2, limit).payout().subtract(bet).compareTo(limit) <= 0);
            PricePathGenerator.Path path = new PricePathGenerator(random).generate(bet, limit, 400);
            for (double price : path.prices()) {
                assertTrue(100 * price * PricePathGenerator.SELL_FEE - 100 <= limit.doubleValue());
            }
        }
    }

    @Test
    void houseHasEdgeInSlotsAndDice() {
        BigDecimal bet = BigDecimal.TEN;
        BigDecimal noLimit = new BigDecimal("1000000");
        SlotMachine slots = new SlotMachine(random);
        DiceGame dice = new DiceGame(random);
        BigDecimal slotsPaid = BigDecimal.ZERO;
        BigDecimal dicePaid = BigDecimal.ZERO;
        int rounds = 100_000;
        for (int i = 0; i < rounds; i++) {
            slotsPaid = slotsPaid.add(slots.spin(bet, noLimit).payout());
            dicePaid = dicePaid.add(dice.roll(bet, 50, noLimit).payout());
        }
        BigDecimal wagered = bet.multiply(BigDecimal.valueOf(rounds));
        assertTrue(slotsPaid.compareTo(wagered) < 0, "слоти мають бути в плюс казино");
        assertTrue(dicePaid.compareTo(wagered) < 0, "кубик має бути в плюс казино");
    }

    @Test
    void cardChecksAndEncryption() {
        assertTrue(CardValidator.isValidNumber(CardValidator.normalize("4242 4242 4242 4242")));
        assertFalse(CardValidator.isValidNumber("4242424242424241"));

        CryptoService crypto = new CryptoService("q1c2Vb9xJ0mXKk6l3Q3b5N7rT2wYfE8uA4sD6gH1jZo=", random);
        String encrypted = crypto.encrypt("4242424242424242");
        assertNotEquals("4242424242424242", encrypted);
        assertEquals("4242424242424242", crypto.decrypt(encrypted));
        assertEquals("4242 **** **** 4242", CryptoService.maskCard("4242424242424242"));
    }
}
