package com.rsvp.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsvp.common.enums.TaskPriority;
import com.rsvp.common.enums.TaskStatus;
import com.rsvp.event.entity.Event;
import com.rsvp.event.service.EventService;
import com.rsvp.task.dto.TaskRequest;
import com.rsvp.task.entity.Task;
import com.rsvp.task.repository.ReminderRepository;
import com.rsvp.task.repository.TaskRepository;
import com.rsvp.task.service.TaskService;
import com.rsvp.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;
	@Mock
	private ReminderRepository reminderRepository;
	@Mock
	private EventService eventService;
	@Mock
	private com.rsvp.contact.service.ContactService contactService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private ApplicationEventPublisher publisher;

	private TaskService taskService;

	@BeforeEach
	void setUp() {
		taskService = new TaskService(taskRepository, reminderRepository, eventService, contactService, userRepository, publisher,
				Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void completesATask() {
		Event event = new Event();
		event.setId(1L);
		event.setUuid(UUID.randomUUID());
		Task task = new Task();
		task.setId(2L);
		task.setUuid(UUID.randomUUID());
		task.setEvent(event);
		task.setTitle("Call caterer");
		task.setStatus(TaskStatus.TODO);
		task.setPriority(TaskPriority.HIGH);
		when(taskRepository.findByUuidAndEvent_User_IdAndDeletedFalse(task.getUuid(), 1L)).thenReturn(Optional.of(task));
		when(reminderRepository.findByTask_IdAndStatus(task.getId(), com.rsvp.common.enums.ReminderStatus.PENDING))
				.thenReturn(Optional.empty());
		var completed = taskService.complete(1L, task.getUuid());
		assertThat(completed.status()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(completed.completedAt()).isEqualTo(Instant.parse("2026-10-18T12:00:00Z"));
		verify(publisher).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
	}

	@Test
	void createsATodoWithDefaultPriority() {
		Event event = new Event();
		event.setId(1L);
		event.setUuid(UUID.randomUUID());
		when(eventService.require(1L, event.getUuid())).thenReturn(event);
		when(taskRepository.save(org.mockito.ArgumentMatchers.any(Task.class))).thenAnswer(invocation -> {
			Task task = invocation.getArgument(0);
			task.setId(5L);
			task.setUuid(UUID.randomUUID());
			return task;
		});
		when(reminderRepository.findByTask_IdAndStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.empty());
		var created = taskService.create(1L, event.getUuid(),
				new TaskRequest("Confirm hotel", null, null, null, null, null, null, false, null, null));
		assertThat(created.status()).isEqualTo(TaskStatus.TODO);
		assertThat(created.priority()).isEqualTo(TaskPriority.MEDIUM);
	}
}
