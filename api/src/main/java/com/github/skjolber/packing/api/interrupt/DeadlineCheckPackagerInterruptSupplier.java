package com.github.skjolber.packing.api.interrupt;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;

public class DeadlineCheckPackagerInterruptSupplier implements PackagerInterruptSupplier, Runnable, Closeable {

	protected volatile boolean expired = false;
	protected ScheduledFuture<?> future;
	
	public DeadlineCheckPackagerInterruptSupplier() {
	}

	@Override
	public boolean getAsBoolean() {
		return expired;
	}

	@Override
	public void run() {
		this.expired = true;
	}
	
	public void close() {
		future.cancel(true);
	}
	
	public void setFuture(ScheduledFuture<?> future) {
		this.future = future;
	}

}
