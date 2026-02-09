package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.MemberTravelStyle;
import com.retrip.auth.domain.entity.TravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberTravelStyleRepository extends JpaRepository<MemberTravelStyle, Long> {
    List<MemberTravelStyle> findByMemberId(UUID memberId);
    void deleteByMemberIdAndTravelStyleId(UUID memberId, Long travelStyleId);
    void deleteByMemberId(UUID memberId);
    boolean existsByMemberAndTravelStyle(Member member, TravelStyle travelStyle);
}
