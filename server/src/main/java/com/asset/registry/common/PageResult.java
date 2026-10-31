package com.asset.registry.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应结构。
 */
public record PageResult<T>(Long total, Integer page, Integer size, Integer pages, List<T> list) {

    public static <T> PageResult<T> of(Page<T> p, int pageNo, int pageSize) {
        return new PageResult<>(p.getTotalElements(), pageNo, pageSize, p.getTotalPages(), p.getContent());
    }

    public static <T> PageResult<T> of(List<T> list, long total, int pageNo, int pageSize) {
        int pages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(total, pageNo, pageSize, pages, list);
    }
}
