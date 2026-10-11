package com.github.skjolber.packing.packer.bruteforce;

import java.util.List;

import org.eclipse.collections.api.iterator.IntIterator;

import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

/**
 * The positions where a box is fully supported, for brute force with full support required: the free points where the
 * box rests completely on the floor or on the boxes below, and, like {@code FullSupportPlacementControls}, positions
 * shifted from a free point onto the corner of a box below. A large free point can hide the smaller point on top of a
 * single box (the smaller one is within it), so a box which would overhang at the point's origin can be fully
 * supported further in:
 *
 * <pre>
 *  z
 *  2          [   B   ]     the free point on top of A starts at x = 0 and reaches over O and C; B there would rest
 *  1  [A][O]  [   C   ]     partly on the obstacle O. Shifted onto C's corner (x = 2), B is fully supported.
 *  0
 *     0  1  2           5  x
 * </pre>
 *
 * Boxes rest on the floor or on placed boxes, not on obstacles. Each position refers to the free point which holds it.
 */
public class FullSupportCandidates {

	private int[] pointIndexes = new int[16];
	private SimplePoint3D[] points = new SimplePoint3D[16];
	private int size;

	/**
	 * Find the positions for a box.
	 *
	 * @param pointCalculator the free points
	 * @param pointIndexes the free points to consider (from a point filter), or null for all
	 * @param stack the placed boxes
	 * @param stackValue the box, in a rotation
	 */
	public void populate(DefaultPointCalculator3D pointCalculator, IntIterator pointIndexes, List<Placement> stack, BoxStackValue stackValue) {
		size = 0;
		if(pointIndexes == null) {
			for(int i = 0; i < pointCalculator.size(); i++) {
				add(pointCalculator, i, stack, stackValue);
			}
		} else {
			while(pointIndexes.hasNext()) {
				add(pointCalculator, pointIndexes.next(), stack, stackValue);
			}
		}
	}

	private void add(DefaultPointCalculator3D pointCalculator, int pointIndex, List<Placement> stack, BoxStackValue stackValue) {
		SimplePoint3D point = pointCalculator.get(pointIndex);
		if(!point.fits3D(stackValue)) {
			return;
		}
		int z = point.getMinZ();
		if(z == 0 || point.isSupportedXYPlane(stackValue) || isFullySupported(stack, point.getMinX(), point.getMinY(), z, stackValue)) {
			add(pointIndex, point);
		}
		if(z == 0) {
			// on the floor: always fully supported at the point's origin
			return;
		}
		// shifted onto the corners of the boxes below, within the point
		int lastX = point.getMaxX() - stackValue.getDx() + 1;
		int lastY = point.getMaxY() - stackValue.getDy() + 1;
		// a box below must at least reach the end of the box placed at the point's origin
		int minEndX = point.getMinX() + stackValue.getDx() - 1;
		int minEndY = point.getMinY() + stackValue.getDy() - 1;
		int start = size;
		for(int i = 0; i < stack.size(); i++) {
			Placement below = stack.get(i);
			if(below.getAbsoluteEndZ() != z - 1) {
				continue;
			}
			if(below.getAbsoluteX() > lastX || below.getAbsoluteEndX() < minEndX) {
				continue;
			}
			if(below.getAbsoluteY() > lastY || below.getAbsoluteEndY() < minEndY) {
				continue;
			}
			int x = Math.max(below.getAbsoluteX(), point.getMinX());
			int y = Math.max(below.getAbsoluteY(), point.getMinY());
			if(x == point.getMinX() && y == point.getMinY()) {
				// the point's origin, see above
				continue;
			}
			if(contains(start, x, y) || !isFullySupported(stack, x, y, z, stackValue)) {
				continue;
			}
			add(pointIndex, new DefaultPoint3D(x, y, z, point.getMaxX(), point.getMaxY(), point.getMaxZ()));
		}
	}

	private boolean contains(int start, int x, int y) {
		for(int i = start; i < size; i++) {
			if(points[i].getMinX() == x && points[i].getMinY() == y) {
				return true;
			}
		}
		return false;
	}

	private void add(int pointIndex, SimplePoint3D point) {
		if(size == points.length) {
			int capacity = size * 2;
			int[] nextPointIndexes = new int[capacity];
			System.arraycopy(pointIndexes, 0, nextPointIndexes, 0, size);
			SimplePoint3D[] nextPoints = new SimplePoint3D[capacity];
			System.arraycopy(points, 0, nextPoints, 0, size);
			pointIndexes = nextPointIndexes;
			points = nextPoints;
		}
		pointIndexes[size] = pointIndex;
		points[size] = point;
		size++;
	}

	/**
	 * @param stack the placed boxes
	 * @return true if a box at the position rests completely on the floor or on the placed boxes (which do not overlap)
	 */
	public static boolean isFullySupported(List<Placement> stack, int x, int y, int z, BoxStackValue stackValue) {
		if(z == 0) {
			return true;
		}
		int endX = x + stackValue.getDx() - 1;
		int endY = y + stackValue.getDy() - 1;
		long area = 0L;
		for(int i = 0; i < stack.size(); i++) {
			Placement below = stack.get(i);
			if(below.getAbsoluteEndZ() != z - 1) {
				continue;
			}
			int minX = Math.max(x, below.getAbsoluteX());
			int maxX = Math.min(endX, below.getAbsoluteEndX());
			if(minX > maxX) {
				continue;
			}
			int minY = Math.max(y, below.getAbsoluteY());
			int maxY = Math.min(endY, below.getAbsoluteEndY());
			if(minY > maxY) {
				continue;
			}
			area += (long) (maxX - minX + 1) * (maxY - minY + 1);
		}
		return area == stackValue.getArea();
	}

	/** @return the number of positions */
	public int size() {
		return size;
	}

	/** @return the index of the free point which holds the position */
	public int getPointIndex(int index) {
		return pointIndexes[index];
	}

	/** @return the position: the free point, or a point within it */
	public SimplePoint3D getPoint(int index) {
		return points[index];
	}
}
