package com.github.skjolber.packing.api.interrupt;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;

public class DelegateDeadlineCheckPackagerInterruptSupplier implements PackagerInterruptSupplier, Runnable, Closeable {

	// this is not entirely accurate for multi-threading, but close enough
	// (should have been volatile)
	protected boolean expired = false;
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
