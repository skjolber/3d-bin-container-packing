package com.github.skjolber.packing.accelerator;

import static com.github.skjolber.packing.accelerator.FlatGeometry.MIN_Z;
import static com.github.skjolber.packing.accelerator.FlatLayout.POINT_INTS;

/**
 * The search of one box order, as a port of {@code FastBruteForcePackager.searchOrder(..)} without skipping and without load limits, obstacles,
 * extraction orders and groups, with the default point comparator: depth first over the boxes of the permutation, where each box is tried in each of
 * its rotations, at the best free point for the rotation. The first arrangement which places more boxes than any before it is the best, and the search
 * ends as soon as an arrangement places them all.
 * <p>
 * The levels are the placed boxes, with a frame of free points for each level (see {@link FlatPoints}), so going back to a level restores its points
 * without copying anything. The method is static and uses primitive arrays only, so that the same code can be run for many searches at once.
 */
public final class FlatSearch {

	private FlatSearch() {
	}

	/**
	 * Search the permutation which is in the scratch ({@link FlatLayout#perm}, as types). The result is the number of placed boxes, their
	 * rotations ({@link FlatLayout#bestRotation}) and their volume ({@link FlatLayout#bestVolume}).
	 *
	 * @param problem the ints of the {@linkplain FlatProblem}
	 * @param problemLongs the longs of the {@linkplain FlatProblem}
	 * @param s the scratch of the work item, with the invariants of {@link FlatLayout#reset(int[], int)}
	 * @param sl the long scratch of the work item
	 * @param base the offset of the work item in the ints
	 * @param longBase the offset of the work item in the longs
	 * @param maxPackableCount the number of leading boxes of the permutation which may be placed
	 * @return the number of placed boxes, or -1 if there were more free points than the layout holds (the scratch is then reset)
	 */
	public static int searchPermutation(FlatLayout layout, int[] problem, long[] problemLongs, int[] s, long[] sl, int base, int longBase, int maxPackableCount) {
		final int types = problem[FlatProblem.TYPES];
		final int n = problem[FlatProblem.N];
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);

		final int permBase = base + layout.perm;
		final int rotationBase = base + layout.rotation;
		final int placedRotationBase = base + layout.placedRotation;
		final int bestRotationBase = base + layout.bestRotation;
		final int control = base + layout.control;
		final int minAreaBase = longBase + layout.minArea;
		final int minVolumeBase = longBase + layout.minVolume;
		final int placedVolumeBase = longBase + layout.placedVolume;
		final int length = maxPackableCount;

		sl[longBase + layout.bestVolume] = 0L;
		s[control + FlatLayout.C_BEST_COUNT] = 0;
		if(length == 0) {
			return 0;
		}

		// the smallest area of the boxes from each level on, in any rotation, and the smallest volume of all the boxes of the permutation from each level on
		long minArea = Long.MAX_VALUE;
		for (int i = length - 1; i >= 0; i--) {
			long area = problemLongs[FlatProblem.minAreas(types) + s[permBase + i]];
			if(area < minArea) {
				minArea = area;
			}
			sl[minAreaBase + i] = minArea;
		}
		long minVolume = Long.MAX_VALUE;
		for (int i = n - 1; i >= 0; i--) {
			long volume = problemLongs[s[permBase + i]];
			if(volume < minVolume) {
				minVolume = volume;
			}
			sl[minVolumeBase + i] = minVolume;
		}

		FlatPoints.clear(layout, s, base, problem[FlatProblem.CX], problem[FlatProblem.CY], problem[FlatProblem.CZ]);
		if(sl[minAreaBase] != 0L || sl[minVolumeBase] != 0L) {
			FlatPoints.filterMinimums(layout, s, base, 0, sl[minAreaBase], sl[minVolumeBase]);
		}
		sl[placedVolumeBase] = 0L;

		int bestCount = 0;
		int nodes = 0;
		int maxPoints = s[control + FlatLayout.C_MAX_POINTS];

