package com.github.skjolber.packing.packer.bruteforce.flat;

/**
 * Offsets into the scratch arrays of one flat brute-force work item: the free points, placements and search state, in one
 * {@code int[]} (and the volumes in one {@code long[]}), so that the same code can run for many work items side by side
 * (and, as primitive-array code, on an accelerator).
 * <p>
 * A free point is {@link #POINT_INTS} ints: min x, min y, min z, max x, max y, max z and the supporting planes. The planes
 * are packed as three 10-bit placement ids (yz, xz, xy; {@link #PLANE_NONE} for none; the container is the id {@link #n}).
 */
public final class FlatLayout {

	public static final int POINT_INTS = 7;
	public static final int PLANE_NONE = 1023;
	public static final int PLACEMENT_INTS = 6;
	public static final int MAX_PLACEMENTS = 1000;
	/** a maximal empty space of the free space: min x, y, z (inclusive) and max x, y, z (exclusive) */
	public static final int SPACE_INTS = 6;
	/** the most maximal spaces of a level; a level with more has an unknown free space */
	public static final int MAX_SPACES = 48;

	/** control slots */
	public static final int C_OVERFLOW = 0;
	public static final int C_MOVE_X = 1;
	public static final int C_MOVE_Y = 2;
	public static final int C_MOVE_Z = 3;
	public static final int C_POOL_X = 4;
	public static final int C_POOL_Y = 5;
	public static final int C_POOL_Z = 6;
	public static final int C_BEST_COUNT = 7;
	public static final int C_NODES = 8;
	public static final int C_MAX_POINTS = 9;
	/** the tree search ({@link FlatTree}): the level of the search, whether it is about to expand the node of the level, and the key of the remaining boxes */
	public static final int C_TREE_LEVEL = 10;
	public static final int C_TREE_DESCEND = 11;
	public static final int C_TREE_KEY = 12;
	/** the free space of the tree search ({@link FlatSpace}): the levels with spaces and with answers of the current path, the checks and cuts made, and whether it was given up */
	public static final int C_SPACE_VALID = 13;
	public static final int C_SPACE_KNOWN_VALID = 14;
	public static final int C_SPACE_CHECKS = 15;
	public static final int C_SPACE_CUTS = 16;
	public static final int C_SPACE_OFF = 17;
	public static final int CONTROL_INTS = 20;

	/** the maximum number of placements (levels) */
	public final int n;
	/** the maximum number of free points in a frame */
	public final int pMax;
	/** the number of box types (at least one): the stride of the arrays per type, which can be more than the boxes when some types have no box left */
	public final int types;
	/** the number of ints of a frame: the point count, then the points and a spare point, which is the scratch of the merge */
	public final int frameInts;

	public final int control;
	/** placements: x, y, z, end x, end y, end z; the container has the id {@link #n} */
	public final int placements;
	public final int frames;
	public final int flags;
	public final int moveX;
	public final int moveY;
	public final int moveZ;
	/** the points which were moved in a direction, in the order they were generated */
	public final int poolX;
	public final int poolY;
	public final int poolZ;
	/** the next pool entry at the same insertion index, or -1 */
	public final int nextX;
	public final int nextY;
	public final int nextZ;
	public final int headX;
	public final int headY;
	public final int headZ;
	public final int tailX;
	public final int tailY;
	public final int tailZ;
	/** the constrained max coordinate of the copy of a point, or -1 */
	public final int copyX;
	public final int copyY;
	public final int copyZ;

	/** search state, per level */
	public final int perm;
	public final int rotation;
	public final int placedRotation;
	public final int bestRotation;
	/** the tree search ({@link FlatTree}), per level: the type being tried, whether a rotation of it was placed, and the type placed */
	public final int typeAt;
	public final int fit;
	public final int placedType;
	/** the tree search: the number of boxes of each type still to place (one slot per type) */
	public final int remaining;
	/** the free space ({@link FlatSpace}), per level: the maximal spaces ({@link #MAX_SPACES} of {@link #SPACE_INTS} each), their number (or -1 for unknown), the boxes remaining below the level by type, the answer per type and its witness space, and whether the node was checked */
	public final int spaces;
	public final int spaceSizes;
	public final int spaceRemaining;
	public final int known;
	public final int witness;
	public final int spaceChecked;

	/** the number of ints in the scratch */
	public final int size;

	/** long scratch */
	public final int minArea;
	public final int minVolume;
	public final int placedVolume;
	public final int bestVolume;
	/** the tree search, per level: the weight of the placed boxes */
	public final int placedWeight;
	public final int longSize;

