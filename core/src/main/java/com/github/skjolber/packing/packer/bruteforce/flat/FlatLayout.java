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
	public static final int CONTROL_INTS = 12;

	/** the maximum number of placements (levels) */
	public final int n;
	/** the maximum number of free points in a frame */
	public final int pMax;
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

	/** the number of ints in the scratch */
	public final int size;

	/** long scratch */
	public final int minArea;
	public final int minVolume;
	public final int placedVolume;
	public final int bestVolume;
	public final int longSize;

	public FlatLayout(int n, int pMax) {
		if(n < 0 || n > MAX_PLACEMENTS) {
			throw new IllegalArgumentException("Expected 0 <= n <= " + MAX_PLACEMENTS + ", got " + n);
		}
		if(pMax < 1) {
			throw new IllegalArgumentException("Expected at least one point per frame");
		}
		this.n = n;
		this.pMax = pMax;
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
