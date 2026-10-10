package com.rsvp.calendar.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class IcsBuilder {

	private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

	private IcsBuilder() {
	}

	public static String calendar(List<Component> components) {
		StringBuilder builder = new StringBuilder();
		builder.append("BEGIN:VCALENDAR\r\n");
		builder.append("VERSION:2.0\r\n");
		builder.append("PRODID:-//RSVP//EN\r\n");
		builder.append("CALSCALE:GREGORIAN\r\n");
		builder.append("METHOD:PUBLISH\r\n");
		for (Component component : components) {
			builder.append("BEGIN:VEVENT\r\n");
			line(builder, "UID", component.uid());
			line(builder, "DTSTAMP", UTC.format(Instant.now()));
			line(builder, "DTSTART", UTC.format(component.start()));
			line(builder, "DTEND", UTC.format(component.end()));
			line(builder, "SUMMARY", component.summary());
			line(builder, "DESCRIPTION", component.description());
			line(builder, "LOCATION", component.location());
			line(builder, "URL", component.url());
			line(builder, "STATUS", component.status());
			builder.append("END:VEVENT\r\n");
		}
		builder.append("END:VCALENDAR\r\n");
		return builder.toString();
	}

	private static void line(StringBuilder builder, String name, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		builder.append(name).append(':').append(escape(value)).append("\r\n");
	}

	static String escape(String value) {
		return value.replace("\\", "\\\\")
				.replace("\r\n", "\\n")
				.replace("\n", "\\n")
				.replace(",", "\\,")
				.replace(";", "\\;");
	}

	public record Component(String uid, Instant start, Instant end, String summary, String description, String location,
			String url, String status) {
	}
}
