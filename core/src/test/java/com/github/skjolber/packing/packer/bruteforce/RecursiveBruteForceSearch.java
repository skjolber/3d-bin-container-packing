package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.List;

import org.eclipse.collections.api.iterator.IntIterator;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Reference for the brute-force placement search: the recursive implementation (5.0.0 development),
 * kept as an oracle for the iterative search. Each box of the permutation is placed at every fitting
 * point in turn, recursing to the next box, and the deepest arrangement is kept.
 *
 * <pre>
 *  box 0:  point a ─┬─ box 1: point c ─── box 2: ...
 *                   └─ box 1: point d ─── box 2: ...
 *          point b ─── box 1: ...
 * </pre>
 */
public class RecursiveBruteForceSearch {

	/** As {@code AbstractBruteForcePackager.packStackPlacement(..)} without load limits. */
	public static List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points,
			BruteForcePointIteratorFilter pointFilter, int maxPackableCount) throws PackagerInterruptedException {
		pointCalculator.resetBest();
		if(placements.length == 0) {
			return Collections.emptyList();
		}
		int maxLoadWeight = container.getMaxLoadWeight();
		if(maxPackableCount == 0) {
			return Collections.emptyList();
		}
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(points != null) {
			pointCalculator.setPoints(points);
			pointCalculator.clear();
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(0));
		if(pointFilter == null) {
			packStackPlacement(pointCalculator, placements, iterator, stack, maxLoadWeight, 0, interrupt, minStackableAreaIndex, maxPackableCount);
		} else {
			packStackPlacement(pointCalculator, placements, iterator, stack, maxLoadWeight, 0, interrupt, minStackableAreaIndex, maxPackableCount, pointFilter);
		}
		return pointCalculator.getBestPoints();
	}

	/** As {@code LoadBruteForcePackager.packStackPlacement(..)}. */
	public static List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			Container container, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points, LoadPlacementUtility utility,
			BruteForcePointIteratorFilter pointFilter, int maxPackableCount) throws PackagerInterruptedException {
		pointCalculator.resetBest();
		if(placements.length == 0) {
			return Collections.emptyList();
		}
		if(utility == null) {
			return packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt, minStackableAreaIndex, points, pointFilter, maxPackableCount);
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

		utility.initialize(iterator.length());
		if(pointFilter == null) {
			packStackPlacement(pointCalculator, placements, iterator, stack, container.getMaxLoadWeight(), 0, interrupt, minStackableAreaIndex, maxPackableCount, utility);
		} else {
			packStackPlacement(pointCalculator, placements, iterator, stack, container.getMaxLoadWeight(), 0, interrupt, minStackableAreaIndex, maxPackableCount, utility, pointFilter);
		}
		return pointCalculator.getBestPoints();
	}

	private static void packStackPlacement(PointCalculator3DStack pointCalculatorStack, Placement[] placements, BoxItemPermutationRotationIterator rotator, Stack stack,
			int maxLoadWeight, int placementIndex, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, int maxPackableCount) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		if(pointCalculatorStack.getBestStackIndex() >= maxPackableCount) {
			return;
		}
		BoxStackValue stackValue = rotator.getStackValue(placementIndex);
		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return;
		}
		Placement placement = placements[placementIndex];
		placement.setStackValue(stackValue);
		maxLoadWeight -= stackValue.getBox().getWeight();
		pointCalculatorStack.updateBest();
		pointCalculatorStack.push();
		int currentPointsCount = pointCalculatorStack.size();
		for(int k = 0; k < currentPointsCount; k++) {
			SimplePoint3D point3d = pointCalculatorStack.get(k);
			if(!point3d.fits3D(stackValue)) {
				continue;
			}
			placement.setPoint(point3d);
			pointCalculatorStack.add(k, placement);
			if(placementIndex + 1 >= maxPackableCount) {
				pointCalculatorStack.updateBest();
				break;
			}
			stack.add(placement);
			int nextMinStackableAreaIndex;
			if(placementIndex == minStackableAreaIndex) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);
				pointCalculatorStack.setMinimumAreaAndVolumeLimit(rotator.getStackValue(nextMinStackableAreaIndex).getArea(), rotator.getMinBoxVolume(placementIndex + 1));
			} else {
				pointCalculatorStack.setMinimumVolumeLimit(rotator.getMinBoxVolume(placementIndex + 1));
				nextMinStackableAreaIndex = minStackableAreaIndex;
			}
			packStackPlacement(pointCalculatorStack, placements, rotator, stack, maxLoadWeight, placementIndex + 1, interrupt, nextMinStackableAreaIndex, maxPackableCount);
			stack.remove(stack.size() - 1);
			if(pointCalculatorStack.getBestStackIndex() >= maxPackableCount) {
				break;
			}
			pointCalculatorStack.redo();
		}
		pointCalculatorStack.pop();
	}

	private static void packStackPlacement(PointCalculator3DStack pointCalculatorStack, Placement[] placements, BoxItemPermutationRotationIterator rotator, Stack stack,
			int maxLoadWeight, int placementIndex, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, int maxPackableCount,
			BruteForcePointIteratorFilter pointFilter) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		if(pointCalculatorStack.getBestStackIndex() >= maxPackableCount) {
			return;
		}
		BoxStackValue stackValue = rotator.getStackValue(placementIndex);
		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return;
		}
		Placement placement = placements[placementIndex];
		placement.setStackValue(stackValue);
		maxLoadWeight -= stackValue.getBox().getWeight();
		pointCalculatorStack.updateBest();
		pointCalculatorStack.push();
		IntIterator pointIterator = pointFilter.getPoints(pointCalculatorStack, stackValue);
		while(pointIterator.hasNext()) {
			int k = pointIterator.next();
			SimplePoint3D point3d = pointCalculatorStack.get(k);
			placement.setPoint(point3d);
			pointCalculatorStack.add(k, placement);
			if(placementIndex + 1 >= maxPackableCount) {
				pointCalculatorStack.updateBest();
				break;
			}
			stack.add(placement);
			int nextMinStackableAreaIndex;
			if(placementIndex == minStackableAreaIndex) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);
				pointCalculatorStack.setMinimumAreaAndVolumeLimit(rotator.getStackValue(nextMinStackableAreaIndex).getArea(), rotator.getMinBoxVolume(placementIndex + 1));
			} else {
				pointCalculatorStack.setMinimumVolumeLimit(rotator.getMinBoxVolume(placementIndex + 1));
				nextMinStackableAreaIndex = minStackableAreaIndex;
			}
			packStackPlacement(pointCalculatorStack, placements, rotator, stack, maxLoadWeight, placementIndex + 1, interrupt, nextMinStackableAreaIndex, maxPackableCount,
					pointFilter);
			stack.remove(stack.size() - 1);
			if(pointCalculatorStack.getBestStackIndex() >= maxPackableCount) {
				break;
			}
			pointCalculatorStack.redo();
		}
		pointCalculatorStack.pop();
	}

	private static void packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack, int maxLoadWeight,
			int placementIndex, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, int maxPackableCount, LoadPlacementUtility utility)
			throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		if(pointCalculator.getBestStackIndex() >= maxPackableCount) {
			return;
		}
		BoxStackValue stackValue = iterator.getStackValue(placementIndex);
		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return;
		}
		pointCalculator.updateBest();
		pointCalculator.push();
		int currentPointsCount = pointCalculator.size();
		for(int k = 0; k < currentPointsCount; k++) {
			SimplePoint3D point = pointCalculator.get(k);
			if(!point.fits3D(stackValue)) {
				continue;
			}
			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			long supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
			if(supportedArea == -1L) {
				continue;
			}
			attemptPlacement(pointCalculator, placements, iterator, stack, maxLoadWeight, placementIndex, interrupt, minStackableAreaIndex, maxPackableCount, utility,
					stackValue, k, point, supportedArea, null);
			if(pointCalculator.getBestStackIndex() >= maxPackableCount) {
				break;
			}
			pointCalculator.redo();
		}
		pointCalculator.pop();
	}

	private static void packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack, int maxLoadWeight,
			int placementIndex, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, int maxPackableCount, LoadPlacementUtility utility,
			BruteForcePointIteratorFilter pointFilter) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		if(pointCalculator.getBestStackIndex() >= maxPackableCount) {
			return;
		}
		BoxStackValue stackValue = iterator.getStackValue(placementIndex);
		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return;
		}
		pointCalculator.updateBest();
		pointCalculator.push();
		IntIterator points = pointFilter.getPoints(pointCalculator, stackValue);
		while(points.hasNext()) {
			int k = points.next();
			SimplePoint3D point = pointCalculator.get(k);
			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			long supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
			if(supportedArea == -1L) {
				continue;
			}
			attemptPlacement(pointCalculator, placements, iterator, stack, maxLoadWeight, placementIndex, interrupt, minStackableAreaIndex, maxPackableCount, utility,
					stackValue, k, point, supportedArea, pointFilter);
			if(pointCalculator.getBestStackIndex() >= maxPackableCount) {
				break;
			}
			pointCalculator.redo();
		}
		pointCalculator.pop();
	}

	private static void attemptPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
			int maxLoadWeight, int placementIndex, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, int maxPackableCount, LoadPlacementUtility utility,
			BoxStackValue stackValue, int pointIndex, SimplePoint3D point, long supportedArea, BruteForcePointIteratorFilter pointFilter) throws PackagerInterruptedException {
		Placement placement = placements[placementIndex];
		placement.setStackValue(stackValue);
		placement.setPoint(point);
		placement.setIndex(stack.size());
		placement.setSupportedArea(supportedArea);
		pointCalculator.add(pointIndex, placement);
		if(placementIndex + 1 >= maxPackableCount) {
			pointCalculator.updateBest();
			return;
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
		if(pointFilter == null) {
			packStackPlacement(pointCalculator, placements, iterator, stack, maxLoadWeight - stackValue.getBox().getWeight(), placementIndex + 1, interrupt,
					nextMinStackableAreaIndex, maxPackableCount, utility);
		} else {
			packStackPlacement(pointCalculator, placements, iterator, stack, maxLoadWeight - stackValue.getBox().getWeight(), placementIndex + 1, interrupt,
					nextMinStackableAreaIndex, maxPackableCount, utility, pointFilter);
		}
		for(PlacementLoad placementLoad : placement.getSupporters()) {
			placementLoad.getPlacement().removeLastSupportee();
		}
		placement.clearLoad();
		stack.remove(stack.size() - 1);
	}

	/** Brute-force packager using the recursive search. */
	public static class RecursiveBruteForcePackager extends BruteForcePackager {

		public RecursiveBruteForcePackager(BruteForcePointIteratorFilter pointFilter) {
			super(new BruteForceIntermediatePackagerResultComparator(), pointFilter, false);
		}

		@Override
		protected List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
				Container container, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points, LoadPlacementUtility loadPlacementUtility,
				BruteForcePointIteratorFilter pointFilter, int maxPackableCount) throws PackagerInterruptedException {
			return RecursiveBruteForceSearch.packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt, minStackableAreaIndex, points, pointFilter,
					maxPackableCount);
		}
	}

	/** Load-aware brute-force packager using the recursive search. */
	public static class RecursiveLoadBruteForcePackager extends LoadBruteForcePackager {

		public RecursiveLoadBruteForcePackager(BruteForcePointIteratorFilter pointFilter) {
			super(new BruteForceIntermediatePackagerResultComparator(), pointFilter);
		}

		@Override
		protected List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, Placement[] placements, BoxItemPermutationRotationIterator iterator, Stack stack,
				Container container, PackagerInterruptSupplier interrupt, int minStackableAreaIndex, List<Point> points, LoadPlacementUtility loadPlacementUtility,
				BruteForcePointIteratorFilter pointFilter, int maxPackableCount) throws PackagerInterruptedException {
			return RecursiveBruteForceSearch.packStackPlacement(pointCalculator, placements, iterator, stack, container, interrupt, minStackableAreaIndex, points,
					loadPlacementUtility, pointFilter, maxPackableCount);
		}
	}
}
