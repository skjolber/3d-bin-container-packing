package com.github.skjolber.packing.accelerator;

import static com.github.skjolber.packing.accelerator.FlatGeometry.*;
import static com.github.skjolber.packing.accelerator.FlatLayout.PLACEMENT_INTS;
import static com.github.skjolber.packing.accelerator.FlatLayout.PLANE_NONE;
import static com.github.skjolber.packing.accelerator.FlatLayout.POINT_INTS;

/**
 * The free points of the extreme point algorithm, as a port of {@code DefaultPointCalculator3D.add(..)} with immutable points, on a
 * flat {@code int[]}. The result of an add is the same list of points, in the same order, as the calculator's; a differential test pins that.
 * <p>
 * There is one frame (a list of points) for each level of the search. An add reads the frame of its level and writes the frame of the next level,
 * so going back to a level restores its points without copying anything. The methods are static and use primitive arrays only, so that the same code
 * can be run for many searches at once.
 */
public final class FlatPoints {

	private FlatPoints() {
	}

	/** @return the offset of the point with the index in the frame of the level */
	public static int pointOffset(FlatLayout layout, int base, int level, int index) {
		return base + layout.frameOffset(level) + 1 + index * POINT_INTS;
	}

	public static int count(FlatLayout layout, int[] s, int base, int level) {
		return s[base + layout.frameOffset(level)];
	}

	public static boolean isOverflow(FlatLayout layout, int[] s, int base) {
		return s[base + layout.control + FlatLayout.C_OVERFLOW] != 0;
	}

	public static void setPlacement(FlatLayout layout, int[] s, int base, int id, int x, int y, int z, int endX, int endY, int endZ) {
		int o = base + layout.placements + id * PLACEMENT_INTS;
		s[o] = x;
		s[o + 1] = y;
		s[o + 2] = z;
		s[o + 3] = endX;
		s[o + 4] = endY;
		s[o + 5] = endZ;
	}

	/**
	 * Set the first frame to the single point which is the whole container, and register the container as the placement with the highest id.
	 */
	public static void clear(FlatLayout layout, int[] s, int base, int dx, int dy, int dz) {
		setPlacement(layout, s, base, layout.n, 0, 0, 0, dx - 1, dy - 1, dz - 1);

		int frame = base + layout.frameOffset(0);
		s[frame] = 1;
		int p = frame + 1;
		s[p] = 0;
		s[p + 1] = 0;
		s[p + 2] = 0;
		s[p + 3] = dx - 1;
		s[p + 4] = dy - 1;
		s[p + 5] = dz - 1;
		s[p + 6] = FlatLayout.planes(layout.n, layout.n, layout.n);
	}

	/**
	 * Remove the points of the frame whose area or volume is below the limits, preserving the order.
	 *
	 * @return the new number of points
	 */
	public static int filterMinimums(FlatLayout layout, int[] s, int base, int level, long minArea, long minVolume) {
		int frame = base + layout.frameOffset(level);
		int size = s[frame];
		int values = frame + 1;

		int target = 0;
		for (int i = 0; i < size; i++) {
			int p = values + i * POINT_INTS;
			if(!(volume(s, p) < minVolume | area(s, p) < minArea)) {
				if(target != i) {
					copyPoint(s, p, values + target * POINT_INTS);
				}
				target++;
			}
		}
		s[frame] = target;
		return target;
	}

	private static boolean supportedXY(int[] s, int placements, int id, int x, int y) {
		if(id == PLANE_NONE) {
			return false;
		}
		int o = placements + id * PLACEMENT_INTS;
		return s[o] <= x & x <= s[o + 3] & s[o + 1] <= y & y <= s[o + 4];
	}

	private static boolean supportedXZ(int[] s, int placements, int id, int x, int z) {
		if(id == PLANE_NONE) {
			return false;
		}
		int o = placements + id * PLACEMENT_INTS;
		return s[o] <= x & x <= s[o + 3] & s[o + 2] <= z & z <= s[o + 5];
	}

	private static boolean supportedYZ(int[] s, int placements, int id, int y, int z) {
		if(id == PLANE_NONE) {
			return false;
		}
		int o = placements + id * PLACEMENT_INTS;
		return s[o + 1] <= y & y <= s[o + 4] & s[o + 2] <= z & z <= s[o + 5];
	}

