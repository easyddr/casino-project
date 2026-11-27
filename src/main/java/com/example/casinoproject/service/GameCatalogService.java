package com.example.casinoproject.service;

import com.example.casinoproject.domain.Game;
import com.example.casinoproject.domain.GameType;
import com.example.casinoproject.dto.GameRequest;
import com.example.casinoproject.dto.GameResponse;
import com.example.casinoproject.dto.JsonPatchOperation;
import com.example.casinoproject.dto.PageResponse;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.repository.BetRepository;
import com.example.casinoproject.repository.GameRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Каталог ігор: повний CRUD, фільтрація з пагінацією і часткове оновлення двома способами
 * (JSON Merge Patch RFC 7386 і JSON Patch RFC 6902).
 */
@Service
public class GameCatalogService {

    private static final Set<String> FIELDS = Set.of("name", "type", "description", "minBet", "maxBet", "active");

    private final GameRepository games;
    private final BetRepository bets;
    private final ObjectMapper json;
    private final Validator validator;

    public GameCatalogService(GameRepository games, BetRepository bets, ObjectMapper json, Validator validator) {
        this.games = games;
        this.bets = bets;
        this.json = json;
        this.validator = validator;
    }

    /**
     * Пошук ігор. Будь-який фільтр можна не передавати: type, active, а name шукається як підрядок без урахування регістру.
     * Сортування і розмір сторінки приходять у Pageable (?page=0&size=10&sort=name,asc).
     */
    @Transactional(readOnly = true)
    public PageResponse<GameResponse> search(GameType type, Boolean active, String name, Pageable pageable) {
        Specification<Game> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (type != null) {
                p.add(cb.equal(root.get("type"), type));
            }
            if (active != null) {
                p.add(cb.equal(root.get("active"), active));
            }
            if (name != null && !name.isBlank()) {
                p.add(cb.like(cb.lower(root.<String>get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.from(games.findAll(spec, pageable), GameResponse::from);
    }

    @Transactional(readOnly = true)
    public List<GameResponse> activeGames() {
        return games.findByActiveTrueOrderByIdAsc().stream().map(GameResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<GameResponse> allGames() {
        return games.findAllByOrderByIdAsc().stream().map(GameResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public GameResponse get(Long id) {
        return GameResponse.from(find(id));
    }

    @Transactional
    public GameResponse create(GameRequest request) {
        checkLimits(request);
        if (games.existsByNameIgnoreCase(request.name())) {
            throw CasinoException.conflict("Гра з такою назвою вже існує");
        }
        Game game = new Game();
        apply(game, request);
        return GameResponse.from(games.save(game));
    }

    /**
     * Повна заміна всіх полів гри (PUT). Для зміни одного-двох полів краще PATCH.
     */
    @Transactional
    public GameResponse update(Long id, GameRequest request) {
        Game game = find(id);
        checkLimits(request);
        if (games.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw CasinoException.conflict("Гра з такою назвою вже існує");
        }
        apply(game, request);
        return GameResponse.from(game);
    }

    /**
     * JSON Merge Patch: передаєте тільки ті поля, які треба змінити, наприклад {"maxBet": 750}.
     * Поле зі значенням null означає "очистити", але для обов'язкових полів це дасть помилку валідації.
     */
    @Transactional
    public GameResponse mergePatch(Long id, Map<String, Object> patch) {
        Game game = find(id);
        Map<String, Object> current = toMap(game);
        for (Map.Entry<String, Object> e : patch.entrySet()) {
            requireKnownField(e.getKey());
            current.put(e.getKey(), e.getValue());
        }
        return update(id, fromMap(current));
    }

    /**
     * JSON Patch: список операцій, наприклад [{"op":"replace","path":"/active","value":false}].
     * Операція test дозволяє перевірити поточне значення перед зміною, якщо воно інше, повертається 409.
     */
    @Transactional
    public GameResponse jsonPatch(Long id, List<JsonPatchOperation> ops) {
        Game game = find(id);
        Map<String, Object> current = toMap(game);
        for (JsonPatchOperation op : ops) {
            String field = op.path().startsWith("/") ? op.path().substring(1) : op.path();
            requireKnownField(field);
            switch (op.op()) {
                case "add", "replace" -> current.put(field, op.value());
                case "remove" -> current.put(field, null);
                case "test" -> {
                    if (!sameValue(current.get(field), op.value())) {
                        throw CasinoException.conflict("Перевірка test не пройшла для " + op.path());
                    }
                }
                default -> throw CasinoException.badRequest("Непідтримувана операція: " + op.op());
            }
        }
        return update(id, fromMap(current));
    }

    /**
     * Видаляє гру. Якщо на неї вже були ставки, база не дасть видалити (зовнішній ключ),
     * тоді повертається 409 і гру краще просто вимкнути через active = false.
     */
    @Transactional
    public void delete(Long id) {
        Game game = find(id);
        if (bets.existsByGameId(id)) {
            throw CasinoException.conflict("На гру вже є ставки, її можна тільки вимкнути");
        }
        games.delete(game);
    }

    @Transactional
    public GameResponse toggleActive(Long id) {
        Game game = find(id);
        game.setActive(!game.isActive());
        return GameResponse.from(game);
    }

    Game find(Long id) {
        return games.findById(id).orElseThrow(() -> CasinoException.notFound("Гру не знайдено"));
    }

    private void checkLimits(GameRequest r) {
        Set<ConstraintViolation<GameRequest>> violations = validator.validate(r);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw CasinoException.badRequest(msg);
        }
        if (r.maxBet().compareTo(r.minBet()) < 0) {
            throw CasinoException.badRequest("maxBet не може бути менше за minBet");
        }
    }

    private void apply(Game game, GameRequest r) {
        game.setName(r.name().trim());
        game.setType(r.type());
        game.setDescription(r.description());
        game.setMinBet(r.minBet());
        game.setMaxBet(r.maxBet());
        game.setActive(r.active());
    }

    private boolean sameValue(Object actual, Object expected) {
        if (actual instanceof Number a && expected instanceof Number e) {
            return new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(e.toString())) == 0;
        }
        return String.valueOf(actual).equals(String.valueOf(expected));
    }

    private void requireKnownField(String field) {
        if (!FIELDS.contains(field)) {
            throw CasinoException.badRequest("Невідоме поле: " + field);
        }
    }

    private Map<String, Object> toMap(Game g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", g.getName());
        m.put("type", g.getType().name());
        m.put("description", g.getDescription());
        m.put("minBet", g.getMinBet());
        m.put("maxBet", g.getMaxBet());
        m.put("active", g.isActive());
        return m;
    }

    private GameRequest fromMap(Map<String, Object> m) {
        try {
            return json.convertValue(m, GameRequest.class);
        } catch (RuntimeException e) {
            throw CasinoException.badRequest("Некоректні дані у patch: " + e.getMessage());
        }
    }
}
