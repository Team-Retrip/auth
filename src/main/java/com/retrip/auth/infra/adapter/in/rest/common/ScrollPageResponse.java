package com.retrip.auth.infra.adapter.in.rest.common;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScrollPageResponse<T> {
    private Long totalCount;
    private boolean hasNext;
    private List<T> list;

    public static <T> ScrollPageResponse<T> of(Long totalCount, boolean hasNext, List<T> list) {
        return new ScrollPageResponse<>(totalCount, hasNext, list);
    }
}
