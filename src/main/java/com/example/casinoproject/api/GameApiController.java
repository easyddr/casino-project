package com.example.casinoproject.api;

import com.example.casinoproject.domain.GameType;
import com.example.casinoproject.dto.GameRequest;
import com.example.casinoproject.dto.GameResponse;
import com.example.casinoproject.dto.JsonPatchOperation;
import com.example.casinoproject.dto.PageResponse;
import com.example.casinoproject.service.GameCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Games", description = "Каталог ігор. Читати може будь-хто, змінювати тільки ADMIN")
public class GameApiController {

    private final GameCatalogService catalog;

    public GameApiController(GameCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "Пошук ігор з фільтрами і пагінацією",
            description = "Повертає сторінку ігор. Усі фільтри необов'язкові: type точно збігається з типом, active фільтрує " +
                    "за увімкненістю, name шукається як підрядок без урахування регістру. Пагінація через page (з нуля), size " +
                    "і sort, наприклад sort=minBet,desc.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сторінка з іграми"),
            @ApiResponse(responseCode = "400", description = "Невірне значення фільтра (наприклад, невідомий type)")
    })
    public PageResponse<GameResponse> search(
            @Parameter(description = "Тип гри") @RequestParam(required = false) GameType type,
            @Parameter(description = "Тільки увімкнені або тільки вимкнені") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Частина назви") @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return catalog.search(type, active, name, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Одна гра за id", description = "Повертає повну інформацію про гру, включно з лімітами ставок.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Гру знайдено"),
            @ApiResponse(responseCode = "404", description = "Гри з таким id нема")
    })
    public GameResponse get(@Parameter(description = "id гри") @PathVariable Long id) {
        return catalog.get(id);
    }

    @PostMapping
    @Operation(summary = "Створити гру",
            description = "Додає нову гру в каталог. Назва має бути унікальною, maxBet не менше за minBet. " +
                    "У відповіді заголовок Location вказує на створений ресурс.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Гру створено"),
            @ApiResponse(responseCode = "400", description = "Помилка валідації"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Потрібна роль ADMIN"),
            @ApiResponse(responseCode = "409", description = "Гра з такою назвою вже існує")
    })
    public ResponseEntity<GameResponse> create(@Valid @RequestBody GameRequest request) {
        GameResponse created = catalog.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Повністю оновити гру", description = "Замінює всі поля гри значеннями з тіла запиту. Поля, яких нема в тілі, вважаються порожніми.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Гру оновлено"),
            @ApiResponse(responseCode = "400", description = "Помилка валідації"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Потрібна роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Гру не знайдено"),
            @ApiResponse(responseCode = "409", description = "Назва зайнята іншою грою")
    })
    public GameResponse update(@PathVariable Long id, @Valid @RequestBody GameRequest request) {
        return catalog.update(id, request);
    }

    @PatchMapping(value = "/{id}", consumes = "application/merge-patch+json")
    @Operation(summary = "Часткове оновлення (JSON Merge Patch, RFC 7386)",
            description = "Тіло містить тільки поля, які треба змінити, наприклад {\"maxBet\": 750, \"active\": false}. " +
                    "Content-Type має бути application/merge-patch+json.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Гру оновлено"),
            @ApiResponse(responseCode = "400", description = "Невідоме поле або результат не проходить валідацію"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Потрібна роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Гру не знайдено"),
            @ApiResponse(responseCode = "415", description = "Невірний Content-Type")
    })
    public GameResponse mergePatch(@PathVariable Long id, @RequestBody Map<String, Object> patch) {
        return catalog.mergePatch(id, patch);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    @Operation(summary = "Часткове оновлення (JSON Patch, RFC 6902)",
            description = "Тіло це масив операцій, наприклад [{\"op\":\"test\",\"path\":\"/active\",\"value\":true}," +
                    "{\"op\":\"replace\",\"path\":\"/maxBet\",\"value\":900}]. Підтримуються add, replace, remove і test. " +
                    "Операції застосовуються по черзі, і якщо хоч одна не пройшла, гра не змінюється. Content-Type application/json-patch+json.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Гру оновлено"),
            @ApiResponse(responseCode = "400", description = "Невідома операція, поле або невалідний результат"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Потрібна роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Гру не знайдено"),
            @ApiResponse(responseCode = "409", description = "Операція test не пройшла"),
            @ApiResponse(responseCode = "415", description = "Невірний Content-Type")
    })
    public GameResponse jsonPatch(@PathVariable Long id, @Valid @RequestBody List<JsonPatchOperation> ops) {
        return catalog.jsonPatch(id, ops);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити гру", description = "Видаляє гру, якщо на неї ще не було жодної ставки. Інакше її можна тільки вимкнути.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Гру видалено"),
            @ApiResponse(responseCode = "401", description = "Не авторизовано"),
            @ApiResponse(responseCode = "403", description = "Потрібна роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Гру не знайдено"),
            @ApiResponse(responseCode = "409", description = "На гру вже є ставки")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        catalog.delete(id);
        return ResponseEntity.noContent().build();
    }
}