		// one exit and no continue: the OpenCL backend does not lower a return, break or continue in a loop correctly
		int level = 0;
		boolean descend = true;
		boolean overflow = false;
		while (level >= 0 && !overflow) {
			boolean unwind;
			if(descend && level == length) {
				// every box is placed
				if(level > bestCount) {
					bestCount = offer(layout, s, sl, base, longBase, level);
				}
				unwind = true;
			} else if(!descend && bestCount == length) {
				// the best arrangement places all boxes: unwind without trying more rotations
				unwind = true;
			} else {
				if(descend) {
					s[rotationBase + level] = 0;
				}

				// the next rotation of the box of the level which has a position
				final int type = s[permBase + level];
				final int firstRotation = problem[rotationOffsets + type];
				final int rotationCount = problem[rotationOffsets + type + 1] - firstRotation;
				final int frame = base + layout.frameOffset(level);

				int rotationIndex = s[rotationBase + level];
				int pointIndex = -1;
				int dx = 0;
				int dy = 0;
				int dz = 0;
				while (pointIndex == -1 && rotationIndex < rotationCount) {
					int rotation = rotationValues + 3 * (firstRotation + rotationIndex);
					dx = problem[rotation];
					dy = problem[rotation + 1];
					dz = problem[rotation + 2];
					rotationIndex++;
					pointIndex = bestPoint(s, frame, dx, dy, dz);
				}
				s[rotationBase + level] = rotationIndex;

				if(pointIndex != -1) {
					// place the box and continue with the next level
					int point = frame + 1 + pointIndex * POINT_INTS;
					int x = s[point];
					int y = s[point + 1];
					int z = s[point + 2];
					FlatPoints.setPlacement(layout, s, base, level, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
					int count = FlatPoints.add(layout, s, base, level, pointIndex, level, sl[minAreaBase + level], sl[minVolumeBase + level]);
					if(count < 0) {
						overflow = true;
					} else {
						if(count > maxPoints) {
							maxPoints = count;
						}
						int nextLevel = level + 1;
						if(nextLevel < length) {
							long nextArea = sl[minAreaBase + nextLevel];
							long nextVolume = sl[minVolumeBase + nextLevel];
							if(nextArea != sl[minAreaBase + level] || nextVolume != sl[minVolumeBase + level]) {
								FlatPoints.filterMinimums(layout, s, base, nextLevel, nextArea, nextVolume);
							}
						}
						s[placedRotationBase + level] = rotationIndex - 1;
						sl[placedVolumeBase + nextLevel] = sl[placedVolumeBase + level] + problemLongs[type];
						nodes++;
						level = nextLevel;
						descend = true;
					}
					unwind = false;
				} else {
					// no rotation has a position: the arrangement ends at this box
					if(level > bestCount) {
						bestCount = offer(layout, s, sl, base, longBase, level);
					}
					unwind = true;
				}
			}
			if(unwind) {
				level--;
				descend = false;
			}
		}

		if(overflow) {
			layout.reset(s, base);
			s[control + FlatLayout.C_OVERFLOW] = 1;
			return -1;
		}
		s[control + FlatLayout.C_BEST_COUNT] = bestCount;
		s[control + FlatLayout.C_NODES] += nodes;
		s[control + FlatLayout.C_MAX_POINTS] = maxPoints;
		return bestCount;
	}

	private static int offer(FlatLayout layout, int[] s, long[] sl, int base, int longBase, int count) {
		sl[longBase + layout.bestVolume] = sl[longBase + layout.placedVolume + count];
		int from = base + layout.placedRotation;
		int to = base + layout.bestRotation;
		for (int i = 0; i < count; i++) {
			s[to + i] = s[from + i];
		}
		return count;
	}

	/**
	 * The best free point of the frame for a box of the size, by the default point comparator of the brute force packager: the scan order matters,
	 * as the comparator is not symmetric. A point is better than the best so far unless it has less area, or a higher z, or the same area and less volume.
	 *
	 * @param frame the offset of the frame
	 * @return the index of the point in the frame, or -1 if the box does not fit any point
	 */
	public static int bestPoint(int[] s, int frame, int dx, int dy, int dz) {
		final int size = s[frame];
		final int values = frame + 1;

		int best = -1;
		long bestArea = 0L;
		int bestMinZ = 0;
		for (int k = 0; k < size; k++) {
			int p = values + k * POINT_INTS;
			if(FlatGeometry.fits3D(s, p, dx, dy, dz)) {
				long area = FlatGeometry.area(s, p);
				// the candidate is not better if it has less area, or a higher z, or the same area and less volume
				if(best == -1 || !(bestArea < area || bestMinZ < s[p + MIN_Z] || (bestArea == area && FlatGeometry.volume(s, values + best * POINT_INTS) < FlatGeometry.volume(s, p)))) {
					best = k;
					bestArea = area;
					bestMinZ = s[p + MIN_Z];
				}
			}
		}
		return best;
	}
}
