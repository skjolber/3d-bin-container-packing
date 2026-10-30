package com.github.skjolber.packing.packer.bruteforce.tree;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager.FastBruteForceBoxStackValuePointComparator;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastPointCalculator3DStack;
import com.github.skjolber.packing.packer.bruteforce.FullSupportCandidates;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * The candidates of {@link FastBruteForcePackager}: per rotation the best free point by its point comparator (where the box is insertable, and
 * with full support the best fully supported position), on its point calculator.
 */
public final class BruteForceTreeFastCandidates extends BruteForceTreeCandidates {

	private final FastBruteForcePackager packager;
	private final FastPointCalculator3DStack pointCalculator;
	private final FastBruteForceBoxStackValuePointComparator pointComparator;
	private final FullSupportCandidates fullSupportCandidates;
	private final boolean checkExtraction;
	private final boolean checkObstacles;
	private final Container container;
	private final int[] rotationIndexes;

	public BruteForceTreeFastCandidates(FastBruteForcePackager packager, FastPointCalculator3DStack pointCalculator, FastBruteForceBoxStackValuePointComparator pointComparator, boolean fullSupport,
			boolean checkExtraction, LoadPlacementUtility utility, Container holder, int n) {
		super(holder.getStack(), utility);
		this.packager = packager;
		this.pointCalculator = pointCalculator;
		this.pointComparator = pointComparator;
		this.fullSupportCandidates = fullSupport ? pointCalculator.getFullSupportCandidates() : null;
		this.checkExtraction = checkExtraction;
		this.checkObstacles = !holder.getObstacles().isEmpty();
		this.container = holder;
		this.rotationIndexes = new int[n];
	}

	@Override
	void start(Container holder, ContainerItem containerItem, int n) {
		clearToSize(pointCalculator, holder, containerItem);
		if(utility != null) {
			utility.initialize(n);
		}
	}

	@Override
	void setLimits(long minArea, long minVolume) {
		pointCalculator.setMinimumAreaAndVolumeLimit(minArea, minVolume);
	}

	@Override
	void enter(int level) {
		// the candidates do not change the free points
	}

	@Override
	void startType(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement) {
		rotationIndexes[level] = 0;
	}

	@Override
	int next(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement, int insertAfterCount) {
		while (rotationIndexes[level] < stackValues.length) {
			BoxStackValue stackValue = stackValues[rotationIndexes[level]];
			rotationIndexes[level]++;
			int pointIndex;
			SimplePoint3D candidatePoint;
			if(fullSupportCandidates != null) {
				fullSupportCandidates.populate(pointCalculator, null, stack.getPlacements(), stackValue);
				int candidate = packager.getBestFullySupported(fullSupportCandidates, item, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility,
						pointComparator);
				if(candidate == -1) {
					continue;
				}
				pointIndex = fullSupportCandidates.getPointIndex(candidate);
				candidatePoint = fullSupportCandidates.getPoint(candidate);
			} else {
				if(utility == null) {
					pointIndex = packager.getBestPoint(pointCalculator, item, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, pointComparator);
				} else {
					pointIndex = packager.getBestPointWithLoad(pointCalculator, item, stackValue, stack, insertAfterCount, container, checkObstacles, checkExtraction, utility, pointComparator);
				}
				if(pointIndex == -1) {
					continue;
				}
				candidatePoint = pointCalculator.get(pointIndex);
			}
			if(utility != null) {
				// the utility caches are primed for the last position checked: repopulate them for the selected one
				utility.populatePointSupporters(candidatePoint);
				utility.populatePointSupportees(candidatePoint, stackValue.getDz(), stackValue.getDz());
				supportedArea = utility.getSupportedAreaAtPoint(candidatePoint, stackValue, false);
			}
			placement.setStackValue(item, stackValue);
			point = candidatePoint;
			rotation = rotationIndexes[level] - 1;
			return pointIndex;
		}
		return -1;
	}

	@Override
	void add(int level, int pointIndex, Placement placement, boolean last) {
		if(last) {
			pointCalculator.addLast(point, placement);
		} else {
			pointCalculator.add(pointIndex, placement, point);
		}
	}

	@Override
	void restore(int level) {
		pointCalculator.setStackSize(level);
	}

	@Override
	void leave(int level) {
		// nothing saved
	}

	@Override
	SimplePoint3D placedPoint(int level) {
		return pointCalculator.getPoint(level);
	}
}