	private static int binarySearchPlusMinX(int[] s, int values, int size, int low, int key) {
		int high = size - 1;
		int found = -1;
		while (low <= high && found == -1) {
			int mid = (low + high) >>> 1;
			int midVal = s[values + mid * POINT_INTS];
			if(midVal < key) {
				low = mid + 1;
			} else if(midVal != key) {
				high = mid - 1;
			} else {
				do {
					mid++;
				} while (mid < size && s[values + mid * POINT_INTS] == key);
				found = mid;
			}
		}
		return found == -1 ? low : found;
	}

	private static boolean canMoveX(int[] s, int p, int xx, long minArea, long minVolume) {
		if(s[p + MAX_X] < xx) {
			return false;
		}
		long areaAtX = areaAtX(s, p, xx);
		if(areaAtX >= minArea) {
			return true;
		}
		return !(areaAtX * dz(s, p) < minVolume);
	}

	private static boolean canMoveY(int[] s, int p, int yy, long minArea, long minVolume) {
		if(s[p + MAX_Y] < yy) {
			return false;
		}
		long areaAtY = areaAtY(s, p, yy);
		if(areaAtY >= minArea) {
			return true;
		}
		return !(areaAtY * dz(s, p) < minVolume);
	}

	private static boolean canMoveZ(int[] s, int p, int zz, long minVolume) {
		if(s[p + MAX_Z] < zz) {
			return false;
		}
		return !(volumeAtZ(s, p, zz) < minVolume);
	}

	private static boolean isConstrainedAtMaxX(int[] s, int p, int maxX, long minArea, long minVolume) {
		long areaAtMaxX = areaAtMaxX(s, p, maxX);
		if(areaAtMaxX >= minArea) {
			return false;
		}
		return areaAtMaxX * dz(s, p) < minVolume;
	}

	private static boolean isConstrainedAtMaxY(int[] s, int p, int maxY, long minArea, long minVolume) {
		long areaAtMaxY = areaAtMaxY(s, p, maxY);
		if(areaAtMaxY >= minArea) {
			return false;
		}
		return areaAtMaxY * dz(s, p) < minVolume;
	}

	/** @return true if the candidate is contained by a point of the output which is not after it in x */
	private static boolean isEclipsed(int[] s, int out, int count, int candidate) {
		int minX = s[candidate];
		int index = count - 1;
		while (index >= 0 && s[out + index * POINT_INTS] > minX) {
			index--;
		}
		boolean eclipsed = false;
		while (index >= 0 && !eclipsed) {
			eclipsed = eclipses(s, out + index * POINT_INTS, candidate);
			index--;
		}
		return eclipsed;
	}

	private static boolean isEclipsedAtXX(int[] s, int out, int count, int candidate, int xx) {
		boolean eclipsed = false;
		boolean done = false;
		int index = count - 1;
		while (index >= 0 && !done) {
			int o = out + index * POINT_INTS;
			if(s[o] < xx) {
				done = true;
			} else if(eclipses(s, o, candidate)) {
				eclipsed = true;
				done = true;
			}
			index--;
		}
		return eclipsed;
	}

	private static void sortX(int[] s, int list, int size, int values, int placements, int xx) {
		for (int i = 1; i < size; i++) {
			int index = s[list + i];
			int point = values + index * POINT_INTS;
			int j = i - 1;
			while (j >= 0 && FlatPointOrder.isAfterX(s, placements, values + s[list + j] * POINT_INTS, point, xx)) {
				s[list + j + 1] = s[list + j];
				j--;
			}
			s[list + j + 1] = index;
		}
	}

	private static void sortY(int[] s, int list, int size, int values, int placements, int yy) {
		for (int i = 1; i < size; i++) {
			int index = s[list + i];
			int point = values + index * POINT_INTS;
			int j = i - 1;
			while (j >= 0 && FlatPointOrder.isAfterY(s, placements, values + s[list + j] * POINT_INTS, point, yy)) {
				s[list + j + 1] = s[list + j];
				j--;
			}
			s[list + j + 1] = index;
		}
	}

	private static void sortZ(int[] s, int list, int size, int values, int placements, int zz) {
		for (int i = 1; i < size; i++) {
			int index = s[list + i];
			int point = values + index * POINT_INTS;
			int j = i - 1;
			while (j >= 0 && FlatPointOrder.isAfterZ(s, placements, values + s[list + j] * POINT_INTS, point, zz)) {
				s[list + j + 1] = s[list + j];
				j--;
			}
			s[list + j + 1] = index;
		}
	}

