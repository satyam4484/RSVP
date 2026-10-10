package com.rsvp.common.util;

public final class SearchText {

	private SearchText() {
	}

	public static String like(String raw) {
		String escaped = raw.trim()
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped.toLowerCase() + "%";
	}
}
