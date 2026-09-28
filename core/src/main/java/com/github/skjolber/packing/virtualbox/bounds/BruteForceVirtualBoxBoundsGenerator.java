package com.github.skjolber.packing.virtualbox.bounds;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Standalone geometric search for compact assemblies of all supplied boxes.
 * Searches permutations, rotations and extreme-point placements, with safe
 * objective pruning, until exhausted, a goal is reached or interrupted.
 *
 * <p>Reusable across operations, including concurrent operations with separate
 * result builders. Close after all operations finish to release the deadline
 * scheduler. This component does not create or configure a packager.</p>
 */
public class BruteForceVirtualBoxBoundsGenerator implements AutoCloseable {

	protected final ScheduledThreadPoolExecutor scheduler;

	public BruteForceVirtualBoxBoundsGenerator() {
		scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.setRemoveOnCancelPolicy(true);
	}

	/** @return an independent builder for one bounding-box search */
	public BruteForceVirtualBoxBoundsResultBuilder newResultBuilder() {
		if(scheduler.isShutdown()) {
			throw new IllegalStateException("Bounding-box search is closed");
		}
		return new BruteForceVirtualBoxBoundsResultBuilder(scheduler, supportsLoad());
	}

	protected boolean supportsLoad() {
		return false;
	}

	/** Release the scheduler after the active operations have finished. */
	@Override
	public void close() {
		scheduler.shutdownNow();
	}
}
