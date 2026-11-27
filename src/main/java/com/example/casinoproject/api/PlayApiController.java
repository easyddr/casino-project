package com.example.casinoproject.api;

import com.example.casinoproject.dto.BetRequest;
import com.example.casinoproject.dto.CryptoStateResponse;
import com.example.casinoproject.dto.DiceRequest;
import com.example.casinoproject.dto.RoundResult;
import com.example.casinoproject.security.CasinoUserPrincipal;
import com.example.casinoproject.service.CryptoTradingService;
import com.example.casinoproject.service.PlayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/play")
@Tag(name = "Play", description = "Ігри. Всі операції від імені залогіненого гравця")
public class PlayApiController {

    private final PlayService play;
    private final CryptoTradingService crypto;

    public PlayApiController(PlayService play, CryptoTradingService crypto) {
        this.play = play;
        this.crypto = crypto;
    }

    @PostMapping("/slots/{gameId}")
    @Operation(summary = "Спін слотів",
            description = "Списує ставку, крутить три барабани і одразу нараховує виграш, якщо він є. " +
                    "У details повертаються символи на барабанах і множник.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Раунд зіграно"),
            @ApiResponse(responseCode = "400", description = "Ставка поза лімітами гри або гра іншого типу"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Акаунт заблоковано"),
            @ApiResponse(responseCode = "404", description = "Гру не знайдено"),
            @ApiResponse(responseCode = "409", description = "Гра вимкнена"),
            @ApiResponse(responseCode = "422", description = "Недостатньо коштів")
    })
    public RoundResult slots(@AuthenticationPrincipal CasinoUserPrincipal me,
                             @Parameter(description = "id гри типу SLOTS") @PathVariable Long gameId,
                             @Valid @RequestBody BetRequest request) {
        return play.spinSlots(me.getId(), gameId, request.amount());
    }

    @PostMapping("/dice/{gameId}")
    @Operation(summary = "Кидок кубика",
            description = "Гравець виграє, якщо випало число менше за chance. Виплата дорівнює ставці помноженій на 96/chance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Раунд зіграно"),
            @ApiResponse(responseCode = "400", description = "Невірна ставка або шанс"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Акаунт заблоковано"),
            @ApiResponse(responseCode = "404", description = "Гру не знайдено"),
            @ApiResponse(responseCode = "409", description = "Гра вимкнена"),
            @ApiResponse(responseCode = "422", description = "Недостатньо коштів")
    })
    public RoundResult dice(@AuthenticationPrincipal CasinoUserPrincipal me, @PathVariable Long gameId,
                            @Valid @RequestBody DiceRequest request) {
        return play.rollDice(me.getId(), gameId, request.amount(), request.chance());
    }

    @PostMapping("/crypto/{gameId}/buy")
    @Operation(summary = "Купити монету (старт крипто-раунду)",
            description = "Списує ставку і запускає графік ціни. Ціна оновлюється кожні 300 мс. Повертає betId, " +
                    "за яким далі треба опитувати стан і продавати. Одночасно може бути тільки одна відкрита позиція.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Позицію відкрито"),
            @ApiResponse(responseCode = "400", description = "Ставка поза лімітами"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "404", description = "Гру не знайдено"),
            @ApiResponse(responseCode = "409", description = "Вже є відкрита позиція або гра вимкнена"),
            @ApiResponse(responseCode = "422", description = "Недостатньо коштів")
    })
    public CryptoStateResponse buy(@AuthenticationPrincipal CasinoUserPrincipal me, @PathVariable Long gameId,
                                   @Valid @RequestBody BetRequest request) {
        return crypto.start(me.getId(), gameId, request.amount());
    }

    @GetMapping("/crypto/rounds/{betId}")
    @Operation(summary = "Стан крипто-раунду",
            description = "Віддає ціни від тіку from до поточного. Якщо монета вже обвалилась, раунд закривається " +
                    "і finished = true, crashed = true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Поточний стан"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Раунд належить іншому гравцю"),
            @ApiResponse(responseCode = "404", description = "Раунд не знайдено")
    })
    public CryptoStateResponse state(@AuthenticationPrincipal CasinoUserPrincipal me, @PathVariable Long betId,
                                     @Parameter(description = "З якого тіку віддати ціни") @RequestParam(defaultValue = "0") int from) {
        return crypto.state(me.getId(), betId, from);
    }

    @PostMapping("/crypto/rounds/{betId}/sell")
    @Operation(summary = "Продати монету",
            description = "Фіксує ціну на момент приходу запиту і виплачує ставку помножену на ціну мінус 3% комісії. " +
                    "Якщо обвал стався раніше, виплата 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Раунд закрито, дивіться payout і status"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Раунд належить іншому гравцю"),
            @ApiResponse(responseCode = "404", description = "Раунд не знайдено")
    })
    public CryptoStateResponse sell(@AuthenticationPrincipal CasinoUserPrincipal me, @PathVariable Long betId) {
        return crypto.sell(me.getId(), betId);
    }
}