	/** Append the pool entry to the chain of the target index. */
	private static void chain(int[] s, int head, int tail, int next, int target, int entry) {
		s[next + entry] = -1;
		int last = s[tail + target];
		if(last == -1) {
			s[head + target] = entry;
		} else {
			s[next + last] = entry;
		}
		s[tail + target] = entry;
	}

	/**
	 * Add a placement at a free point of the frame of the level, and write the resulting free points to the frame of the next level.
	 * The placement must be in the placement table, and must be within the free point. The point is not filtered by the limits afterwards.
	 *
	 * @param level the level of the input frame
	 * @param pointIndex the index of the free point which holds the placement at its min corner
	 * @param placementId the id of the placement, in the placement table
	 * @param minArea the minimum area limit while adding
	 * @param minVolume the minimum volume limit while adding
	 * @return the number of points in the next frame, or -1 if there were more points than the layout holds (the scratch must then be reset)
	 */
	public static int add(FlatLayout layout, int[] s, int base, int level, int pointIndex, int placementId, long minArea, long minVolume) {
		final int placements = base + layout.placements;
		final int pl = placements + placementId * PLACEMENT_INTS;
		final int placementX = s[pl];
		final int placementY = s[pl + 1];
		final int placementZ = s[pl + 2];
		final int placementEndX = s[pl + 3];
		final int placementEndY = s[pl + 4];
		final int placementEndZ = s[pl + 5];

		final int inFrame = base + layout.frameOffset(level);
		final int outFrame = base + layout.frameOffset(level + 1);
		final int size = s[inFrame];
		final int values = inFrame + 1;
		final int out = outFrame + 1;
		final int flags = base + layout.flags;

		final int xx = placementEndX + 1;
		final int yy = placementEndY + 1;
		final int zz = placementEndZ + 1;

		int source = values + pointIndex * POINT_INTS;
		int sourcePlanes = s[source + PLANES];
		boolean supportedXY = s[source + MIN_Z] == placementZ && supportedXY(s, placements, FlatLayout.xy(sourcePlanes), placementEndX, placementEndY);
		boolean supportedXZ = s[source + MIN_Y] == placementY && supportedXZ(s, placements, FlatLayout.xz(sourcePlanes), placementEndX, placementEndZ);
		boolean supportedYZ = s[source + MIN_X] == placementX && supportedYZ(s, placements, FlatLayout.yz(sourcePlanes), placementEndY, placementEndZ);
		boolean supported = supportedXY && supportedXZ && supportedYZ;

		int endIndex = binarySearchPlusMinX(s, values, size, pointIndex + 1, placementEndX);

		int startIndex;
		if(supportedYZ) {
			startIndex = pointIndex;
			while (startIndex > 0 && s[values + (startIndex - 1) * POINT_INTS] == placementX) {
				startIndex--;
			}
		} else {
			startIndex = 0;
		}

		// classify
		final int listX = base + layout.moveX;
		final int listY = base + layout.moveY;
		final int listZ = base + layout.moveZ;
		int moveCountX = 0;
		int moveCountY = 0;
		int moveCountZ = 0;

		for (int i = startIndex; i < endIndex; i++) {
			int p = values + i * POINT_INTS;
			if(s[p + MIN_Y] <= placementEndY && s[p + MIN_Z] <= placementEndZ) {
				boolean swallowed = s[p + MIN_X] >= placementX && s[p + MIN_Y] >= placementY && s[p + MIN_Z] >= placementZ;
				if(swallowed) {
					s[flags + i] = 1;
				}
				if(swallowed || !supported) {
					if(canMoveX(s, p, xx, minArea, minVolume)) {
						s[listX + moveCountX] = i;
						moveCountX++;
					}
					if(canMoveY(s, p, yy, minArea, minVolume)) {
						s[listY + moveCountY] = i;
						moveCountY++;
					}
					if(canMoveZ(s, p, zz, minVolume)) {
						s[listZ + moveCountZ] = i;
						moveCountZ++;
					}
				}
			}
		}

		// move x
		final int poolX = base + layout.poolX;
		int poolCountX = 0;
		if(moveCountX > 0) {
			sortX(s, listX, moveCountX, values, placements, xx);

			int target = endIndex;
			for (int k = 0; k < moveCountX; k++) {
				int currentIndex = s[listX + k];
				int p = values + currentIndex * POINT_INTS;

				boolean eclipsed = false;
				int a = 0;
				while (a < poolCountX && !eclipsed) {
					eclipsed = eclipsesMovedX(s, poolX + a * POINT_INTS, p, xx);
					a++;
				}
				if(!eclipsed) {
					int added = poolX + poolCountX * POINT_INTS;
					int planes = s[p + PLANES];
					int xz = FlatLayout.xz(planes);
					int xy = FlatLayout.xy(planes);
					if(xz != PLANE_NONE && xx > s[placements + xz * PLACEMENT_INTS + 3]) {
						xz = PLANE_NONE;
					}
					if(xy != PLANE_NONE && xx > s[placements + xy * PLACEMENT_INTS + 3]) {
						xy = PLANE_NONE;
					}
					int yz = PLANE_NONE;
					if(!(s[p + MIN_Y] < placementY || s[p + MIN_Z] < placementZ)) {
						yz = placementId;
					}
					s[added] = xx;
					s[added + 1] = s[p + 1];
					s[added + 2] = s[p + 2];
					s[added + 3] = s[p + 3];
					s[added + 4] = s[p + 4];
					s[added + 5] = s[p + 5];
					s[added + PLANES] = FlatLayout.planes(yz, xz, xy);

					while (target < size && isAfterXyz(s, added, values + target * POINT_INTS)) {
						target++;
					}
					chain(s, base + layout.headX, base + layout.tailX, base + layout.nextX, target, poolCountX);
					poolCountX++;
				}
			}
		}

		// move y
		final int poolY = base + layout.poolY;
		int poolCountY = 0;
		if(moveCountY > 0) {
			sortY(s, listY, moveCountY, values, placements, yy);

			for (int k = 0; k < moveCountY; k++) {
				int currentIndex = s[listY + k];
				int p = values + currentIndex * POINT_INTS;

				boolean eclipsed = false;
				int a = 0;
				while (a < poolCountY && !eclipsed) {
					eclipsed = eclipsesMovedY(s, poolY + a * POINT_INTS, p, yy);
					a++;
				}
				if(!eclipsed) {
					int added = poolY + poolCountY * POINT_INTS;
					int planes = s[p + PLANES];
					int yz = FlatLayout.yz(planes);
					int xy = FlatLayout.xy(planes);
					if(yz != PLANE_NONE && yy > s[placements + yz * PLACEMENT_INTS + 4]) {
						yz = PLANE_NONE;
					}
					if(xy != PLANE_NONE && yy > s[placements + xy * PLACEMENT_INTS + 4]) {
						xy = PLANE_NONE;
					}
					int xz = PLANE_NONE;
					if(!(s[p + MIN_X] < placementX || s[p + MIN_Z] < placementZ)) {
						xz = placementId;
					}
					s[added] = s[p];
					s[added + 1] = yy;
					s[added + 2] = s[p + 2];
					s[added + 3] = s[p + 3];
					s[added + 4] = s[p + 4];
					s[added + 5] = s[p + 5];
					s[added + PLANES] = FlatLayout.planes(yz, xz, xy);

					int target = currentIndex + 1;
					while (target < size && isAfterXyz(s, added, values + target * POINT_INTS)) {
						target++;
					}
					chain(s, base + layout.headY, base + layout.tailY, base + layout.nextY, target, poolCountY);
					poolCountY++;
				}
			}
		}

		// move z
		final int poolZ = base + layout.poolZ;
		int poolCountZ = 0;
		if(moveCountZ > 0) {
			sortZ(s, listZ, moveCountZ, values, placements, zz);

			for (int k = 0; k < moveCountZ; k++) {
				int currentIndex = s[listZ + k];
				int p = values + currentIndex * POINT_INTS;

				boolean eclipsed = false;
				int a = 0;
				while (a < poolCountZ && !eclipsed) {
					eclipsed = eclipsesMovedZ(s, poolZ + a * POINT_INTS, p, zz);
					a++;
				}
				if(!eclipsed) {
					int added = poolZ + poolCountZ * POINT_INTS;
					int planes = s[p + PLANES];
					int yz = FlatLayout.yz(planes);
					int xz = FlatLayout.xz(planes);
					if(yz != PLANE_NONE && zz > s[placements + yz * PLACEMENT_INTS + 5]) {
						yz = PLANE_NONE;
					}
					if(xz != PLANE_NONE && zz > s[placements + xz * PLACEMENT_INTS + 5]) {
						xz = PLANE_NONE;
					}
					int xy = PLANE_NONE;
					if(!(s[p + MIN_X] < placementX || s[p + MIN_Y] < placementY)) {
						xy = placementId;
					}
					s[added] = s[p];
					s[added + 1] = s[p + 1];
					s[added + 2] = zz;
					s[added + 3] = s[p + 3];
					s[added + 4] = s[p + 4];
					s[added + 5] = s[p + 5];
					s[added + PLANES] = FlatLayout.planes(yz, xz, xy);

					int target = currentIndex + 1;
					while (target < size && isAfterXyz(s, added, values + target * POINT_INTS)) {
						target++;
					}
					chain(s, base + layout.headZ, base + layout.tailZ, base + layout.nextZ, target, poolCountZ);
					poolCountZ++;
				}
			}
		}

		// constrain the max values of the points which the placement cuts into
		final int copyX = base + layout.copyX;
		final int copyY = base + layout.copyY;
		final int copyZ = base + layout.copyZ;

		if(supported) {
			// not necessary
		} else if(supportedXY && supportedXZ) {
			// directly left of the placement
			int limit = 0;
			int scan = endIndex - 1;
			while (scan >= 0 && limit == 0) {
				if(s[flags + scan] == 0 && s[values + scan * POINT_INTS] < placementX) {
					limit = scan + 1;
				}
				scan--;
			}
			for (int i = 0; i < limit; i++) {
				int p = values + i * POINT_INTS;
				if(s[flags + i] == 0 && placementY <= s[p + MIN_Y] && s[p + MIN_Y] <= placementEndY && placementZ <= s[p + MIN_Z] && s[p + MIN_Z] <= placementEndZ && placementX <= s[p + MAX_X]) {
					long area = (placementX - s[p + MIN_X]) * (long)dy(s, p);
					if(area >= minArea) {
						s[copyX + i] = placementX - 1;
					}
					s[flags + i] = 1;
				}
			}
		} else if(supportedXY && supportedYZ) {
			// directly in front of the placement
			for (int i = startIndex; i < endIndex; i++) {
				int p = values + i * POINT_INTS;
				if(s[flags + i] == 0 && s[p + MIN_Y] < placementY && placementY <= s[p + MAX_Y] && placementZ <= s[p + MIN_Z] && s[p + MIN_Z] <= placementEndZ && placementX <= s[p + MIN_X]
						&& s[p + MIN_X] <= placementEndX) {
					long area = (placementY - s[p + MIN_Y]) * (long)dx(s, p);
					if(area >= minArea) {
						s[copyY + i] = placementY - 1;
					}
					s[flags + i] = 1;
				}
			}
		} else if(supportedXZ && supportedYZ) {
			// directly below the placement
			for (int i = startIndex; i < endIndex; i++) {
				int p = values + i * POINT_INTS;
				if(s[flags + i] == 0 && s[p + MIN_Z] < placementZ && placementZ <= s[p + MAX_Z] && placementY <= s[p + MIN_Y] && s[p + MIN_Y] <= placementEndY && placementX <= s[p + MIN_X]
						&& s[p + MIN_X] <= placementEndX) {
					s[copyZ + i] = placementZ - 1;
					s[flags + i] = 1;
				}
			}
		} else {
			for (int i = 0; i < endIndex; i++) {
				int p = values + i * POINT_INTS;
				if(!(placementEndX < s[p + MIN_X] || placementEndY < s[p + MIN_Y] || placementEndZ < s[p + MIN_Z] || placementX > s[p + MAX_X] || placementY > s[p + MAX_Y]
						|| placementZ > s[p + MAX_Z])) {
					if(s[p + MIN_X] < placementX) {
						int copyMaxX = placementX - 1;
						if(!isConstrainedAtMaxX(s, p, copyMaxX, minArea, minVolume)) {
							s[copyX + i] = copyMaxX;
						}
					}
					if(s[p + MIN_Y] < placementY) {
						int copyMaxY = placementY - 1;
						if(!isConstrainedAtMaxY(s, p, copyMaxY, minArea, minVolume)) {
							s[copyY + i] = copyMaxY;
						}
					}
					if(s[p + MIN_Z] < placementZ) {
						int copyMaxZ = placementZ - 1;
						if(!(volumeAtMaxZ(s, p, copyMaxZ) < minVolume)) {
							s[copyZ + i] = copyMaxZ;
						}
					}
					s[flags + i] = 1;
				}
			}
		}

		// merge, in x order
		final int headX = base + layout.headX;
		final int headY = base + layout.headY;
		final int headZ = base + layout.headZ;
		final int tailX = base + layout.tailX;
		final int tailY = base + layout.tailY;
		final int tailZ = base + layout.tailZ;
		final int nextX = base + layout.nextX;
		final int nextY = base + layout.nextY;
		final int nextZ = base + layout.nextZ;
		final int pMax = layout.pMax;
		final int spare = out + pMax * POINT_INTS;

		int count = 0;
		boolean overflow = false;
		for (int i = 0; i < endIndex; i++) {
			for (int round = 0; round < 2; round++) {
				int head = round == 0 ? headZ : headY;
				int next = round == 0 ? nextZ : nextY;
				int pool = round == 0 ? poolZ : poolY;
				int entry = s[head + i];
				while (entry != -1) {
					int p = pool + entry * POINT_INTS;
					if(!isEclipsed(s, out, count, p)) {
						if(count < pMax) {
							copyPoint(s, p, out + count * POINT_INTS);
							count++;
						} else {
							overflow = true;
						}
					}
					entry = s[next + entry];
				}
				s[head + i] = -1;
				s[(round == 0 ? tailZ : tailY) + i] = -1;
			}

			int p = values + i * POINT_INTS;
			if(s[flags + i] == 0) {
				if(count < pMax) {
					copyPoint(s, p, out + count * POINT_INTS);
					count++;
				} else {
					overflow = true;
				}
			} else {
				s[flags + i] = 0;
			}

			for (int round = 0; round < 3; round++) {
				int copy = round == 0 ? copyX : round == 1 ? copyY : copyZ;
				int max = s[copy + i];
				if(max != -1) {
					copyPoint(s, p, spare);
					s[spare + MAX_X + round] = max;
					if(!isEclipsed(s, out, count, spare)) {
						if(count < pMax) {
							copyPoint(s, spare, out + count * POINT_INTS);
							count++;
						} else {
							overflow = true;
						}
					}
					s[copy + i] = -1;
				}
			}
		}

		for (int round = 0; round < 2; round++) {
			int head = round == 0 ? headZ : headY;
			int next = round == 0 ? nextZ : nextY;
			int pool = round == 0 ? poolZ : poolY;
			int entry = s[head + endIndex];
			while (entry != -1) {
				int p = pool + entry * POINT_INTS;
				if(!isEclipsed(s, out, count, p)) {
					if(count < pMax) {
						copyPoint(s, p, out + count * POINT_INTS);
						count++;
					} else {
						overflow = true;
					}
				}
				entry = s[next + entry];
			}
			s[head + endIndex] = -1;
			s[(round == 0 ? tailZ : tailY) + endIndex] = -1;
		}

		for (int i = endIndex; i <= size; i++) {
			int entry = s[headX + i];
			while (entry != -1) {
				int p = poolX + entry * POINT_INTS;
				boolean eclipsed;
				if(FlatLayout.yz(s[p + PLANES]) != PLANE_NONE) {
					eclipsed = isEclipsedAtXX(s, out, count, p, xx);
				} else {
					eclipsed = isEclipsed(s, out, count, p);
				}
				if(!eclipsed) {
					if(count < pMax) {
						copyPoint(s, p, out + count * POINT_INTS);
						count++;
					} else {
						overflow = true;
					}
				}
				entry = s[nextX + entry];
			}
			s[headX + i] = -1;
			s[tailX + i] = -1;

			if(i < size) {
				if(s[flags + i] == 0) {
					if(count < pMax) {
						copyPoint(s, values + i * POINT_INTS, out + count * POINT_INTS);
						count++;
					} else {
						overflow = true;
					}
				} else {
					s[flags + i] = 0;
				}
			}
		}

		s[outFrame] = count;
		if(overflow) {
			s[base + layout.control + FlatLayout.C_OVERFLOW] = 1;
			return -1;
		}
		return count;
	}
}
