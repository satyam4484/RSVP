package com.rsvp.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(List<T> data, PageMeta pagination) {

	public static <T> PagedResponse<T> from(Page<T> page) {
		return new PagedResponse<>(
				page.getContent(),
				new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
	}
}
