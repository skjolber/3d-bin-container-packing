package com.github.skjolber.packing.packer.bruteforce.tree;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.packer.bruteforce.AbstractBruteForcePackager;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * The candidate positions of a box at a node, and the free points of the nodes: a packager's own search of an order, so that the tree places
 * the boxes as the packager's loop does. The levels are the depths of the tree; a level's box is tried in each rotation, at the candidates of
 * the rotation ({@link #next}); the placement of a level is removed when the tree returns to it ({@link #unplace}).
 */
abstract class BruteForceTreeCandidates {

	protected final Stack stack;
	/** with box load limits, the loads of the placed boxes; otherwise null */
	protected final LoadPlacementUtility utility;
	/** whether any arrangement of the best value will do, see {@link AbstractBruteForcePackager#setTreeSearchAnyBest(boolean)} */
	public boolean anyBest;
	/** the candidate found by {@link #next}: its point, its rotation (index of the box's stack values) and with load limits its supported area */
	protected SimplePoint3D point;
	protected int rotation;
	protected long supportedArea;

	protected BruteForceTreeCandidates(Stack stack, LoadPlacementUtility utility) {
		this.stack = stack;
		this.utility = utility;
	}

	/** the start of a search (or a restart): the container's free points, no placements */
	abstract void start(Container holder, ContainerItem containerItem, int n);

	/** the smallest area and volume of the boxes still to place: free points which cannot hold them are dropped */
	abstract void setLimits(long minArea, long minVolume);

	/** a node at the level is expanded: its free points are the ones each candidate starts from */
	abstract void enter(int level);

	/** the level's box is tried: its first rotation, and the placement for it */
	abstract void startType(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement);

	/**
	 * The next candidate of the level's box, in the current rotation or the next ones: the placement's stack value is the rotation, and
	 * {@link #point}, {@link #rotation} and {@link #supportedArea} describe the candidate.
	 *
	 * @param insertAfterCount the number of boxes placed before the box, which it must be insertable after
	 * @return the point index, or -1 if the box has no more candidates in any rotation
	 */
	abstract int next(int level, BoxItem item, BoxStackValue[] stackValues, Placement placement, int insertAfterCount);

	/** place the candidate at the level; when last, the free space after it is never searched */
	void place(int level, int pointIndex, Placement placement, boolean last) {
		placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());
		if(utility != null) {
			placement.clearLoad();
			placement.setIndex(stack.size());
			placement.setSupportedArea(supportedArea);
		}
		add(level, pointIndex, placement, last);
		stack.add(placement);
		if(utility != null) {
			utility.saveLoads(level);
			utility.addSupportersLoad(placement);
		}
	}

	/** the placement at its point, to the free points */
	abstract void add(int level, int pointIndex, Placement placement, boolean last);

	/** remove the placement of the level (the last placed), and restore the free points to before it */
	void unplace(int level, Placement placement) {
		if(utility != null) {
			placement.removeSupporteesAbove();
			for (PlacementLoad placementLoad : placement.getSupporters()) {
				placementLoad.getPlacement().removeLastSupportee();
			}
			placement.clearLoad();
			utility.restoreLoads(level);
		}
		stack.remove(stack.size() - 1);
		restore(level);
	}

	/** restore the free points to the ones the level's candidates start from */
	abstract void restore(int level);

	/** the node at the level is done (after {@link #enter}) */
	abstract void leave(int level);

	/** @return the point of the placement of the level */
	abstract SimplePoint3D placedPoint(int level);

	/** the free points of the node being expanded */
	abstract DefaultPointCalculator3D freePoints();

	/** @return a free point of the node which holds the box in one of its rotations (an empty region, so the box fits the free space), or -1 */
	int pointHolding(BoxStackValue[] rotations) {
		DefaultPointCalculator3D points = freePoints();
		int size = points.size();
		for (BoxStackValue rotation : rotations) {
			int dx = rotation.getDx();
			int dy = rotation.getDy();
			int dz = rotation.getDz();
			for (int i = 0; i < size; i++) {
				SimplePoint3D point = points.get(i);
				if(point.getDx() >= dx && point.getDy() >= dy && point.getDz() >= dz) {
					return i;
				}
			}
		}
		return -1;
	}

	protected static void clearToSize(DefaultPointCalculator3D pointCalculator, Container holder, ContainerItem containerItem) {
		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.getInitialPoints() != null) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
	}
}
