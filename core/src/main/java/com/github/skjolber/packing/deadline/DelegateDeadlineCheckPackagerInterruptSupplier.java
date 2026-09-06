package com.github.skjolber.packing.deadline;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BooleanSupplier;

public class DelegateDeadlineCheckPackagerInterruptSupplier implements PackagerInterruptSupplier, Runnable, Closeable {

	protected volatile boolean expired = false;
	protected ScheduledFuture<?> future;
	protected final BooleanSupplier delegate;
	
	public DelegateDeadlineCheckPackagerInterruptSupplier(BooleanSupplier delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public boolean getAsBoolean() {
		return expired || delegate.getAsBoolean();
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
