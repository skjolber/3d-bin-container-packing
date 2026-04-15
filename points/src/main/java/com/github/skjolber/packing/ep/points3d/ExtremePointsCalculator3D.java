package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.github.skjolber.packing.api.point.Point;

/**
 * Computes the extreme points representing the remaining free space within a 3D container
 * after accounting for a set of pre-placed obstacles.
 *
 * <p>An extreme point is a corner of the free space where a new box can potentially be
 * placed. Candidates are generated from the coordinate intersections of obstacle faces
 * and the container origin, filtered to remove occupied positions, and then reduced by
 * eclipse removal (a point whose free space is fully contained within another point's
 * free space is discarded).</p>
 *
 * <p>Obstacles need not be in any particular order. They must not overlap each other or
 * extend outside the container.</p>
 */
public class ExtremePointsCalculator3D {

	/**
	 * A pre-placed box occupying a fixed region within the container.
	 *
	 * <p>Coordinates are inclusive: the obstacle occupies
	 * [{@code x}, {@code x+dx-1}] × [{@code y}, {@code y+dy-1}] × [{@code z}, {@code z+dz-1}].</p>
	 */
	public static class Obstacle {

		private final int x, y, z;
		private final int dx, dy, dz;

		/**
		 * @param x  origin x-coordinate (inclusive, ≥ 0)
		 * @param y  origin y-coordinate (inclusive, ≥ 0)
		 * @param z  origin z-coordinate (inclusive, ≥ 0)
		 * @param dx size in x (must be &gt; 0)
		 * @param dy size in y (must be &gt; 0)
		 * @param dz size in z (must be &gt; 0)
		 */
		public Obstacle(int x, int y, int z, int dx, int dy, int dz) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
		}

		public int getX() { return x; }
		public int getY() { return y; }
		public int getZ() { return z; }
		public int getDx() { return dx; }
		public int getDy() { return dy; }
		public int getDz() { return dz; }

		int endX() { return x + dx - 1; }
		int endY() { return y + dy - 1; }
		int endZ() { return z + dz - 1; }
	}

	/**
	 * Computes the extreme points of the free space remaining in a container after
	 * placing all obstacles.
	 *
	 * <p>The algorithm:</p>
	 * <ol>
	 *   <li>Collect candidate coordinates on each axis: the container origin (0) plus the
	 *       coordinate immediately beyond each obstacle's far face.</li>
	 *   <li>For each coordinate triple, skip positions occupied by any obstacle.</li>
	 *   <li>For surviving positions, compute the tightest bounding box: how far a box
	 *       placed there can extend in each direction before hitting an obstacle or wall.</li>
	 *   <li>Remove eclipsed points: if one point's free-space bounding box fully contains
	 *       another's, the smaller one is redundant and is discarded.</li>
	 * </ol>
	 *
	 * @param containerDx container size in x
	 * @param containerDy container size in y
	 * @param containerDz container size in z
	 * @param obstacles   pre-placed boxes; order does not matter
	 * @return non-eclipsed extreme points representing the remaining free space
	 */
	public List<Point> calculate(int containerDx, int containerDy, int containerDz, List<Obstacle> obstacles) {
		int maxX = containerDx - 1;
		int maxY = containerDy - 1;
		int maxZ = containerDz - 1;

		int[] xs = candidateCoordinates(obstacles, containerDx, 0);
		int[] ys = candidateCoordinates(obstacles, containerDy, 1);
		int[] zs = candidateCoordinates(obstacles, containerDz, 2);

		List<Point> result = new ArrayList<>();

		for (int cx : xs) {
			for (int cy : ys) {
				for (int cz : zs) {
					if (isOccupied(cx, cy, cz, obstacles)) {
						continue;
					}
					int pointMaxX = computeMaxX(cx, cy, cz, maxX, obstacles);
					int pointMaxY = computeMaxY(cx, cy, cz, maxY, obstacles);
					int pointMaxZ = computeMaxZ(cx, cy, cz, maxZ, obstacles);
					result.add(new DefaultPoint3D(cx, cy, cz, pointMaxX, pointMaxY, pointMaxZ));
				}
			}
		}

		removeEclipsed(result);
		return result;
	}

	/**
	 * Builds the sorted, deduplicated set of candidate coordinates along one axis.
	 *
	 * @param axis 0 = x, 1 = y, 2 = z
	 */
	private int[] candidateCoordinates(List<Obstacle> obstacles, int containerSize, int axis) {
		TreeSet<Integer> set = new TreeSet<>();
		set.add(0);
		for (Obstacle o : obstacles) {
			int face = (axis == 0) ? (o.x + o.dx) : (axis == 1) ? (o.y + o.dy) : (o.z + o.dz);
			if (face < containerSize) {
				set.add(face);
			}
		}
		int[] result = new int[set.size()];
		int i = 0;
		for (int v : set) {
			result[i++] = v;
		}
		return result;
	}

	private boolean isOccupied(int cx, int cy, int cz, List<Obstacle> obstacles) {
		for (Obstacle o : obstacles) {
			if (cx >= o.x && cx <= o.endX()
					&& cy >= o.y && cy <= o.endY()
					&& cz >= o.z && cz <= o.endZ()) {
				return true;
			}
		}
		return false;
	}

	/** Returns the largest maxX reachable from cx without entering any obstacle. */
	private int computeMaxX(int cx, int cy, int cz, int containerMaxX, List<Obstacle> obstacles) {
		int max = containerMaxX;
		for (Obstacle o : obstacles) {
			if (o.x > cx && cy >= o.y && cy <= o.endY() && cz >= o.z && cz <= o.endZ()) {
				max = Math.min(max, o.x - 1);
			}
		}
		return max;
	}

	private int computeMaxY(int cx, int cy, int cz, int containerMaxY, List<Obstacle> obstacles) {
		int max = containerMaxY;
		for (Obstacle o : obstacles) {
			if (o.y > cy && cx >= o.x && cx <= o.endX() && cz >= o.z && cz <= o.endZ()) {
				max = Math.min(max, o.y - 1);
			}
		}
		return max;
	}

	private int computeMaxZ(int cx, int cy, int cz, int containerMaxZ, List<Obstacle> obstacles) {
		int max = containerMaxZ;
		for (Obstacle o : obstacles) {
			if (o.z > cz && cx >= o.x && cx <= o.endX() && cy >= o.y && cy <= o.endY()) {
				max = Math.min(max, o.z - 1);
			}
		}
		return max;
	}

	/**
	 * Removes points whose free-space bounding box is fully contained within another
	 * point's bounding box (eclipse removal).
	 */
	private void removeEclipsed(List<Point> points) {
		int size = points.size();
		boolean[] eclipsed = new boolean[size];

		for (int i = 0; i < size; i++) {
			if (eclipsed[i]) {
				continue;
			}
			Point pi = points.get(i);
			for (int j = 0; j < size; j++) {
				if (i == j || eclipsed[j]) {
					continue;
				}
				if (points.get(j).eclipses(pi)) {
					eclipsed[i] = true;
					break;
				}
			}
		}

		for (int i = size - 1; i >= 0; i--) {
			if (eclipsed[i]) {
				points.remove(i);
			}
		}
	}
}
