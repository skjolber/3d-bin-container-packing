package com.github.skjolber.packing;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Brute force packager builder. 
 * 
 * Performance note: Setting the number of checkpoints per deadline to any value except 1 and Integer.MAX_VALUE
 * check seems to slow down parallel packaging (instead of speeding it up).
 * 
 */

public class BruteForcePackagerBuilder {

	protected int threads = 1;
	
	protected List<Container> containers;
	protected boolean rotate3D = true;
	protected boolean binarySearch = true;
	protected ExecutorService executorService;
	protected int checkpointsPerDeadlineCheck = 1;

	public BruteForcePackagerBuilder withThreads(int threads) {
		if(threads <= 1) {
			throw new IllegalArgumentException("Unexpected thread count " + threads);
		}
		this.threads = threads;
		return this;
	}

	public BruteForcePackagerBuilder withContainers(List<Container> containers) {
		this.containers = containers;
		return this;
	}

	public BruteForcePackagerBuilder withExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
		
		return this;
	}
	
	public BruteForcePackagerBuilder withRotate3D() {
		rotate3D = true;
		return this;
	}
	public BruteForcePackagerBuilder withRotate2D() {
		rotate3D = false;
		return this;
	}

	public BruteForcePackagerBuilder withBinarySearch() {
		this.binarySearch = true;
		return this;
	}

	public BruteForcePackagerBuilder withLinearSearch() {
		this.binarySearch = false;
		return this;
	}

	public BruteForcePackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return this;
	}

	public BruteForcePackager build() {
		if(containers == null) {
			throw new IllegalStateException("Expected containers");
		}

		if(threads == 1) {
			return new BruteForcePackager(containers, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
		}
		if(executorService != null) {
			return new ParallelBruteForcePackager(containers, executorService, threads, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
		}
		return new ParallelBruteForcePackager(containers, threads, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
	}
	
}
