package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Parallel brute-force packager which evaluates per-box load weight, pressure,
 * box-count, and identical-box constraints while exploring placements.
 */
public class LoadParallelBoxItemBruteForcePackager extends ParallelBoxItemBruteForcePackager {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends ParallelBruteForcePackagerBuilder {



		@Override
		public Builder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		@Override
		public Builder withThreads(int threads) {
			super.withThreads(threads);
			return this;
		}

		@Override
		public Builder withPointFilter(BruteForcePointIteratorFilter pointFilter) {
			super.withPointFilter(pointFilter);
			return this;
		}

		@Override
		public Builder withSkipReversePermutations(boolean filterReversePermutations) {
			super.withSkipReversePermutations(filterReversePermutations);
			return this;
		}

		@Override
		public Builder withParallelizationCount(int parallelizationCount) {
			super.withParallelizationCount(parallelizationCount);
			return this;
		}

		@Override
		public Builder withExecutorService(ExecutorService executorService) {
			super.withExecutorService(executorService);
			return this;
		}

		@Override
		public Builder withAvailableProcessors(int factor) {
			super.withAvailableProcessors(factor);
			return this;
		}

		@Override
		public LoadParallelBoxItemBruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			if(executorService == null) {
				if(threads == -1) {
					threads = Runtime.getRuntime().availableProcessors();
				}
				executorService = Executors.newFixedThreadPool(threads);
				if(parallelizationCount == -1) {
					parallelizationCount = 16 * threads;
				}
			} else {
				if(threads != -1) {
					throw new IllegalArgumentException("Not expecting both thread count and executor service");
				}
				if(parallelizationCount == -1) {
					if(executorService instanceof ThreadPoolExecutor threadPoolExecutor) {
						parallelizationCount = 16 * threadPoolExecutor.getMaximumPoolSize();
					} else {
						throw new ParallelBruteForcePackagerException(
								"Expected a parallelization count for custom executor service");
					}
				}
			}
			
			LoadParallelBoxItemBruteForcePackager packager = new LoadParallelBoxItemBruteForcePackager(executorService, parallelizationCount, comparator, pointFilter, filterReversePermutations);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			return packager;
		}
	}

	public LoadParallelBoxItemBruteForcePackager(ExecutorService executorService, int parallelizationCount,
			Comparator<IntermediatePackagerResult> comparator, BruteForcePointIteratorFilter pointFilter) {
		this(executorService, parallelizationCount, comparator, pointFilter, false);
	}

	public LoadParallelBoxItemBruteForcePackager(ExecutorService executorService, int parallelizationCount,
			Comparator<IntermediatePackagerResult> comparator, BruteForcePointIteratorFilter pointFilter, boolean filterReversePermutations) {
		super(executorService, parallelizationCount, comparator, pointFilter, filterReversePermutations);
	}

	@Override
	protected boolean supportsLoad() {
		return true;
	}

	@Override
	protected LoadPlacementUtility createLoadPlacementUtility(BoxItemPermutationRotationIterator iterator, Stack stack) {
		return LoadBruteForcePackager.createLoadPlacementUtilityImpl(iterator, stack);
	}

	@Override
	public List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points,
			LoadPlacementUtility loadPlacementUtility, BruteForcePointIteratorFilter pointFilter) throws PackagerInterruptedException {
		int maxPackableCount = placements.length == 0 ? 0 : getMaxPackableCount(iterator, container.getMaxLoadVolume(), container.getMaxLoadWeight());
		return preservePoints(packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt,
				minStackableAreaIndex, points, loadPlacementUtility, pointFilter, maxPackableCount));
	}

	@Override
	protected List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points,
			LoadPlacementUtility loadPlacementUtility, BruteForcePointIteratorFilter pointFilter,
			int maxPackableCount) throws PackagerInterruptedException {
		pointCalculator.resetBest();
		if(placements.length == 0) {
			return Collections.emptyList();
		}
		if(loadPlacementUtility == null) {
			return super.packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt,
					minStackableAreaIndex, points, null, pointFilter, maxPackableCount);
		}
		if(maxPackableCount == 0) {
			return Collections.emptyList();
		}

		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(points != null) {
			pointCalculator.setPoints(points);
			pointCalculator.clear();
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(0));

		loadPlacementUtility.initialize(iterator.length());
		search(pointCalculator, placements, iterator, stack, container.getMaxLoadWeight(), interrupt, minStackableAreaIndex, maxPackableCount, loadPlacementUtility, pointFilter, container.getObstacles(), container.getAccess());
		return pointCalculator.getBestPoints();
	}

}
