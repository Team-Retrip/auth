package com.retrip.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "member_travel_styles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class MemberTravelStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", columnDefinition = "varbinary(16)", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_style_id", nullable = false)
    private TravelStyle travelStyle;

    public static MemberTravelStyle of(Member member, TravelStyle travelStyle) {
        return MemberTravelStyle.builder()
                .member(member)
                .travelStyle(travelStyle)
                .build();
    }
}
