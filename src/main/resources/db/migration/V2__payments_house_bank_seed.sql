ALTER TABLE games DROP CONSTRAINT IF EXISTS games_type_check;
ALTER TABLE games ADD CONSTRAINT games_type_check
    CHECK (type IN ('SLOTS', 'ROULETTE', 'BLACKJACK', 'DICE', 'CRYPTO'));

ALTER TABLE bets DROP CONSTRAINT IF EXISTS bets_status_check;
ALTER TABLE bets ADD CONSTRAINT bets_status_check
    CHECK (status IN ('PLACED', 'WON', 'LOST', 'CANCELLED'));

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_type_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_type_check
    CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'BET', 'WIN', 'BONUS', 'REFUND'));

CREATE TABLE payments (
                          id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          user_id        BIGINT NOT NULL REFERENCES users(id),
                          type           VARCHAR(12) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAWAL')),
                          amount         NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                          status         VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
                          card_mask      VARCHAR(25) NOT NULL,
                          card_encrypted TEXT NOT NULL,
                          provider_ref   VARCHAR(64),
                          error_message  VARCHAR(255),
                          created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_user_created ON payments(user_id, created_at DESC);

CREATE TABLE house_bank (
                            id         SMALLINT PRIMARY KEY CHECK (id = 1),
                            balance    NUMERIC(16,2) NOT NULL CHECK (balance >= 0),
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO house_bank (id, balance) VALUES (1, 100000.00);

INSERT INTO games (name, type, description, min_bet, max_bet) VALUES
    ('Crypto Trader', 'CRYPTO', 'Купи монету, дивись на графік і продай вчасно. Ринок може обвалитись будь-якої секунди.', 1.00, 1000.00),
    ('Lucky Slots',   'SLOTS',  'Класичні три барабани. Три однакові символи або дві вишні дають виграш.', 0.50, 500.00),
    ('Dice 100',      'DICE',   'Обери шанс від 2 до 95 відсотків. Чим менший шанс, тим більший множник.', 0.50, 1000.00);

INSERT INTO promotions (code, title, bonus_amount, valid_until) VALUES
    ('WELCOME50', 'Вітальний бонус', 50.00, now() + INTERVAL '365 days'),
    ('FRIDAY10',  'П''ятничний бонус', 10.00, now() + INTERVAL '30 days');
