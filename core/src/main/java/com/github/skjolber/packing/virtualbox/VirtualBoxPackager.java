package com.github.skjolber.packing.virtualbox;

import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResultBuilder;

/**
 * Rectangular assembly preprocessing around any Packager. The delegate remains caller-owned:
 * closing this wrapper does not close it. Configure delegate-specific options on the
 * delegate before wrapping it. Inventory-dependent custom controls must not assume
 * that delegate input boxes are the original boxes.
 *
 * Groups, ordered inputs, controlled containers and existing
 * placements bypass aggregation and are handed directly to the delegate.
 * Load-constrained assemblies are checked internally and again after expansion.
 * Invalid expanded results trigger refinement or fallback, never an unchecked success.
 */
public class VirtualBoxPackager implements Packager<VirtualBoxPackagerResultBuilder> {
	protected final Packager<? extends PackagerResultBuilder> delegate;
	protected final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
	protected boolean closed;

	public VirtualBoxPackager(Packager<? extends PackagerResultBuilder> delegate) {
		this.delegate = Objects.requireNonNull(delegate);
		scheduler.setRemoveOnCancelPolicy(true);
	}

	@Override
	public synchronized VirtualBoxPackagerResultBuilder newResultBuilder() {
		if(closed) {
			throw new IllegalStateException("Virtual box packager is closed");
		}
		return new VirtualBoxPackagerResultBuilder(delegate, scheduler);
	}

	/** Release the deadline scheduler after active operations finish; leave the delegate open. */
	@Override
	public synchronized void close() {
		closed = true;
		scheduler.shutdownNow();
	}
}
