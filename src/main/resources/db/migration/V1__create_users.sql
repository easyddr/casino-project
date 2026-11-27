-- V1__init_schema.sql  (PostgreSQL / Supabase)
CREATE TABLE users (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       username      VARCHAR(50)  NOT NULL UNIQUE,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(10)  NOT NULL DEFAULT 'PLAYER'
                           CHECK (role IN ('PLAYER', 'ADMIN')),
                       balance       NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
                       is_blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE games (
                       id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       name        VARCHAR(100) NOT NULL UNIQUE,
                       type        VARCHAR(20)  NOT NULL
                           CHECK (type IN ('SLOTS', 'ROULETTE', 'BLACKJACK', 'DICE')),
                       description TEXT,
                       min_bet     NUMERIC(14,2) NOT NULL CHECK (min_bet > 0),
                       max_bet     NUMERIC(14,2) NOT NULL,
                       is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CHECK (max_bet >= min_bet)
);

CREATE TABLE bets (
                      id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      user_id    BIGINT NOT NULL REFERENCES users(id),
                      game_id    BIGINT NOT NULL REFERENCES games(id),
                      amount     NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                      payout     NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (payout >= 0),
                      status     VARCHAR(10)  NOT NULL DEFAULT 'PLACED'
                          CHECK (status IN ('PLACED', 'WON', 'LOST')),
                      result     JSONB,                       -- деталі раунду: випали символи, число рулетки тощо
                      created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                      settled_at TIMESTAMPTZ
);

CREATE TABLE transactions (
                              id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              user_id       BIGINT NOT NULL REFERENCES users(id),
                              bet_id        BIGINT REFERENCES bets(id),       -- заповнено для BET / WIN
                              type          VARCHAR(12) NOT NULL
                                  CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'BET', 'WIN', 'BONUS')),
                              amount        NUMERIC(14,2) NOT NULL CHECK (amount > 0),
                              balance_after NUMERIC(14,2) NOT NULL,
                              created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE promotions (
                            id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            code         VARCHAR(30) NOT NULL UNIQUE,
                            title        VARCHAR(100) NOT NULL,
                            bonus_amount NUMERIC(14,2) NOT NULL CHECK (bonus_amount > 0),
                            valid_until  TIMESTAMPTZ,
                            is_active    BOOLEAN NOT NULL DEFAULT TRUE
);

-- many-to-many: користувач активував акцію (один раз)
CREATE TABLE user_promotions (
                                 user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
                                 activated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 PRIMARY KEY (user_id, promotion_id)
);

CREATE INDEX idx_bets_user_created ON bets(user_id, created_at DESC);
CREATE INDEX idx_bets_game         ON bets(game_id);
CREATE INDEX idx_tx_user_created   ON transactions(user_id, created_at DESC);