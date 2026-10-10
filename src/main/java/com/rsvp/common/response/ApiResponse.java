package com.rsvp.common.response;

public record ApiResponse<T>(T data, String message) {

	public static <T> ApiResponse<T> of(T data, String message) {
		return new ApiResponse<>(data, message);
	}

	public static ApiResponse<Void> message(String message) {
		return new ApiResponse<>(null, message);
	}
}
