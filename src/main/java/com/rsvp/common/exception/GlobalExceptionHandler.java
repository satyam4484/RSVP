package com.rsvp.common.exception;

import com.rsvp.common.response.ApiErrorResponse;
import com.rsvp.common.response.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
		return error(ex.getErrorCode().status(), ex.getErrorCode().name(), ex.getMessage(), request, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(field -> new FieldErrorDetail(field.getField(), field.getDefaultMessage()))
				.toList();
		return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(), "Validation failed", request, errors);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MaxUploadSizeExceededException.class})
	public ResponseEntity<ApiErrorResponse> handleUnreadable(Exception ex, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(), "Request could not be read", request,
				null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.name(), "Invalid email or password", request, null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name(), "Access denied", request, null);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("operation=data-integrity path={} success=false", request.getRequestURI());
		return error(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_ASSOCIATION.name(), "The request conflicts with existing data",
				request, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("operation=unhandled path={} success=false", request.getRequestURI(), ex);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.name(), "Unexpected error", request, null);
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message,
			HttpServletRequest request, List<FieldErrorDetail> errors) {
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(), code, message,
				request.getRequestURI(), errors);
		return ResponseEntity.status(status).body(body);
	}
}
