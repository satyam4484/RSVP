package com.rsvp.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RsvpFlowTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void registersLogsInAndKeepsEventsPrivateBetweenUsers() throws Exception {
		String ownerToken = tokenFor("owner@example.com", "Owner");
		String otherToken = tokenFor("other@example.com", "Other");
		MvcResult created = mockMvc.perform(post("/api/v1/events")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Wedding","startDateTime":"2026-12-01T04:00:00Z","endDateTime":"2026-12-01T12:00:00Z","timezone":"Asia/Kolkata"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("PLANNING"))
				.andReturn();
		String eventId = jsonMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asString();

		mockMvc.perform(get("/api/v1/events/" + eventId).header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch("/api/v1/events/" + eventId + "/status")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"PLANNING\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/events/" + eventId + "/status")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"COMPLETED\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/events/" + eventId + "/status")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"PLANNING\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/contacts")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"ABC Catering","phone":"09876543210","type":"VENDOR"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.normalizedPhone").value("+919876543210"));

		mockMvc.perform(post("/api/v1/contacts")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Duplicate","phone":"+91 98765 43210","type":"VENDOR"}
								"""))
				.andExpect(status().isConflict());
	}

	private String tokenFor(String email, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","email":"%s","password":"password123","timezone":"Asia/Kolkata","locale":"en-IN"}
								""".formatted(name, email)))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());
		assertThat(body.get("data").get("accessToken").asString()).isNotBlank();
		assertThat(body.get("data").get("user").has("passwordHash")).isFalse();
		return body.get("data").get("accessToken").asString();
	}
}
