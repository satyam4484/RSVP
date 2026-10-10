package com.rsvp.common.config;

import com.rsvp.common.response.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "Validation or business rule error",
				content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required",
				content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
		@ApiResponse(responseCode = "403", description = "Forbidden",
				content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
		@ApiResponse(responseCode = "404", description = "Resource not found",
				content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
		@ApiResponse(responseCode = "409", description = "Conflict",
				content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public @interface StandardApiErrors {
}
