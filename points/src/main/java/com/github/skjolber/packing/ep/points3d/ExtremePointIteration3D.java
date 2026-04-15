package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.ExtremePointsCalculator3D.Obstacle;

/**
 * Processes one obstacle against the current set of extreme points and returns the
 * updated set for the next iteration.
 *
 * <p>For a single obstacle the steps are:</p>
 * <ol>
 *   <li><b>Project</b> — each current point is projected onto the three outward faces of
 *       the obstacle, producing candidate extreme points just beyond those faces.</li>
 *   <li><b>Filter</b> — candidates that fall inside any accumulated obstacle are
 *       discarded.</li>
 *   <li><b>Bounds</b> — surviving candidates receive tight {@code maxX/maxY/maxZ} bounds
 *       by scanning all accumulated obstacles for the nearest blocker in each positive
 *       direction.</li>
 *   <li><b>Update existing</b> — points occupied by the new obstacle are removed; the
 *       remaining points have their bounds incrementally tightened by the new obstacle.</li>
 *   <li><b>Eclipse removal</b> — points whose free-space bounding box is fully contained
 *       within another point's bounding box are discarded.</li>
 * </ol>
 */
public class ExtremePointIteration3D {

	/**
	 * Applies one obstacle to the current set of extreme points.
	 *
	 * @param current      extreme points computed from all prior obstacles
	 * @param obstacle     the obstacle being added in this iteration
	 * @param allObstacles all obstacles accumulated so far, including {@code obstacle}
	 * @param containerDx  container size in x
	 * @param containerDy  container size in y
	 * @param containerDz  container size in z
	 * @return updated extreme points after accounting for {@code obstacle}
	 */
	public List<Point> calculate(List<Point> current, Obstacle obstacle,
			List<Obstacle> allObstacles,
			int containerDx, int containerDy, int containerDz) {

		int maxX = containerDx - 1;
		int maxY = containerDy - 1;
		int maxZ = containerDz - 1;

		// Coordinates just beyond each outward face of the obstacle
		int xx = obstacle.getX() + obstacle.getDx();
		int yy = obstacle.getY() + obstacle.getDy();
		int zz = obstacle.getZ() + obstacle.getDz();

		// Step 1 — project each current point onto the three obstacle faces
		List<int[]> candidateCoords = new ArrayList<>(current.size() * 3);
		for (Point p : current) {
			if (xx <= maxX) candidateCoords.add(new int[]{ xx, p.getMinY(), p.getMinZ() });
			if (yy <= maxY) candidateCoords.add(new int[]{ p.getMinX(), yy, p.getMinZ() });
			if (zz <= maxZ) candidateCoords.add(new int[]{ p.getMinX(), p.getMinY(), zz });
		}

		// Step 2 & 3 — discard occupied candidates; compute bounds for survivors
		List<Point> candidates = new ArrayList<>(candidateCoords.size());
		for (int[] coord : candidateCoords) {
			int cx = coord[0], cy = coord[1], cz = coord[2];
			if (isOccupiedByAny(cx, cy, cz, allObstacles)) {
				continue;
			}
			int pmx = computeMaxX(cx, cy, cz, maxX, allObstacles);
			int pmy = computeMaxY(cx, cy, cz, maxY, allObstacles);
			int pmz = computeMaxZ(cx, cy, cz, maxZ, allObstacles);
			candidates.add(new DefaultPoint3D(cx, cy, cz, pmx, pmy, pmz));
		}

		// Step 4 — update existing points: remove those occupied, tighten bounds for survivors
		List<Point> result = new ArrayList<>(current.size() + candidates.size());
		for (Point p : current) {
			if (isOccupied(p.getMinX(), p.getMinY(), p.getMinZ(), obstacle)) {
				continue;
			}
			tightenBounds(p, obstacle);
			result.add(p);
		}

		result.addAll(candidates);

		// Step 5 — eclipse removal
		removeEclipsed(result);

		return result;
	}

	/**
	 * Tightens a point's max bounds based on a newly-added obstacle that may now block
	 * the point's free-space extent in one or more directions.
	 */
	private void tightenBounds(Point p, Obstacle o) {
		int px = p.getMinX(), py = p.getMinY(), pz = p.getMinZ();

		if (o.getX() > px && py >= o.getY() && py <= o.endY() && pz >= o.getZ() && pz <= o.endZ()) {
			if (o.getX() - 1 < p.getMaxX()) p.setMaxX(o.getX() - 1);
		}
		if (o.getY() > py && px >= o.getX() && px <= o.endX() && pz >= o.getZ() && pz <= o.endZ()) {
			if (o.getY() - 1 < p.getMaxY()) p.setMaxY(o.getY() - 1);
		}
		if (o.getZ() > pz && px >= o.getX() && px <= o.endX() && py >= o.getY() && py <= o.endY()) {
			if (o.getZ() - 1 < p.getMaxZ()) p.setMaxZ(o.getZ() - 1);
		}
	}

	private boolean isOccupied(int cx, int cy, int cz, Obstacle o) {
		return cx >= o.getX() && cx <= o.endX()
				&& cy >= o.getY() && cy <= o.endY()
				&& cz >= o.getZ() && cz <= o.endZ();
	}

	private boolean isOccupiedByAny(int cx, int cy, int cz, List<Obstacle> obstacles) {
		for (Obstacle o : obstacles) {
			if (isOccupied(cx, cy, cz, o)) return true;
		}
		return false;
	}

	private int computeMaxX(int cx, int cy, int cz, int containerMaxX, List<Obstacle> obstacles) {
		int max = containerMaxX;
		for (Obstacle o : obstacles) {
			if (o.getX() > cx && cy >= o.getY() && cy <= o.endY() && cz >= o.getZ() && cz <= o.endZ()) {
				max = Math.min(max, o.getX() - 1);
			}
		}
		return max;
	}

	private int computeMaxY(int cx, int cy, int cz, int containerMaxY, List<Obstacle> obstacles) {
		int max = containerMaxY;
		for (Obstacle o : obstacles) {
			if (o.getY() > cy && cx >= o.getX() && cx <= o.endX() && cz >= o.getZ() && cz <= o.endZ()) {
				max = Math.min(max, o.getY() - 1);
			}
		}
		return max;
	}

	private int computeMaxZ(int cx, int cy, int cz, int containerMaxZ, List<Obstacle> obstacles) {
		int max = containerMaxZ;
		for (Obstacle o : obstacles) {
			if (o.getZ() > cz && cx >= o.getX() && cx <= o.endX() && cy >= o.getY() && cy <= o.endY()) {
				max = Math.min(max, o.getZ() - 1);
			}
		}
		return max;
	}

	private void removeEclipsed(List<Point> points) {
		int size = points.size();
		boolean[] eclipsed = new boolean[size];

		for (int i = 0; i < size; i++) {
			if (eclipsed[i]) continue;
			Point pi = points.get(i);
			for (int j = 0; j < size; j++) {
				if (i == j || eclipsed[j]) continue;
				if (points.get(j).eclipses(pi)) {
					eclipsed[i] = true;
					break;
				}
			}
		}

		for (int i = size - 1; i >= 0; i--) {
			if (eclipsed[i]) points.remove(i);
		}
	}
}
