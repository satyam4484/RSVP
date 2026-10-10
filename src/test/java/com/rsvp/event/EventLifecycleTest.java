package com.rsvp.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rsvp.common.enums.EventStatus;
import com.rsvp.common.exception.BusinessException;
import com.rsvp.event.service.EventLifecycle;
import org.junit.jupiter.api.Test;

class EventLifecycleTest {

	@Test
	void allowsTheForwardLifecycleAndCancellation() {
		assertThatCode(() -> EventLifecycle.assertTransition(EventStatus.PLANNING, EventStatus.CONFIRMED))
				.doesNotThrowAnyException();
		assertThatCode(() -> EventLifecycle.assertTransition(EventStatus.CONFIRMED, EventStatus.IN_PROGRESS))
				.doesNotThrowAnyException();
		assertThatCode(() -> EventLifecycle.assertTransition(EventStatus.IN_PROGRESS, EventStatus.COMPLETED))
				.doesNotThrowAnyException();
		assertThatCode(() -> EventLifecycle.assertTransition(EventStatus.COMPLETED, EventStatus.ARCHIVED))
				.doesNotThrowAnyException();
		assertThatCode(() -> EventLifecycle.assertTransition(EventStatus.PLANNING, EventStatus.CANCELLED))
				.doesNotThrowAnyException();
		assertThatCode(() -> EventLifecycle.assertTransition(EventStatus.CONFIRMED, EventStatus.CANCELLED))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsReopeningACompletedEvent() {
		assertThatThrownBy(() -> EventLifecycle.assertTransition(EventStatus.COMPLETED, EventStatus.PLANNING))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void autoCompletesOnlyConfirmedAndInProgressEvents() {
		org.assertj.core.api.Assertions.assertThat(EventLifecycle.autoCompletable(EventStatus.CONFIRMED)).isTrue();
		org.assertj.core.api.Assertions.assertThat(EventLifecycle.autoCompletable(EventStatus.IN_PROGRESS)).isTrue();
		org.assertj.core.api.Assertions.assertThat(EventLifecycle.autoCompletable(EventStatus.PLANNING)).isFalse();
		org.assertj.core.api.Assertions.assertThat(EventLifecycle.autoCompletable(EventStatus.CANCELLED)).isFalse();
	}
}
