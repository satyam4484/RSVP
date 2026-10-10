package com.rsvp.common.util;

import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequests {

	public static final int MAX_SIZE = 100;

	private PageRequests() {
	}

	public static Pageable of(int page, int size, String sort, Set<String> allowed, String defaultSort) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? 20 : Math.min(size, MAX_SIZE);
		String requested = sort == null || sort.isBlank() ? defaultSort : sort;
		String[] parts = requested.split(",", 2);
		String field = parts[0].trim();
		if (!allowed.contains(field)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported sort field: " + field);
		}
		Sort.Direction direction = Sort.Direction.ASC;
		if (parts.length == 2 && "desc".equalsIgnoreCase(parts[1].trim())) {
			direction = Sort.Direction.DESC;
		}
		return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
	}
}
