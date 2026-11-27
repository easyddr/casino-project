package com.example.casinoproject.dao;

import java.math.BigDecimal;

/**
 * Гроші самого казино (рядок з id = 1 у таблиці house_bank).
 */
public interface HouseBankDao {

    BigDecimal getBalance();

    BigDecimal lockBalance();

    void add(BigDecimal delta);
}
