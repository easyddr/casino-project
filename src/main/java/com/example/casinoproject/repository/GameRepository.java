package com.example.casinoproject.repository;

import com.example.casinoproject.domain.Game;
import com.example.casinoproject.domain.GameType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameRepository extends CrudRepository<Game, Long>,
        PagingAndSortingRepository<Game, Long>, JpaSpecificationExecutor<Game> {

    List<Game> findByActiveTrueOrderByIdAsc();

    List<Game> findAllByOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Активні ігри конкретного типу. Зручно, коли треба знайти, наприклад, усі слоти.
     */
    @Query("select g from Game g where g.type = :type and g.active = true order by g.id")
    List<Game> findActiveByType(@Param("type") GameType type);
}
