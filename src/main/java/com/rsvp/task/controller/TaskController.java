package com.rsvp.task.controller;

import com.rsvp.common.config.StandardApiErrors;
import com.rsvp.common.enums.TaskPriority;
import com.rsvp.common.enums.TaskStatus;
import com.rsvp.common.response.ApiResponse;
import com.rsvp.common.response.PagedResponse;
import com.rsvp.common.security.SecurityUtils;
import com.rsvp.task.dto.TaskRequest;
import com.rsvp.task.dto.TaskResponse;
import com.rsvp.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tasks")
public class TaskController {

	private final TaskService taskService;

	@PostMapping("/api/v1/events/{eventId}/tasks")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a task")
	@StandardApiErrors
	public ApiResponse<TaskResponse> create(@PathVariable UUID eventId, @Valid @RequestBody TaskRequest request) {
		return ApiResponse.of(taskService.create(SecurityUtils.currentUserId(), eventId, request), "Task created");
	}

	@GetMapping("/api/v1/events/{eventId}/tasks")
	@Operation(summary = "List tasks for an event")
	@StandardApiErrors
	public PagedResponse<TaskResponse> list(
			@PathVariable UUID eventId,
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) TaskPriority priority,
			@RequestParam(required = false) Instant dueFrom,
			@RequestParam(required = false) Instant dueTo,
			@RequestParam(required = false) UUID contactId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "dueAt,asc") String sort) {
		return PagedResponse.from(taskService.list(SecurityUtils.currentUserId(), eventId, status, priority, dueFrom, dueTo,
				contactId, page, size, sort));
	}

	@GetMapping("/api/v1/tasks/{taskId}")
	@Operation(summary = "Get a task")
	@StandardApiErrors
	public ApiResponse<TaskResponse> get(@PathVariable UUID taskId) {
		return ApiResponse.of(taskService.get(SecurityUtils.currentUserId(), taskId), "Task");
	}

	@PutMapping("/api/v1/tasks/{taskId}")
	@Operation(summary = "Update a task")
	@StandardApiErrors
	public ApiResponse<TaskResponse> update(@PathVariable UUID taskId, @Valid @RequestBody TaskRequest request) {
		return ApiResponse.of(taskService.update(SecurityUtils.currentUserId(), taskId, request), "Task updated");
	}

	@PatchMapping("/api/v1/tasks/{taskId}/complete")
	@Operation(summary = "Mark a task complete")
	@StandardApiErrors
	public ApiResponse<TaskResponse> complete(@PathVariable UUID taskId) {
		return ApiResponse.of(taskService.complete(SecurityUtils.currentUserId(), taskId), "Task completed");
	}

	@DeleteMapping("/api/v1/tasks/{taskId}")
	@Operation(summary = "Soft-delete a task")
	@StandardApiErrors
	public ApiResponse<Void> delete(@PathVariable UUID taskId) {
		taskService.delete(SecurityUtils.currentUserId(), taskId);
		return ApiResponse.message("Task deleted");
	}
}
