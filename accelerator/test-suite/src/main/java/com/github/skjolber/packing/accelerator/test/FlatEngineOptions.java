package com.github.skjolber.packing.accelerator.test;

import com.github.skjolber.packing.accelerator.*;

/** The options of an engine in the tests, so that the same tests build {@linkplain FlatEngine} and its ports: the options and defaults of its builder. */
public class FlatEngineOptions {

	public int threads = Runtime.getRuntime().availableProcessors();
	public int maxPoints = FlatEngine.DEFAULT_MAX_POINTS;
	public int nodeBudget = FlatEngine.DEFAULT_NODE_BUDGET;
	public int itemsPerThread = FlatEngine.DEFAULT_ITEMS_PER_THREAD;
	public int maxItems = FlatEngine.DEFAULT_MAX_ITEMS;
	public int minBoxes = FlatEngine.DEFAULT_MIN_BOXES;
	public int maxBoxes = FlatEngine.DEFAULT_MAX_BOXES;
	public boolean tree = false;
	/** the states which a thread of the tree search remembers ({@link FlatStateCache}), zero for none */
	public int stateCache = 0;

	public FlatEngineOptions withThreads(int threads) {
		this.threads = threads;
		return this;
	}

	public FlatEngineOptions withMaxPoints(int maxPoints) {
		this.maxPoints = maxPoints;
		return this;
	}

	public FlatEngineOptions withNodeBudget(int nodeBudget) {
		this.nodeBudget = nodeBudget;
		return this;
	}

	public FlatEngineOptions withItemsPerThread(int itemsPerThread) {
		this.itemsPerThread = itemsPerThread;
		return this;
	}

	public FlatEngineOptions withMaxItems(int maxItems) {
		this.maxItems = maxItems;
		return this;
	}

	public FlatEngineOptions withMinBoxes(int minBoxes) {
		this.minBoxes = minBoxes;
		return this;
	}

	public FlatEngineOptions withMaxBoxes(int maxBoxes) {
		this.maxBoxes = maxBoxes;
		return this;
	}

	public FlatEngineOptions withStateCache(int stateCache) {
		this.stateCache = stateCache;
		return this;
	}

	public FlatEngineOptions withTree(boolean tree) {
		this.tree = tree;
		return this;
	}

	public FlatEngine toFlatEngine() {
		return FlatEngine.newBuilder()
				.withThreads(threads)
				.withMaxPoints(maxPoints)
				.withNodeBudget(nodeBudget)
				.withItemsPerThread(itemsPerThread)
				.withMaxItems(maxItems)
				.withMinBoxes(minBoxes)
				.withMaxBoxes(maxBoxes)
				.withTree(tree)
				.withStateCache(stateCache)
				.build();
	}
}