	/** A layout for as many types as boxes. */
	public FlatLayout(int n, int pMax) {
		this(n, pMax, n);
	}

	/**
	 * @param n the most placements (levels)
	 * @param pMax the most free points in a frame
	 * @param types the number of box types of the problem (the stride of the arrays per type)
	 */
	public FlatLayout(int n, int pMax, int types) {
		if(n < 0 || n > MAX_PLACEMENTS) {
			throw new IllegalArgumentException("Expected 0 <= n <= " + MAX_PLACEMENTS + ", got " + n);
		}
		if(pMax < 1) {
			throw new IllegalArgumentException("Expected at least one point per frame");
		}
		this.n = n;
		this.pMax = pMax;
		this.types = Math.max(1, types);
		this.frameInts = 1 + (pMax + 1) * POINT_INTS;

		int offset = 0;
		control = offset;
		offset += CONTROL_INTS;
		placements = offset;
		offset += (n + 1) * PLACEMENT_INTS;
		frames = offset;
		offset += (n + 1) * frameInts;
		flags = offset;
		offset += pMax + 1;
		moveX = offset;
		offset += pMax;
		moveY = offset;
		offset += pMax;
		moveZ = offset;
		offset += pMax;
		poolX = offset;
		offset += pMax * POINT_INTS;
		poolY = offset;
		offset += pMax * POINT_INTS;
		poolZ = offset;
		offset += pMax * POINT_INTS;
		nextX = offset;
		offset += pMax;
		nextY = offset;
		offset += pMax;
		nextZ = offset;
		offset += pMax;
		headX = offset;
		offset += pMax + 1;
		headY = offset;
		offset += pMax + 1;
		headZ = offset;
		offset += pMax + 1;
		tailX = offset;
		offset += pMax + 1;
		tailY = offset;
		offset += pMax + 1;
		tailZ = offset;
		offset += pMax + 1;
		copyX = offset;
		offset += pMax;
		copyY = offset;
		offset += pMax;
		copyZ = offset;
		offset += pMax;
		perm = offset;
		offset += n + 1;
		rotation = offset;
		offset += n + 1;
		placedRotation = offset;
		offset += n + 1;
		bestRotation = offset;
		offset += n + 1;
		typeAt = offset;
		offset += n + 1;
		fit = offset;
		offset += n + 1;
		placedType = offset;
		offset += n + 1;
		remaining = offset;
		offset += this.types;
		spaces = offset;
		offset += (n + 1) * MAX_SPACES * SPACE_INTS;
		spaceSizes = offset;
		offset += n + 1;
		spaceRemaining = offset;
		offset += (n + 1) * this.types;
		known = offset;
		offset += (n + 1) * this.types;
		witness = offset;
		offset += (n + 1) * this.types * SPACE_INTS;
		spaceChecked = offset;
		offset += n + 1;
		size = offset;

		int longOffset = 0;
		minArea = longOffset;
		longOffset += n + 1;
		minVolume = longOffset;
		longOffset += n + 1;
		placedVolume = longOffset;
		longOffset += n + 1;
		bestVolume = longOffset;
		longOffset += 1;
		placedWeight = longOffset;
		longOffset += n + 1;
		longSize = longOffset;
	}

	/** @return a scratch with the invariants which the point functions expect: no flags, no chains, no copies */
	public int[] newScratch() {
		int[] s = new int[size];
		reset(s, 0);
		return s;
	}

	/** Restore the invariants of a scratch at the base: all chain heads are empty, all copies are absent, no point is flagged. */
	public void reset(int[] s, int b) {
		for (int i = 0; i <= pMax; i++) {
			s[b + flags + i] = 0;
			s[b + headX + i] = -1;
			s[b + headY + i] = -1;
			s[b + headZ + i] = -1;
			s[b + tailX + i] = -1;
			s[b + tailY + i] = -1;
			s[b + tailZ + i] = -1;
		}
		for (int i = 0; i < pMax; i++) {
			s[b + copyX + i] = -1;
			s[b + copyY + i] = -1;
			s[b + copyZ + i] = -1;
		}
		for (int i = 0; i < CONTROL_INTS; i++) {
			s[b + control + i] = 0;
		}
	}

	public int frameOffset(int level) {
		return frames + level * frameInts;
	}

	public static int yz(int planes) {
		return planes & PLANE_NONE;
	}

	public static int xz(int planes) {
		return (planes >> 10) & PLANE_NONE;
	}

	public static int xy(int planes) {
		return (planes >> 20) & PLANE_NONE;
	}

	public static int planes(int yz, int xz, int xy) {
		return yz | (xz << 10) | (xy << 20);
	}
}
