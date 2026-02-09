package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.TravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelStyleRepository extends JpaRepository<TravelStyle, Long> {
    Optional<TravelStyle> findByName(String name);
    boolean existsByName(String name);
}
