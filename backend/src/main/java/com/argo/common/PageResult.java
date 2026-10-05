package com.argo.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

	public static <S, T> PageResult<T> of(Page<S> page, Function<S, T> mapper) {
		return new PageResult<>(page.getContent().stream().map(mapper).toList(),
				page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
