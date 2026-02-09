package com.retrip.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "travel_styles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class TravelStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer displayOrder;

    public static TravelStyle of(String name, Integer displayOrder) {
        return TravelStyle.builder()
                .name(name)
                .displayOrder(displayOrder)
                .build();
    }
}
