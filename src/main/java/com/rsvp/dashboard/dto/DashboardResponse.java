package com.rsvp.dashboard.dto;

import com.rsvp.event.dto.EventResponse;
import com.rsvp.expense.dto.ExpenseResponse;
import com.rsvp.expense.dto.ExpenseSummaryResponse;
import com.rsvp.task.dto.TaskResponse;
import com.rsvp.travel.dto.TravelResponse;
import java.util.List;

public record DashboardResponse(
		List<EventResponse> upcomingEvents,
		List<TaskResponse> todayTasks,
		List<TaskResponse> pendingTasks,
		List<TravelResponse> upcomingTravel,
		List<ExpenseResponse> pendingPayments,
		List<EventResponse> recentEvents,
		ExpenseSummaryResponse expenseSummary) {
}
