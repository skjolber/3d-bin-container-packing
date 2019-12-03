package com.github.skjolber.packing;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class BruteForcePackagerBuilder {

	private int threads = 1;
	
	private List<Container> containers;
	private boolean rotate3D;
	private boolean binarySearch;
	private ExecutorService executorService;

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

	public BruteForcePackager build() {
		if(threads == 1) {
			return new BruteForcePackager(containers, rotate3D, binarySearch);
		}
		if(executorService != null) {
			return new ParallelBruteForcePackager(containers, executorService, threads, rotate3D, binarySearch);
		}
		return new ParallelBruteForcePackager(containers, threads, rotate3D, binarySearch);
	}
	
}
