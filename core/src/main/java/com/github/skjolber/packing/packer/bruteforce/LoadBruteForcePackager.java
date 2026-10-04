package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;
import com.github.skjolber.packing.packer.util.WeightLoadAwarePlacementUtility;
import com.github.skjolber.packing.packer.util.WeightPressureCountIdenticalLoadAwarePlacementUtility;
import com.github.skjolber.packing.packer.util.WeightPressureCountLoadAwarePlacementUtility;

/**
 * Brute-force packager which evaluates per-box load weight, pressure, box-count,
 * and identical-box constraints while exploring placements.
 */

public class LoadBruteForcePackager extends BruteForcePackager {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends BruteForcePackagerBuilder {

		@Override
		public Builder withComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.comparator = comparator;
			return this;
		}

		@Override
		public Builder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		@Override
		public Builder withPoints(List<Point> points) {
			this.points = points;
			return this;
		}

		@Override
		public Builder withPointFilter(BruteForcePointIteratorFilter pointFilter) {
			this.pointFilter = pointFilter;
			return this;
		}

		@Override
		public LoadBruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			LoadBruteForcePackager packager = new LoadBruteForcePackager(comparator, pointFilter);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			return packager;
		}
	}

	public LoadBruteForcePackager(Comparator<IntermediatePackagerResult> comparator) {
		super(comparator, false);
	}

	public LoadBruteForcePackager(Comparator<IntermediatePackagerResult> comparator, BruteForcePackager.BruteForcePointIteratorFilter pointIndexSelector) {
		super(comparator, pointIndexSelector, false);
	}

	@Override
	protected boolean supportsLoad() {
		return true;
	}

	@Override
	public List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack,
			com.github.skjolber.packing.api.Container container, PackagerInterruptSupplier interrupt,
			int minStackableAreaIndex, List<Point> points, LoadPlacementUtility loadPlacementUtility, BruteForcePointIteratorFilter pointFilter) throws PackagerInterruptedException {
		int maxPackableCount = placements.length == 0 ? 0 : getMaxPackableCount(iterator, container.getMaxLoadVolume(), container.getMaxLoadWeight());
		return preservePoints(packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt,
				minStackableAreaIndex, points, loadPlacementUtility, pointFilter, maxPackableCount));
	}

	@Override
	protected List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack,
			com.github.skjolber.packing.api.Container container, PackagerInterruptSupplier interrupt,
			int minStackableAreaIndex, List<Point> points, LoadPlacementUtility loadPlacementUtility,
			BruteForcePointIteratorFilter pointFilter, int maxPackableCount) throws PackagerInterruptedException {
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
		search(pointCalculator, placements, iterator, stack, container.getMaxLoadWeight(), interrupt, minStackableAreaIndex, maxPackableCount, loadPlacementUtility, pointFilter);
		return pointCalculator.getBestPoints();
	}

	@Override
	protected LoadPlacementUtility createLoadPlacementUtility(BoxItemPermutationRotationIterator iterator, Stack stack) {
		return createLoadPlacementUtilityImpl(iterator, stack);
	}

	protected static LoadPlacementUtility createLoadPlacementUtilityImpl(BoxItemPermutationRotationIterator iterator, Stack stack) {
		boolean maxLoadWeight = false;
		boolean maxLoadPressure = false;
		boolean maxLoadBoxCount = false;
		boolean loadIdenticalBox = false;
		for(int i = 0; i < iterator.length(); i++) {
			Box box = iterator.getStackValue(i).getBox();
			maxLoadWeight |= box.isMaxLoadWeight();
			maxLoadPressure |= box.isMaxLoadPressure();
			maxLoadBoxCount |= box.isMaxLoadBoxCount();
			loadIdenticalBox |= box.isLoadIdenticalBoxOnly();
		}

		if(!maxLoadWeight && !maxLoadPressure && !maxLoadBoxCount && !loadIdenticalBox) {
			return null;
		}
		if(maxLoadWeight && !maxLoadPressure && !maxLoadBoxCount && !loadIdenticalBox) {
			return new WeightLoadAwarePlacementUtility(stack);
		}
		if(!loadIdenticalBox) {
			return new WeightPressureCountLoadAwarePlacementUtility(stack);
		}
		return new WeightPressureCountIdenticalLoadAwarePlacementUtility(stack);
	}

}
