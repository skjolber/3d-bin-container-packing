package com.github.skjolber.packing.packer.bruteforce;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements ThreadFactory {

	private final ThreadGroup group;
	private final int priority;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private static final String NAME_PREFIX = "3d-packaging-thread-";

	/**
	 * Create threads with normal priority.
	 */

	public DefaultThreadFactory() {
		this(Thread.NORM_PRIORITY);
	}

	/**
	 * Create threads with a priority. The priority is a hint to the scheduler, and clamped by the thread group's maximum.
	 *
	 * @param priority thread priority, from {@linkplain Thread#MIN_PRIORITY} to {@linkplain Thread#MAX_PRIORITY}
	 */

	public DefaultThreadFactory(int priority) {
		if(priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
			throw new IllegalArgumentException("Unexpected thread priority " + priority);
		}
		this.group = Thread.currentThread().getThreadGroup();
		this.priority = priority;
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, NAME_PREFIX + threadNumber.getAndIncrement(), 0);
		if(t.isDaemon()) {
			t.setDaemon(false);
		}
		if(t.getPriority() != priority) {
			t.setPriority(priority);
		}
		return t;
	}
}