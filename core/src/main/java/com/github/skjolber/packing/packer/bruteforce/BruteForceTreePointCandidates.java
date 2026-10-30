package com.github.skjolber.packing.packer.bruteforce;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * The candidates of {@link BruteForcePackager}: all free points where the box fits (or the point filter's), in index order, the rotations in
 * order, with the checks of the loop ({@link AbstractBruteForcePackager#nextPlacement}), on the loop's point calculator.
 */
final class BruteForceTreePointCandidates extends BruteForceTreeCandidates {

	private final PointCalculator3DStack pointCalculator;
	private final BruteForceSearchFrames frames;
	private final BruteForcePointIteratorFilter pointFilter;
	private final boolean fullSupport;
	private final boolean checkExtraction;
	private final List<Placement> obstacles;
	private final boolean checkObstacles;
	private final ContainerAccess access;

	BruteForceTreePointCandidates(PointCalculator3DStack pointCalculator, BruteForcePointIteratorFilter pointFilter, boolean fullSupport, boolean checkExtraction, LoadPlacementUtility utility,
			Container holder) {
		super(holder.getStack(), utility);
		this.pointCalculator = pointCalculator;
		this.frames = pointCalculator.getSearchFrames();
		this.pointFilter = pointFilter;
		this.fullSupport = fullSupport;
		this.checkExtraction = checkExtraction;
		this.obstacles = holder.getObstacles();
		this.checkObstacles = obstacles != null && !obstacles.isEmpty();
		this.access = holder.getAccess();
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
		pointCalculator.push();
	}

	@Override
	void startType(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement) {
		frames.rotationIndexes[level] = 0;
		placement.setStackValue(item, stackValues[0]);
		AbstractBruteForcePackager.startCandidates(pointCalculator, pointFilter, fullSupport, frames, level, stack, stackValues[0]);
	}

	@Override
	int next(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement, int insertAfterCount) {
		int pointIndex = AbstractBruteForcePackager.nextPlacement(pointCalculator, pointFilter, fullSupport, frames, level, stack, item, stackValues, placement, insertAfterCount, access,
				checkObstacles, obstacles, checkExtraction, utility);
		if(pointIndex != -1) {
			point = frames.candidatePoint;
			supportedArea = frames.candidateSupportedArea;
			rotation = frames.rotationIndexes[level];
		}
		return pointIndex;
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
		pointCalculator.redo();
	}

	@Override
	void leave(int level) {
		pointCalculator.pop();
	}

	@Override
	SimplePoint3D placedPoint(int level) {
		return pointCalculator.stackItems[level + 1].point;
	}
}
