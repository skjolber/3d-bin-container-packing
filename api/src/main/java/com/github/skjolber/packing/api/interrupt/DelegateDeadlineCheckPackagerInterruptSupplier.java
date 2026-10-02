package com.github.skjolber.packing.api.interrupt;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;

public class DelegateDeadlineCheckPackagerInterruptSupplier implements PackagerInterruptSupplier, Runnable, Closeable {

	protected volatile boolean expired = false;
	protected ScheduledFuture<?> future;
	protected final PackagerInterruptSupplier delegate;
	
	public DelegateDeadlineCheckPackagerInterruptSupplier(PackagerInterruptSupplier delegate) {
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
		delegate.close();
	}
	
	public void setFuture(ScheduledFuture<?> future) {
		this.future = future;
	}

}
