package com.rsvp.common.scheduling;

import java.time.Duration;

public interface JobLock {

	boolean tryLock(String jobName, Duration lease);

	void unlock(String jobName);
}
