package com.example.casinoproject.payment;

import java.time.YearMonth;

/**
 * Перевірки картки, які робляться ще до звернення до шлюзу.
 */
public final class CardValidator {

    private CardValidator() {
    }

    /**
     * Прибирає пробіли і дефіси, повертає тільки цифри. Наприклад "4242-4242 4242 4242" стане "4242424242424242".
     */
    public static String normalize(String number) {
        return number == null ? "" : number.replaceAll("[\\s-]", "");
    }

    /**
     * Алгоритм Луна, яким перевіряють контрольну цифру номера картки. Відсікає випадкові помилки при введенні.
     */
    public static boolean isValidNumber(String digits) {
        if (!digits.matches("\\d{13,19}")) {
            return false;
        }
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    public static boolean isNotExpired(int month, int year) {
        return !YearMonth.of(year, month).isBefore(YearMonth.now());
    }
}
