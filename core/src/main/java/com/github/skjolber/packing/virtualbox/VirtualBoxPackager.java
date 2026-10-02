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
 * Without physical load constraints, each filled rectangular layout is packed as
 * one ordinary envelope: one inventory item and one point-calculator insertion.
 * Physical children are expanded only after a successful delegate attempt. No
 * batch insertion, worker child placements or load-contact metadata is needed on
 * this path. Controls requiring original child surfaces must disable aggregation
 * with {@link VirtualBoxPackagerResultBuilder#withAggregation(boolean)}; a filled
 * envelope alone does not preserve those control semantics.
 *
 * Groups, ordered inputs, controlled containers and existing
 * placements bypass aggregation and are handed directly to the delegate.
 * Any box load constraint also bypasses aggregation for the entire operation.
 * Use a load-aware delegate to enforce these constraints on the original boxes.
 * This wrapper does not validate loads or rebuild physical support graphs.
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
