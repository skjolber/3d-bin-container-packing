package com.github.skjolber.packing.accelerator;

import static com.github.skjolber.packing.accelerator.FlatLayout.PLACEMENT_INTS;
import static com.github.skjolber.packing.accelerator.FlatLayout.POINT_INTS;
import static com.github.skjolber.packing.accelerator.FlatLayout.SPACE_INTS;

/**
 * The free space of the nodes of the path of a tree item ({@link FlatTree}), as maximal empty spaces, on the scratch of the work item: a port of
 * {@code BruteForceTreeFreeSpace}. The boxes which fit none of the spaces of a node, in any rotation, can never be placed below it, since the free
 * space only shrinks; the free points cannot tell this (a later placement can add a point no earlier point covered, as a bar across two boxes), the
 * maximal spaces can: a box fits the free space if and only if it fits a maximal space.
 * <p>
 * The answer for a type at a level is kept, with a space which holds the type (its witness): at the level below, the type still fits if the box
 * placed between misses the witness, and does not fit if it did not fit above. A space of the nearest level computed above which holds the type and
 * misses the boxes placed since is a witness, too. The spaces of a level are computed only when some type must be looked for in them, from the
 * level above and the box placed there (the placement table of the scratch), keeping the pieces which hold some box remaining below the level. A
 * level with more than {@link FlatLayout#MAX_SPACES} spaces has an unknown free space, in which every box is taken to fit.
 * <p>
 * The methods are static and use primitive arrays only, in the style of the flat package, so that the kernel can run them.
 */
public final class FlatSpace {

	/** the answers kept per level and type: not known without looking, fits, fits nothing */
	public static final int UNKNOWN = 0;
	public static final int FITS = 1;
	public static final int NONE = 2;

	private FlatSpace() {
	}

	/** Start over: the first level's one space is the container, with all boxes remaining below it. */
	public static void clear(FlatLayout layout, int[] problem, int[] s, int base, int dx, int dy, int dz) {
		final int types = problem[FlatProblem.TYPES];
		int space = base + layout.spaces;
		s[space] = 0;
		s[space + 1] = 0;
		s[space + 2] = 0;
		s[space + 3] = dx;
		s[space + 4] = dy;
		s[space + 5] = dz;
		s[base + layout.spaceSizes] = 1;
		for (int t = 0; t < types; t++) {
			s[base + layout.spaceRemaining + t] = problem[FlatProblem.HEADER + t];
		}
		s[base + layout.control + FlatLayout.C_SPACE_VALID] = 1;
		s[base + layout.control + FlatLayout.C_SPACE_KNOWN_VALID] = 0;
	}

	/** A box placed at the level (in the placement table): the spaces and answers of the levels below are of another path now. */
	public static void place(FlatLayout layout, int[] s, int base, int level) {
		int control = base + layout.control;
		if(s[control + FlatLayout.C_SPACE_VALID] > level + 1) {
			s[control + FlatLayout.C_SPACE_VALID] = level + 1;
		}
		if(s[control + FlatLayout.C_SPACE_KNOWN_VALID] > level + 1) {
			s[control + FlatLayout.C_SPACE_KNOWN_VALID] = level + 1;
		}
	}

	/**
	 * @return whether the type fits the free space of the level's node, as far as known without looking at its spaces: by the answer above, when
	 *         the box placed above misses the type's witness
	 */
	public static int known(FlatLayout layout, int[] problem, int[] s, int base, int level, int type) {
		final int types = problem[FlatProblem.TYPES];
		final int control = base + layout.control;
		final int knownBase = base + layout.known;
		while (s[control + FlatLayout.C_SPACE_KNOWN_VALID] <= level) {
			int row = knownBase + s[control + FlatLayout.C_SPACE_KNOWN_VALID] * layout.types;
			for (int t = 0; t < types; t++) {
				s[row + t] = UNKNOWN;
			}
			s[control + FlatLayout.C_SPACE_KNOWN_VALID]++;
		}
		int k = knownBase + level * layout.types + type;
		int result = s[k];
		if(result == UNKNOWN && level > 0) {
			int above = knownBase + (level - 1) * layout.types + type;
			if(s[above] == NONE) {
				// the free space only shrinks
				s[k] = NONE;
				result = NONE;
			} else if(s[above] == FITS) {
				int w = base + layout.witness + ((level - 1) * layout.types + type) * SPACE_INTS;
				int b = base + layout.placements + (level - 1) * PLACEMENT_INTS;
				if(s[w + 3] <= s[b] || s[w] > s[b + 3] || s[w + 4] <= s[b + 1] || s[w + 1] > s[b + 4] || s[w + 5] <= s[b + 2] || s[w + 2] > s[b + 5]) {
					// the box placed above misses the witness, which is still empty
					int to = base + layout.witness + (level * layout.types + type) * SPACE_INTS;
					for (int i = 0; i < SPACE_INTS; i++) {
						s[to + i] = s[w + i];
					}
					s[k] = FITS;
					result = FITS;
				}
			}
		}
		return result;
	}

