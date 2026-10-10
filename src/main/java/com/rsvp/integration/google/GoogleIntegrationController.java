package com.rsvp.integration.google;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/google")
@RequiredArgsConstructor
@Tag(name = "Google integration")
public class GoogleIntegrationController {

	private final GoogleAccountService googleAccountService;

	@PostMapping("/connect")
	@Operation(summary = "Start Google OAuth for Calendar and Drive")
	@StandardApiErrors
	public ApiResponse<Map<String, String>> connect() {
		return ApiResponse.of(Map.of("authorizationUrl", googleAccountService.authorizationUrl(SecurityUtils.currentUserId())),
				"Open the authorization URL to connect Google");
	}

	@GetMapping("/callback")
	@SecurityRequirements
	@Operation(summary = "Google OAuth callback")
	@StandardApiErrors
	public ApiResponse<Void> callback(@RequestParam String code, @RequestParam String state) {
		googleAccountService.handleCallback(code, state);
		return ApiResponse.message("Google account connected");
	}

	@DeleteMapping
	@Operation(summary = "Disconnect Google and keep RSVP data")
	@StandardApiErrors
	public ApiResponse<Void> disconnect() {
		googleAccountService.disconnect(SecurityUtils.currentUserId());
		return ApiResponse.message("Google account disconnected");
	}
}
