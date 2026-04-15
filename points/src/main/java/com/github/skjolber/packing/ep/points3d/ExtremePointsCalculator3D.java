package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.point.Point;

/**
 * Computes the extreme points representing the remaining free space within a 3D container
 * after accounting for a set of pre-placed obstacles.
 *
 * <p>Starts from a single point covering the full container and processes each obstacle
 * in turn via {@link ExtremePointIteration3D}, which projects the current points onto the
 * obstacle's faces, removes occupied positions, tightens bounds, and eliminates eclipsed
 * points. The result after all obstacles have been processed is the set of corners where
 * a new box could be placed.</p>
 *
 * <p>Obstacles need not be in any particular order.</p>
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

	private final ExtremePointIteration3D iteration = new ExtremePointIteration3D();

	/**
	 * Computes the extreme points of the free space remaining in a container after
	 * placing all obstacles.
	 *
	 * <p>Starts from a single origin point that covers the full container, then applies
	 * each obstacle via {@link ExtremePointIteration3D}. After each obstacle, eclipsed
	 * points are eliminated so the set stays compact.</p>
	 *
	 * @param containerDx container size in x
	 * @param containerDy container size in y
	 * @param containerDz container size in z
	 * @param obstacles   pre-placed boxes; order does not matter
	 * @return non-eclipsed extreme points representing the remaining free space
	 */
	public List<Point> calculate(int containerDx, int containerDy, int containerDz, List<Obstacle> obstacles) {
		List<Point> points = new ArrayList<>();
		points.add(new DefaultPoint3D(0, 0, 0, containerDx - 1, containerDy - 1, containerDz - 1));

		List<Obstacle> accumulated = new ArrayList<>(obstacles.size());

		for (Obstacle obstacle : obstacles) {
			accumulated.add(obstacle);
			points = iteration.calculate(points, obstacle, accumulated, containerDx, containerDy, containerDz);
		}

		return points;
	}
}