	/** The type fits the level's free space at an empty region found elsewhere (a free point): kept as its witness. */
	public static void fitsAt(FlatLayout layout, int[] s, int base, int level, int type, int x, int y, int z, int dx, int dy, int dz) {
		int w = base + layout.witness + (level * layout.types + type) * SPACE_INTS;
		s[w] = x;
		s[w + 1] = y;
		s[w + 2] = z;
		s[w + 3] = x + dx;
		s[w + 4] = y + dy;
		s[w + 5] = z + dz;
		s[base + layout.known + level * layout.types + type] = FITS;
	}

	/** @return whether the space misses the boxes placed at the levels from one level to another (exclusive); the placements have inclusive ends */
	private static boolean misses(FlatLayout layout, int[] s, int base, int space, int from, int to) {
		boolean result = true;
		for (int level = from; level < to && result; level++) {
			int b = base + layout.placements + level * PLACEMENT_INTS;
			if(s[space + 3] > s[b] && s[space] <= s[b + 3] && s[space + 4] > s[b + 1] && s[space + 1] <= s[b + 4] && s[space + 5] > s[b + 2] && s[space + 2] <= s[b + 5]) {
				result = false;
			}
		}
		return result;
	}

	/** @return whether a space of the given size holds the type in some rotation */
	private static boolean holds(int[] problem, int rotationOffsets, int rotationValues, int type, int dx, int dy, int dz) {
		boolean result = false;
		int end = problem[rotationOffsets + type + 1];
		for (int r = problem[rotationOffsets + type]; r < end && !result; r++) {
			int rotation = rotationValues + 3 * r;
			if(dx >= problem[rotation] && dy >= problem[rotation + 1] && dz >= problem[rotation + 2]) {
				result = true;
			}
		}
		return result;
	}

