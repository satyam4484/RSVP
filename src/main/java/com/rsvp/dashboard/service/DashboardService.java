package com.rsvp.dashboard.service;

import com.rsvp.calendar.repository.CalendarConnectionRepository;
import com.rsvp.calendar.repository.CalendarEventMappingRepository;
import com.rsvp.common.enums.AttendanceStatus;
import com.rsvp.common.enums.ContactType;
import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.enums.PaymentStatus;
import com.rsvp.common.enums.SyncStatus;
import com.rsvp.common.enums.TaskStatus;
import com.rsvp.common.util.Timezones;
import com.rsvp.contact.repository.EventContactRepository;
import com.rsvp.contact.repository.EventParticipantRepository;
import com.rsvp.dashboard.dto.DashboardResponse;
import com.rsvp.dashboard.dto.EventSummaryResponse;
import com.rsvp.dashboard.dto.TimelineItem;
import com.rsvp.document.repository.DocumentRepository;
import com.rsvp.event.entity.Event;
import com.rsvp.event.repository.EventRepository;
import com.rsvp.event.service.EventService;
import com.rsvp.expense.entity.Expense;
import com.rsvp.expense.entity.Payment;
import com.rsvp.expense.repository.ExpenseRepository;
import com.rsvp.expense.repository.PaymentRepository;
import com.rsvp.expense.service.ExpenseService;
import com.rsvp.task.repository.ReminderRepository;
import com.rsvp.task.repository.TaskRepository;
import com.rsvp.task.service.TaskService;
import com.rsvp.travel.repository.TravelRepository;
import com.rsvp.travel.service.TravelService;
import com.rsvp.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private final UserService userService;
	private final EventService eventService;
	private final EventRepository eventRepository;
	private final TaskService taskService;
	private final TaskRepository taskRepository;
	private final TravelService travelService;
	private final TravelRepository travelRepository;
	private final ExpenseService expenseService;
	private final ExpenseRepository expenseRepository;
	private final PaymentRepository paymentRepository;
	private final EventParticipantRepository participantRepository;
	private final EventContactRepository eventContactRepository;
	private final DocumentRepository documentRepository;
	private final ReminderRepository reminderRepository;
	private final CalendarConnectionRepository calendarConnectionRepository;
	private final CalendarEventMappingRepository mappingRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public DashboardResponse dashboard(Long userId) {
		var user = userService.requireActive(userId);
		ZoneId zone = Timezones.require(user.getTimezone());
		Instant now = clock.instant();
		Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
		Instant endOfDay = startOfDay.plus(java.time.Duration.ofDays(1));
		var upcoming = eventRepository
				.findTop10ByUser_IdAndDeletedFalseAndStatusInAndStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(userId,
						List.of(EventStatus.PLANNING, EventStatus.CONFIRMED, EventStatus.IN_PROGRESS), now)
				.stream().map(eventService::toResponse).toList();
		var today = taskRepository.findDueBetween(userId, startOfDay, endOfDay).stream().map(taskService::toResponse).toList();
		var pending = taskRepository.findPending(userId, PageRequest.of(0, 10)).stream().map(taskService::toResponse).toList();
		var travel = travelRepository.findUpcoming(userId, now, PageRequest.of(0, 10)).stream().map(travelService::toResponse).toList();
		var payments = expenseRepository
				.findByUserAndStatuses(userId, List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID), PageRequest.of(0, 10))
				.stream().map(expense -> expenseService.get(userId, expense.getUuid())).toList();
		var recent = eventRepository.findTop10ByUser_IdAndDeletedFalseOrderByUpdatedAtDesc(userId).stream()
				.map(eventService::toResponse).toList();
		return new DashboardResponse(upcoming, today, pending, travel, payments, recent, expenseService.summarizeUser(userId));
	}

	@Transactional(readOnly = true)
	public EventSummaryResponse summary(Long userId, UUID eventId) {
		Event event = eventService.require(userId, eventId);
		var totals = expenseService.summarizeEvent(userId, eventId);
		return new EventSummaryResponse(eventService.toResponse(event), participantRepository.countByEvent_Id(event.getId()),
				participantRepository.countByEvent_IdAndAttendanceStatusIn(event.getId(),
						List.of(AttendanceStatus.CONFIRMED, AttendanceStatus.ATTENDED)),
				eventContactRepository.countByEventAndType(event.getId(), ContactType.VENDOR),
				travelRepository.countByEvent_IdAndDeletedFalse(event.getId()),
				taskRepository.countByEvent_IdAndDeletedFalse(event.getId()),
				taskRepository.countByEvent_IdAndDeletedFalseAndStatus(event.getId(), TaskStatus.COMPLETED),
				totals.totalExpense(), totals.totalPaid(), totals.totalPending(), totals.currency(),
				documentRepository.countByEvent_IdAndDeletedFalse(event.getId()), calendarStatus(userId, event.getId()));
	}

	@Transactional(readOnly = true)
	public List<TimelineItem> timeline(Long userId, UUID eventId) {
		Event event = eventService.require(userId, eventId);
		ZoneId zone = Timezones.require(event.getTimezone());
		List<TimelineItem> items = new ArrayList<>();
		items.add(new TimelineItem("EVENT", event.getStartDateTime(), event.getName(), null, event.getUuid()));
		travelRepository.findByEvent_IdAndDeletedFalse(event.getId()).forEach(travel -> {
			if (travel.getDepartureAt() != null) {
				String title = travel.getFromLocation() != null && travel.getToLocation() != null
						? travel.getFromLocation() + " → " + travel.getToLocation()
						: travel.getTitle();
				items.add(new TimelineItem("TRAVEL", travel.getDepartureAt(), title, null, travel.getUuid()));
			}
		});
		taskRepository.findByEvent_IdAndDeletedFalse(event.getId()).forEach(task -> {
			if (task.getDueAt() != null) {
				items.add(new TimelineItem("TASK", task.getDueAt(), task.getTitle(), null, task.getUuid()));
			}
		});
		List<Expense> expenses = expenseRepository.findByEvent_IdAndDeletedFalse(event.getId());
		for (Expense expense : expenses) {
			items.add(new TimelineItem("EXPENSE", startOfDay(expense.getExpenseDate(), zone), expense.getTitle(), expense.getAmount(),
					expense.getUuid()));
			for (Payment payment : paymentRepository.findByExpense_IdAndDeletedFalseOrderByPaymentDateAsc(expense.getId())) {
				items.add(new TimelineItem("PAYMENT", startOfDay(payment.getPaymentDate(), zone), expense.getTitle(),
						payment.getAmount(), payment.getUuid()));
			}
		}
		reminderRepository.findByEvent_Id(event.getId()).forEach(reminder -> items.add(new TimelineItem("REMINDER",
				reminder.getReminderAt(), reminder.getTask().getTitle(), null, reminder.getUuid())));
		items.sort(Comparator.comparing(TimelineItem::dateTime, Comparator.nullsLast(Comparator.naturalOrder())));
		return items;
	}

	private SyncStatus calendarStatus(Long userId, Long eventId) {
		if (calendarConnectionRepository.findByUser_IdAndEnabledTrue(userId).isEmpty()) {
			return SyncStatus.NOT_CONNECTED;
		}
		var mappings = mappingRepository.findByEvent_Id(eventId);
		if (mappings.isEmpty()) {
			return SyncStatus.PENDING;
		}
		if (mappings.stream().anyMatch(mapping -> mapping.getSyncStatus() == SyncStatus.FAILED)) {
			return SyncStatus.FAILED;
		}
		if (mappings.stream().anyMatch(mapping -> mapping.getSyncStatus() == SyncStatus.PENDING)) {
			return SyncStatus.PENDING;
		}
		return SyncStatus.SYNCED;
	}

	private Instant startOfDay(java.time.LocalDate date, ZoneId zone) {
		return date.atStartOfDay(zone).toInstant();
	}
}
