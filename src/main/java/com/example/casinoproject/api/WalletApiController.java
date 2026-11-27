package com.example.casinoproject.api;

import com.example.casinoproject.dao.TransactionRecord;
import com.example.casinoproject.domain.BetStatus;
import com.example.casinoproject.domain.PaymentStatus;
import com.example.casinoproject.domain.TransactionType;
import com.example.casinoproject.dto.*;
import com.example.casinoproject.security.CasinoUserPrincipal;
import com.example.casinoproject.service.PaymentService;
import com.example.casinoproject.service.PlayService;
import com.example.casinoproject.service.PromotionService;
import com.example.casinoproject.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet", description = "Баланс, псевдо-платежі, історія і промокоди поточного гравця")
public class WalletApiController {

    private final WalletService wallet;
    private final PaymentService payments;
    private final PlayService play;
    private final PromotionService promotions;

    public WalletApiController(WalletService wallet, PaymentService payments, PlayService play, PromotionService promotions) {
        this.wallet = wallet;
        this.payments = payments;
        this.play = play;
        this.promotions = promotions;
    }

    @GetMapping("/balance")
    @Operation(summary = "Поточний баланс", description = "Повертає баланс залогіненого гравця.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано")
    })
    public Map<String, BigDecimal> balance(@AuthenticationPrincipal CasinoUserPrincipal me) {
        return Map.of("balance", wallet.balanceOf(me.getId()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Журнал транзакцій", description = "Сторінка руху грошей гравця від новіших до старіших. Можна відфільтрувати по типу.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сторінка транзакцій"),
            @ApiResponse(responseCode = "400", description = "Невідомий тип"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано")
    })
    public PageResponse<TransactionRecord> transactions(@AuthenticationPrincipal CasinoUserPrincipal me,
                                                        @Parameter(description = "Тип операції") @RequestParam(required = false) TransactionType type,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return wallet.transactions(me.getId(), type, page, size);
    }

    @GetMapping("/bets")
    @Operation(summary = "Історія ставок", description = "Сторінка ставок гравця, можна фільтрувати по статусу (WON, LOST, PLACED, CANCELLED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сторінка ставок"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано")
    })
    public PageResponse<BetView> bets(@AuthenticationPrincipal CasinoUserPrincipal me,
                                      @RequestParam(required = false) BetStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return play.history(me.getId(), status, page, size);
    }

    @PostMapping("/deposit")
    @Operation(summary = "Поповнити баланс",
            description = "Списує гроші з картки через псевдо-шлюз. Картка 4242 4242 4242 4242 проходить завжди, " +
                    "картка на 0002 відхиляється (повертається 402 і платіж зі статусом FAILED), на 0119 імітується збій шлюзу. " +
                    "Номер картки зберігається зашифрованим, CVV не зберігається.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Платіж успішний, баланс поповнено"),
            @ApiResponse(responseCode = "400", description = "Невірний номер картки, термін дії або сума"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "402", description = "Шлюз відхилив платіж, деталі в errorMessage"),
            @ApiResponse(responseCode = "403", description = "Акаунт заблоковано"),
            @ApiResponse(responseCode = "404", description = "Збережену картку не знайдено")
    })
    public ResponseEntity<PaymentResponse> deposit(@AuthenticationPrincipal CasinoUserPrincipal me,
                                                   @Valid @RequestBody DepositRequest request) {
        PaymentResponse result = payments.deposit(me.getId(), request);
        HttpStatus status = result.status() == PaymentStatus.SUCCESS ? HttpStatus.CREATED : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(status).body(result);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Вивести кошти",
            description = "Транзакційна операція: баланс списується, потім іде запит у шлюз. Якщо шлюз відмовив або впав " +
                    "(картка на 0119), все відкочується і баланс лишається як був.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Кошти виведено"),
            @ApiResponse(responseCode = "400", description = "Невірний номер картки або сума"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "402", description = "Шлюз відмовив, транзакцію відкочено"),
            @ApiResponse(responseCode = "403", description = "Акаунт заблоковано"),
            @ApiResponse(responseCode = "422", description = "Недостатньо коштів")
    })
    public ResponseEntity<PaymentResponse> withdraw(@AuthenticationPrincipal CasinoUserPrincipal me,
                                                    @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payments.withdraw(me.getId(), request));
    }

    @GetMapping("/payments")
    @Operation(summary = "Останні платежі", description = "До 20 останніх платежів гравця, номери карток замасковані.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список платежів"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано")
    })
    public List<PaymentResponse> paymentHistory(@AuthenticationPrincipal CasinoUserPrincipal me) {
        return payments.history(me.getId());
    }

    @GetMapping("/cards")
    @Operation(summary = "Збережені картки", description = "Маски карток з успішних поповнень. paymentId можна передати як savedPaymentId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список карток"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано")
    })
    public List<SavedCard> cards(@AuthenticationPrincipal CasinoUserPrincipal me) {
        return payments.savedCards(me.getId());
    }

    @PostMapping("/promo")
    @Operation(summary = "Активувати промокод", description = "Нараховує бонус, якщо код дійсний і ще не використовувався цим гравцем.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Бонус нараховано, повертається новий баланс"),
            @ApiResponse(responseCode = "400", description = "Порожній код"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "404", description = "Код не знайдено"),
            @ApiResponse(responseCode = "409", description = "Код вже використано або він неактивний")
    })
    public Map<String, BigDecimal> promo(@AuthenticationPrincipal CasinoUserPrincipal me,
                                         @Valid @RequestBody PromoCodeRequest request) {
        return Map.of("balance", promotions.activate(me.getId(), request.code()));
    }
}
