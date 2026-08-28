package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
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
		public Builder withThreads(int threads) {
			super.withThreads(threads);
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
			return new LoadParallelBoxItemBruteForcePackager(executorService, parallelizationCount, comparator);
		}
	}

	public LoadParallelBoxItemBruteForcePackager(ExecutorService executorService, int parallelizationCount,
			Comparator<IntermediatePackagerResult> comparator) {
		super(executorService, parallelizationCount, comparator);
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
	public List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, List<Placement> placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points,
			LoadPlacementUtility loadPlacementUtility) throws PackagerInterruptedException {
		if(placements.isEmpty()) {
			return Collections.emptyList();
		}
		if(loadPlacementUtility == null) {
			return super.packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt,
					minStackableAreaIndex, points, null);
		}

		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(points != null) {
			pointCalculator.setPoints(points);
			pointCalculator.clear();
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(),
				iterator.getMinBoxVolume(0));

		loadPlacementUtility.initialize(placements.size());
		return packStackPlacement(pointCalculator, placements, iterator, stack, container.getMaxLoadWeight(), 0,
				interrupt, minStackableAreaIndex, Collections.emptyList(), loadPlacementUtility);
	}

	private List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, List<Placement> placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, int maxLoadWeight, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> best,
			LoadPlacementUtility utility) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}

		BoxStackValue stackValue = iterator.getStackValue(placementIndex);
		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return null;
		}
		if(pointCalculator.getStackIndex() > best.size()) {
			best = pointCalculator.getPoints();
		}

		pointCalculator.push();
		int currentPointsCount = pointCalculator.size();
		for(int k = 0; k < currentPointsCount; k++) {
			Point point = pointCalculator.get(k);
			if(!point.fits3D(stackValue)) {
				continue;
			}

			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			long supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
			if(supportedArea == -1L) {
				continue;
			}

			Placement placement = placements.get(placementIndex);
			placement.setStackValue(stackValue);
			placement.setPoint(point);
			placement.setIndex(stack.size());
			placement.setSupportedArea(supportedArea);

			pointCalculator.add(k, placement);
			if(placementIndex + 1 >= iterator.length()) {
				best = pointCalculator.getPoints();
				break;
			}

			stack.add(placement);
			utility.addSupportersLoad(placement);

			int nextMinStackableAreaIndex;
			if(placementIndex == minStackableAreaIndex) {
				nextMinStackableAreaIndex = iterator.getMinStackableAreaIndex(placementIndex + 1);
				pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(nextMinStackableAreaIndex).getArea(), iterator.getMinBoxVolume(placementIndex + 1));
			} else {
				pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(placementIndex + 1));
				nextMinStackableAreaIndex = minStackableAreaIndex;
			}

			List<Point> result = packStackPlacement(pointCalculator, placements, iterator, stack,
					maxLoadWeight - stackValue.getBox().getWeight(), placementIndex + 1, interrupt,
					nextMinStackableAreaIndex, best, utility);

			for(PlacementLoad placementLoad : placement.getSupporters()) {
				placementLoad.getPlacement().removeLastSupportee();
			}
			placement.clearLoad();
			stack.remove(placement);

			if(result != null) {
				if(result.size() >= iterator.length()) {
					best = result;
					break;
				}
				if(best.size() < result.size()) {
					best = result;
				}
			}
			pointCalculator.redo();
		}
		pointCalculator.pop();
		return best;
	}
}
