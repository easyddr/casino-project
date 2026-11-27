package com.example.casinoproject.api;

import com.example.casinoproject.dao.Promotion;
import com.example.casinoproject.dto.AdminStats;
import com.example.casinoproject.dto.PromotionRequest;
import com.example.casinoproject.dto.UserView;
import com.example.casinoproject.service.AdminService;
import com.example.casinoproject.service.PromotionService;
import com.example.casinoproject.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Адміністрування. Всі операції потребують ролі ADMIN")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Не авторизовано"),
        @ApiResponse(responseCode = "403", description = "Потрібна роль ADMIN")
})
public class AdminApiController {

    private final AdminService admin;
    private final UserService users;
    private final PromotionService promotions;

    public AdminApiController(AdminService admin, UserService users, PromotionService promotions) {
        this.admin = admin;
        this.users = users;
        this.promotions = promotions;
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика казино", description = "Банк казино, сума балансів гравців, обороти платежів і бонусів, а також поставлено і виплачено по кожній грі.")
    @ApiResponse(responseCode = "200", description = "Статистика")
    public AdminStats stats() {
        return admin.stats();
    }

    @PostMapping("/house/adjust")
    @Operation(summary = "Змінити банк казино", description = "Додає delta до банку (від'ємне значення зменшує). Від банку залежить максимальний можливий виграш.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Новий баланс банку"),
            @ApiResponse(responseCode = "422", description = "Банк став би від'ємним")
    })
    public Map<String, BigDecimal> adjust(@Parameter(description = "Зміна суми") @RequestParam BigDecimal delta) {
        return Map.of("houseBank", admin.adjustBank(delta), "maxWin", admin.currentMaxWin());
    }

    @GetMapping("/users")
    @Operation(summary = "Список користувачів", description = "Усі користувачі від нових до старих.")
    @ApiResponse(responseCode = "200", description = "Список")
    public List<UserView> users() {
        return users.all();
    }

    @PatchMapping("/users/{id}/blocked")
    @Operation(summary = "Заблокувати або розблокувати", description = "blocked=true блокує гравця (не зможе зайти і грати), false знімає блок. Адміна заблокувати не можна.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Оновлений користувач"),
            @ApiResponse(responseCode = "404", description = "Користувача не знайдено"),
            @ApiResponse(responseCode = "409", description = "Спроба заблокувати адміністратора")
    })
    public UserView block(@PathVariable Long id, @RequestParam boolean blocked) {
        return users.setBlocked(id, blocked);
    }

    @GetMapping("/promotions")
    @Operation(summary = "Усі промокоди", description = "Включно з вимкненими і простроченими.")
    @ApiResponse(responseCode = "200", description = "Список")
    public List<Promotion> promotions() {
        return promotions.all();
    }

    @GetMapping("/promotions/{id}")
    @Operation(summary = "Промокод за id", description = "Повна інформація про один промокод.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Промокод"),
            @ApiResponse(responseCode = "404", description = "Не знайдено")
    })
    public Promotion promotion(@PathVariable long id) {
        return promotions.get(id);
    }

    @PostMapping("/promotions")
    @Operation(summary = "Створити промокод", description = "Код зберігається у верхньому регістрі. validUntil можна не задавати, тоді код безстроковий.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Створено"),
            @ApiResponse(responseCode = "400", description = "Помилка валідації"),
            @ApiResponse(responseCode = "409", description = "Такий код вже існує")
    })
    public ResponseEntity<Promotion> createPromotion(@Valid @RequestBody PromotionRequest request) {
        Promotion created = promotions.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/promotions/{id}")
    @Operation(summary = "Оновити промокод", description = "Повна заміна полів промокоду.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Оновлено"),
            @ApiResponse(responseCode = "400", description = "Помилка валідації"),
            @ApiResponse(responseCode = "404", description = "Не знайдено"),
            @ApiResponse(responseCode = "409", description = "Такий код вже існує")
    })
    public Promotion updatePromotion(@PathVariable long id, @Valid @RequestBody PromotionRequest request) {
        return promotions.update(id, request);
    }

    @DeleteMapping("/promotions/{id}")
    @Operation(summary = "Видалити промокод", description = "Видаляє код і записи про його активацію. Вже нараховані бонуси не забираються.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Видалено"),
            @ApiResponse(responseCode = "404", description = "Не знайдено")
    })
    public ResponseEntity<Void> deletePromotion(@PathVariable long id) {
        promotions.delete(id);
        return ResponseEntity.noContent().build();
    }
}
