package com.rsvp.integration.google;

public class GoogleResourceNotFoundException extends RuntimeException {

	public GoogleResourceNotFoundException() {
		super("Google resource was not found");
	}
}
