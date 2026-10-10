package com.rsvp.task.service;

import com.rsvp.common.enums.ReminderChannel;
import com.rsvp.common.enums.ReminderStatus;
import com.rsvp.common.enums.SyncAction;
import com.rsvp.common.enums.SyncSubject;
import com.rsvp.common.enums.TaskPriority;
import com.rsvp.common.enums.TaskStatus;
import com.rsvp.common.event.CalendarSyncRequested;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.common.exception.ErrorCode;
import com.rsvp.common.exception.ResourceNotFoundException;
import com.rsvp.common.util.PageRequests;
import com.rsvp.contact.entity.Contact;
import com.rsvp.contact.service.ContactService;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.task.dto.TaskRequest;
import com.rsvp.task.dto.TaskResponse;
import com.rsvp.task.entity.Reminder;
import com.rsvp.task.entity.Task;
import com.rsvp.task.repository.ReminderRepository;
import com.rsvp.task.repository.TaskRepository;
import com.rsvp.user.entity.User;
import com.rsvp.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

	private static final Set<String> SORTS = Set.of("dueAt", "createdAt", "priority", "status", "title");

	private final TaskRepository taskRepository;
	private final ReminderRepository reminderRepository;
	private final EventService eventService;
	private final ContactService contactService;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	@Transactional
	public TaskResponse create(Long userId, UUID eventId, TaskRequest request) {
		Event event = eventService.require(userId, eventId);
		Task task = new Task();
		task.setEvent(event);
		task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
		task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
		apply(userId, task, request);
		if (task.getStatus() == TaskStatus.COMPLETED) {
			task.setCompletedAt(clock.instant());
		}
		Task saved = taskRepository.save(task);
		syncReminder(saved, request.reminderChannel());
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.TASK, saved.getId(), SyncAction.UPSERT));
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public Page<TaskResponse> list(Long userId, UUID eventId, TaskStatus status, TaskPriority priority, Instant dueFrom,
			Instant dueTo, UUID contactId, int page, int size, String sort) {
		Event event = eventService.require(userId, eventId);
		Long contact = contactId == null ? null : contactService.require(userId, contactId).getId();
		Pageable pageable = PageRequests.of(page, size, sort, SORTS, "dueAt,asc");
		return taskRepository.filter(event.getId(), status, priority, dueFrom, dueTo, contact, pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public TaskResponse get(Long userId, UUID taskId) {
		return toResponse(require(userId, taskId));
	}

	@Transactional
	public TaskResponse update(Long userId, UUID taskId, TaskRequest request) {
		Task task = require(userId, taskId);
		apply(userId, task, request);
		if (request.status() != null) {
			task.setStatus(request.status());
			if (request.status() == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
				task.setCompletedAt(clock.instant());
			}
			if (request.status() != TaskStatus.COMPLETED) {
				task.setCompletedAt(null);
			}
		}
		syncReminder(task, request.reminderChannel());
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.TASK, task.getId(), SyncAction.UPSERT));
		return toResponse(task);
	}

	@Transactional
	public TaskResponse complete(Long userId, UUID taskId) {
		Task task = require(userId, taskId);
		task.setStatus(TaskStatus.COMPLETED);
		task.setCompletedAt(clock.instant());
		syncReminder(task, null);
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.TASK, task.getId(), SyncAction.UPSERT));
		return toResponse(task);
	}

	@Transactional
	public void delete(Long userId, UUID taskId) {
		Task task = require(userId, taskId);
		task.markDeleted(clock.instant());
		syncReminder(task, null);
		eventPublisher.publishEvent(new CalendarSyncRequested(SyncSubject.TASK, task.getId(), SyncAction.DELETE));
	}

	@Transactional(readOnly = true)
	public Task require(Long userId, UUID taskId) {
		return taskRepository.findByUuidAndEvent_User_IdAndDeletedFalse(taskId, userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TASK_NOT_FOUND, "Task not found"));
	}

	public TaskResponse toResponse(Task task) {
		return new TaskResponse(task.getUuid(), task.getEvent().getUuid(), task.getTitle(), task.getDescription(),
				task.getStatus(), task.getPriority(), task.getDueAt(), task.getCompletedAt(),
				task.getAssignedTo() == null ? null : task.getAssignedTo().getUuid(),
				task.getContact() == null ? null : task.getContact().getUuid(), task.isReminderEnabled(), task.getReminderAt(),
				task.getCreatedAt(), task.getUpdatedAt());
	}

	private void apply(Long userId, Task task, TaskRequest request) {
		task.setTitle(request.title().trim());
		task.setDescription(request.description());
		if (request.priority() != null) {
			task.setPriority(request.priority());
		}
		task.setDueAt(request.dueAt());
		task.setReminderEnabled(Boolean.TRUE.equals(request.reminderEnabled()));
		task.setReminderAt(request.reminderAt());
		if (task.isReminderEnabled() && task.getReminderAt() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "reminderAt is required when reminders are enabled");
		}
		if (request.reminderChannel() != null && request.reminderChannel() != ReminderChannel.IN_APP) {
			throw new BusinessException(ErrorCode.CHANNEL_NOT_ENABLED, "Only IN_APP reminders are enabled");
		}
		if (request.assignedToUserId() == null) {
			task.setAssignedTo(null);
		} else {
			User assignee = userRepository.findByUuidAndDeletedFalse(request.assignedToUserId())
					.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "Assignee not found"));
			task.setAssignedTo(assignee);
		}
		if (request.contactId() == null) {
			task.setContact(null);
		} else {
			Contact contact = contactService.require(userId, request.contactId());
			task.setContact(contact);
		}
	}

	private void syncReminder(Task task, ReminderChannel channel) {
		Reminder pending = reminderRepository.findByTask_IdAndStatus(task.getId(), ReminderStatus.PENDING).orElse(null);
		boolean active = task.isReminderEnabled() && task.getReminderAt() != null && !task.isDeleted()
				&& task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED;
		if (!active) {
			if (pending != null) {
				pending.setStatus(ReminderStatus.CANCELLED);
			}
			return;
		}
		if (pending == null) {
			pending = new Reminder();
			pending.setTask(task);
			pending.setEvent(task.getEvent());
			pending.setStatus(ReminderStatus.PENDING);
			pending.setChannel(channel == null ? ReminderChannel.IN_APP : channel);
		}
		pending.setReminderAt(task.getReminderAt());
		if (channel != null) {
			pending.setChannel(channel);
		}
		reminderRepository.save(pending);
	}
}