	/** @return whether the type fits the free space of the level's node in any of its rotations, looking at its spaces when not known */
	public static boolean fits(FlatLayout layout, int[] problem, long[] problemLongs, int[] s, int base, int level, int type) {
		final int types = problem[FlatProblem.TYPES];
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);
		final int control = base + layout.control;
		int answer = known(layout, problem, s, base, level, type);
		boolean result = answer == FITS;
		if(answer == UNKNOWN) {
			final int k = base + layout.known + level * layout.types + type;
			final int w = base + layout.witness + (level * layout.types + type) * SPACE_INTS;
			boolean found = false;
			int valid = s[control + FlatLayout.C_SPACE_VALID];
			if(valid <= level && s[base + layout.spaceSizes + valid - 1] != -1) {
				// a space of the nearest level computed above which holds the type and misses the boxes placed since is still empty: a witness
				// without computing the level's spaces
				int above = valid - 1;
				int spaces = base + layout.spaces + above * FlatLayout.MAX_SPACES * SPACE_INTS;
				int size = s[base + layout.spaceSizes + above];
				for (int i = 0; i < size * SPACE_INTS && !found; i += SPACE_INTS) {
					int space = spaces + i;
					if(holds(problem, rotationOffsets, rotationValues, type, s[space + 3] - s[space], s[space + 4] - s[space + 1], s[space + 5] - s[space + 2])
							&& misses(layout, s, base, space, above, level)) {
						for (int j = 0; j < SPACE_INTS; j++) {
							s[w + j] = s[space + j];
						}
						found = true;
					}
				}
			}
			if(!found) {
				while (s[control + FlatLayout.C_SPACE_VALID] <= level) {
					split(layout, problem, problemLongs, s, base, s[control + FlatLayout.C_SPACE_VALID] - 1);
					s[control + FlatLayout.C_SPACE_VALID]++;
				}
				int size = s[base + layout.spaceSizes + level];
				if(size == -1) {
					// unknown: taken to fit, with the whole container as the witness, so that the levels below look again
					int container = base + layout.spaces;
					for (int j = 0; j < SPACE_INTS; j++) {
						s[w + j] = s[container + j];
					}
					found = true;
				} else {
					int spaces = base + layout.spaces + level * FlatLayout.MAX_SPACES * SPACE_INTS;
					for (int i = 0; i < size * SPACE_INTS && !found; i += SPACE_INTS) {
						int space = spaces + i;
						if(holds(problem, rotationOffsets, rotationValues, type, s[space + 3] - s[space], s[space + 4] - s[space + 1], s[space + 5] - s[space + 2])) {
							for (int j = 0; j < SPACE_INTS; j++) {
								s[w + j] = s[space + j];
							}
							found = true;
						}
					}
				}
			}
			s[k] = found ? FITS : NONE;
			result = found;
		}
		return result;
	}

	/** @return whether the space holds some box remaining at the level, in some rotation */
	private static boolean holdsAny(int[] problem, int[] s, int remaining, int types, int rotationOffsets, int rotationValues, int dx, int dy, int dz, int pieceDx, int pieceDy, int pieceDz,
			long pieceVolume) {
		boolean result = false;
		if(dx >= pieceDx && dy >= pieceDy && dz >= pieceDz && (long)dx * dy * dz >= pieceVolume) {
			for (int t = 0; t < types && !result; t++) {
				if(s[remaining + t] > 0 && holds(problem, rotationOffsets, rotationValues, t, dx, dy, dz)) {
					result = true;
				}
			}
		}
		return result;
	}

	/** The spaces of the level below the parent: the parent's spaces split by the box placed at the parent, less the ones within others. */
	private static void split(FlatLayout layout, int[] problem, long[] problemLongs, int[] s, int base, int parent) {
		final int types = problem[FlatProblem.TYPES];
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);
		final int child = parent + 1;
		final int left = base + layout.spaceRemaining + child * layout.types;
		final int parentLeft = base + layout.spaceRemaining + parent * layout.types;
		for (int t = 0; t < types; t++) {
			s[left + t] = s[parentLeft + t];
		}
		s[left + s[base + layout.placedType + parent]]--;
		final int parentSize = s[base + layout.spaceSizes + parent];
		if(parentSize == -1) {
			s[base + layout.spaceSizes + child] = -1;
		} else {
			// the least a piece must hold
			int pieceDx = Integer.MAX_VALUE;
			int pieceDy = Integer.MAX_VALUE;
			int pieceDz = Integer.MAX_VALUE;
			long pieceVolume = Long.MAX_VALUE;
			for (int t = 0; t < types; t++) {
				if(s[left + t] > 0) {
					int end = problem[rotationOffsets + t + 1];
					for (int r = problem[rotationOffsets + t]; r < end; r++) {
						int rotation = rotationValues + 3 * r;
						pieceDx = Math.min(pieceDx, problem[rotation]);
						pieceDy = Math.min(pieceDy, problem[rotation + 1]);
						pieceDz = Math.min(pieceDz, problem[rotation + 2]);
					}
					pieceVolume = Math.min(pieceVolume, problemLongs[t]);
				}
			}
			final int p = base + layout.spaces + parent * FlatLayout.MAX_SPACES * SPACE_INTS;
			final int c = base + layout.spaces + child * FlatLayout.MAX_SPACES * SPACE_INTS;
			final int b = base + layout.placements + parent * PLACEMENT_INTS;
			final int bMinX = s[b];
			final int bMinY = s[b + 1];
			final int bMinZ = s[b + 2];
			final int bMaxX = s[b + 3] + 1;
			final int bMaxY = s[b + 4] + 1;
			final int bMaxZ = s[b + 5] + 1;
			// the spaces the box does not touch stay, and stay maximal
			int count = 0;
			for (int i = 0; i < parentSize * SPACE_INTS; i += SPACE_INTS) {
				if(s[p + i + 3] <= bMinX || s[p + i] >= bMaxX || s[p + i + 4] <= bMinY || s[p + i + 1] >= bMaxY || s[p + i + 5] <= bMinZ || s[p + i + 2] >= bMaxZ) {
					for (int j = 0; j < SPACE_INTS; j++) {
						s[c + count * SPACE_INTS + j] = s[p + i + j];
					}
					count++;
				}
			}
			final int kept = count;
			// the others, cut by the box: up to six pieces each, at the sides of the box
			for (int i = 0; i < parentSize * SPACE_INTS && count != -1; i += SPACE_INTS) {
				if(!(s[p + i + 3] <= bMinX || s[p + i] >= bMaxX || s[p + i + 4] <= bMinY || s[p + i + 1] >= bMaxY || s[p + i + 5] <= bMinZ || s[p + i + 2] >= bMaxZ)) {
					int minX = s[p + i];
					int minY = s[p + i + 1];
					int minZ = s[p + i + 2];
					int maxX = s[p + i + 3];
					int maxY = s[p + i + 4];
					int maxZ = s[p + i + 5];
					if(bMinX > minX) {
						count = add(problem, s, c, count, kept, left, types, rotationOffsets, rotationValues, minX, minY, minZ, bMinX, maxY, maxZ, pieceDx, pieceDy, pieceDz, pieceVolume);
					}
					if(bMaxX < maxX) {
						count = add(problem, s, c, count, kept, left, types, rotationOffsets, rotationValues, bMaxX, minY, minZ, maxX, maxY, maxZ, pieceDx, pieceDy, pieceDz, pieceVolume);
					}
					if(bMinY > minY) {
						count = add(problem, s, c, count, kept, left, types, rotationOffsets, rotationValues, minX, minY, minZ, maxX, bMinY, maxZ, pieceDx, pieceDy, pieceDz, pieceVolume);
					}
					if(bMaxY < maxY) {
						count = add(problem, s, c, count, kept, left, types, rotationOffsets, rotationValues, minX, bMaxY, minZ, maxX, maxY, maxZ, pieceDx, pieceDy, pieceDz, pieceVolume);
					}
					if(bMinZ > minZ) {
						count = add(problem, s, c, count, kept, left, types, rotationOffsets, rotationValues, minX, minY, minZ, maxX, maxY, bMinZ, pieceDx, pieceDy, pieceDz, pieceVolume);
					}
					if(bMaxZ < maxZ) {
						count = add(problem, s, c, count, kept, left, types, rotationOffsets, rotationValues, minX, minY, bMaxZ, maxX, maxY, maxZ, pieceDx, pieceDy, pieceDz, pieceVolume);
					}
				}
			}
			s[base + layout.spaceSizes + child] = count;
		}
	}

	/**
	 * Add a piece to the level's spaces (at the offset c), unless it holds no remaining box or is within one of them; the pieces within it are
	 * removed (a kept space, maximal above, is never within a piece).
	 *
	 * @return the number of spaces, or -1 for too many
	 */
	private static int add(int[] problem, int[] s, int c, int count, int kept, int remaining, int types, int rotationOffsets, int rotationValues, int minX, int minY, int minZ, int maxX,
			int maxY, int maxZ, int pieceDx, int pieceDy, int pieceDz, long pieceVolume) {
		int result = count;
		if(count != -1 && holdsAny(problem, s, remaining, types, rotationOffsets, rotationValues, maxX - minX, maxY - minY, maxZ - minZ, pieceDx, pieceDy, pieceDz, pieceVolume)) {
			boolean within = false;
			for (int i = 0; i < kept * SPACE_INTS && !within; i += SPACE_INTS) {
				if(s[c + i] <= minX && s[c + i + 1] <= minY && s[c + i + 2] <= minZ && maxX <= s[c + i + 3] && maxY <= s[c + i + 4] && maxZ <= s[c + i + 5]) {
					within = true;
				}
			}
			int i = kept * SPACE_INTS;
			while (i < result * SPACE_INTS && !within) {
				if(s[c + i] <= minX && s[c + i + 1] <= minY && s[c + i + 2] <= minZ && maxX <= s[c + i + 3] && maxY <= s[c + i + 4] && maxZ <= s[c + i + 5]) {
					// within a piece
					within = true;
				} else if(minX <= s[c + i] && minY <= s[c + i + 1] && minZ <= s[c + i + 2] && s[c + i + 3] <= maxX && s[c + i + 4] <= maxY && s[c + i + 5] <= maxZ) {
					// a piece within it: replaced by the last
					result--;
					for (int j = 0; j < SPACE_INTS; j++) {
						s[c + i + j] = s[c + result * SPACE_INTS + j];
					}
				} else {
					i += SPACE_INTS;
				}
			}
			if(!within) {
				if(result == FlatLayout.MAX_SPACES) {
					result = -1;
				} else {
					int offset = c + result * SPACE_INTS;
					s[offset] = minX;
					s[offset + 1] = minY;
					s[offset + 2] = minZ;
					s[offset + 3] = maxX;
					s[offset + 4] = maxY;
					s[offset + 5] = maxZ;
					result++;
				}
			}
		}
		return result;
	}

	/**
	 * Whether the prefix (its volume, weight and count at the level) plus the remaining boxes of the types which fit the node's free space, within
	 * the load left, can beat neither the bound volume nor the best: the types which fit no maximal space can never be placed below the node. With
	 * every remaining type fitting it is no tighter than the bound table, and false.
	 * <p>
	 * The types are taken in the type order, each by the answer kept from the level above, else by a free point of the level's frame which holds it
	 * (an empty region, so the box fits); the node is viable, and the rest is not looked at, as soon as the types which fit add up to more than the
	 * best. The types which fit no free point are looked for in the maximal spaces.
	 */
	public static boolean isHopeless(FlatLayout layout, int[] problem, long[] problemLongs, int[] s, int base, int level, int remainingBase, long volume, long weight, long maxLoadVolume,
			long maxLoadWeight, long boundVolume, int beatCount, long beatVolume, long beatWeight, boolean anyBest) {
		final int types = problem[FlatProblem.TYPES];
		final int rotations = problem[FlatProblem.ROTATIONS];
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);
		final int typeOrder = FlatProblem.typeOrder(types, rotations);
		final int weights = FlatProblem.weights(types);
		final int frame = base + layout.frameOffset(level);
		final int points = s[frame];
		// the best to beat, by volume: the node is viable once the types which fit add up to more
		final long beat = beatCount > 0 ? beatVolume : -1L;
		final long loadLeft = maxLoadVolume - volume;
		long v = 0L;
		long w = 0L;
		int c = 0;
		boolean excluded = false;
		boolean pointless = false;
		boolean viable = false;
		for (int i = 0; i < types && !viable; i++) {
			int t = problem[typeOrder + i];
			if(s[remainingBase + t] > 0) {
				int known = known(layout, problem, s, base, level, t);
				boolean counts = true;
				if(known == UNKNOWN) {
					// a free point of the level which holds the type is an empty region: the type fits
					int point = -1;
					int end = problem[rotationOffsets + t + 1];
					for (int k = 0; k < points && point == -1; k++) {
						int p = frame + 1 + k * POINT_INTS;
						for (int r = problem[rotationOffsets + t]; r < end && point == -1; r++) {
							int rotation = rotationValues + 3 * r;
							if(FlatGeometry.fits3D(s, p, problem[rotation], problem[rotation + 1], problem[rotation + 2])) {
								point = p;
							}
						}
					}
					if(point == -1) {
						pointless = true;
						counts = false;
					} else {
						fitsAt(layout, s, base, level, t, s[point], s[point + 1], s[point + 2], FlatGeometry.dx(s, point), FlatGeometry.dy(s, point), FlatGeometry.dz(s, point));
					}
				} else if(known == NONE) {
					excluded = true;
					counts = false;
				}
				if(counts) {
					v += s[remainingBase + t] * problemLongs[t];
					w += s[remainingBase + t] * problemLongs[weights + t];
					c += s[remainingBase + t];
					long partial = volume + (v < loadLeft ? v : loadLeft);
					if(partial >= boundVolume && partial > beat) {
						viable = true;
					}
				}
			}
		}
		if(pointless && !viable) {
			for (int i = 0; i < types && !viable; i++) {
				int t = problem[typeOrder + i];
				if(s[remainingBase + t] > 0 && known(layout, problem, s, base, level, t) == UNKNOWN) {
					if(!fits(layout, problem, problemLongs, s, base, level, t)) {
						excluded = true;
					} else {
						v += s[remainingBase + t] * problemLongs[t];
						w += s[remainingBase + t] * problemLongs[weights + t];
						c += s[remainingBase + t];
						long partial = volume + (v < loadLeft ? v : loadLeft);
						if(partial >= boundVolume && partial > beat) {
							viable = true;
						}
					}
				}
			}
		}
		boolean result = false;
		if(!viable && excluded) {
			v = volume + (v < loadLeft ? v : loadLeft);
			long weightLeft = maxLoadWeight - weight;
			w = weight + (w < weightLeft ? w : weightLeft);
			c += level;
			if(v < boundVolume) {
				result = true;
			} else if(beatCount > 0) {
				int compare;
				if(v != beatVolume) {
					compare = v < beatVolume ? -1 : 1;
				} else if(w != beatWeight) {
					compare = w < beatWeight ? -1 : 1;
				} else if(c != beatCount) {
					compare = c < beatCount ? -1 : 1;
				} else {
					compare = 0;
				}
				result = compare < (anyBest ? 1 : 0);
			}
		}
		return result;
	}
}
