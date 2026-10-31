package com.github.skjolber.packing.packer.bruteforce.tree;

import java.util.Arrays;

import com.github.skjolber.packing.api.BoxStackValue;

/**
 * The free space of the nodes of the path being searched, as maximal empty spaces: the boxes which fit none of them, in any rotation, can never
 * be placed below the node, since the free space only shrinks. Unlike the free points (which a later placement can add to, as with a bar across
 * two boxes), the maximal spaces cover the free space entirely: a box fits the free space if and only if it fits a maximal space.
 * <p>
 * The answer for a type at a node is kept, with a space which holds the type (its witness): at the node below, the type still fits if the box
 * placed between misses the witness, and does not fit if it did not fit above. A space of the nearest level computed above which holds the type and
 * misses the boxes placed since is a witness, too. The spaces of a level are computed only when some type must be looked for in them, from the
 * level above and the box placed there, keeping the pieces which hold some box remaining below the level: fewer remain at each level, so a piece
 * dropped at a level would be dropped below it too.
 */
final class BruteForceTreeFreeSpace {

	/** a level with more spaces than this has an unknown free space, in which every box is taken to fit */
	static final int MAX_SPACES = 256;
	private static final int STRIDE = 6;
	/** the answers of {@link #known}: not known without looking, fits, fits nothing */
	static final byte UNKNOWN = 0;
	static final byte FITS = 1;
	static final byte NONE = 2;

	/** per level, the maximal spaces: min x, y, z (inclusive) and max x, y, z (exclusive) */
	private final int[][] spaces;
	/** per level, the number of spaces, or -1 for unknown */
	private final int[] sizes;
	/** per level, the box placed at the level, as a space, and its type */
	private final int[] boxes;
	private final int[] placedType;
	/** per level, the boxes remaining to place below it, by type */
	private final int[][] remaining;
	/** the levels below this have the spaces of the current path */
	private int valid;
	/** per level and type, whether the type fits the level's free space, and for one which does a space which holds it */
	private final byte[][] known;
	private final int[][] witness;
	/** the levels below this have the answers of the current path */
	private int knownValid;
	/** per type, the rotations */
	private BoxStackValue[][] stackValues;
	private int types;
	/** the least a piece must hold to be kept, for the split in progress: the smallest sides and volume of the boxes remaining below */
	private int pieceDx;
	private int pieceDy;
	private int pieceDz;
	private long pieceVolume;

	/**
	 * @param levels the number of levels, i.e. of boxes
	 * @param dx the container's load space
	 * @param counts the boxes by type
	 * @param stackValues per type, the rotations
	 */
	BruteForceTreeFreeSpace(int levels, int dx, int dy, int dz, int[] counts, BoxStackValue[][] stackValues) {
		this.spaces = new int[levels + 1][];
		this.sizes = new int[levels + 1];
		this.boxes = new int[levels * STRIDE];
		this.placedType = new int[levels];
		this.remaining = new int[levels + 1][];
		this.known = new byte[levels + 1][];
		this.witness = new int[levels + 1][];
		this.types = counts.length;
		spaces[0] = new int[STRIDE];
		reset(levels, dx, dy, dz, counts, stackValues);
	}

	/**
	 * Start over for another search, keeping the arrays.
	 *
	 * @return false if the levels do not fit: then a new free space is needed
	 */
	boolean reset(int levels, int dx, int dy, int dz, int[] counts, BoxStackValue[][] stackValues) {
		if(levels + 1 > spaces.length) {
			return false;
		}
		if(counts.length != types) {
			Arrays.fill(known, null);
			Arrays.fill(witness, null);
			Arrays.fill(remaining, null);
			types = counts.length;
		}
		this.stackValues = stackValues;
		int[] container = spaces[0];
		container[3] = dx;
		container[4] = dy;
		container[5] = dz;
		sizes[0] = 1;
		if(remaining[0] == null) {
			remaining[0] = counts.clone();
		} else {
			System.arraycopy(counts, 0, remaining[0], 0, types);
		}
		valid = 1;
		knownValid = 0;
		return true;
	}

	/** the box placed at the level: the spaces and answers of the levels below are of another path now */
	void place(int level, int x, int y, int z, int type, int rotation) {
		BoxStackValue stackValue = stackValues[type][rotation];
		int offset = level * STRIDE;
		boxes[offset] = x;
		boxes[offset + 1] = y;
		boxes[offset + 2] = z;
		boxes[offset + 3] = x + stackValue.getDx();
		boxes[offset + 4] = y + stackValue.getDy();
		boxes[offset + 5] = z + stackValue.getDz();
		placedType[level] = type;
		if(valid > level + 1) {
			valid = level + 1;
		}
		if(knownValid > level + 1) {
			knownValid = level + 1;
		}
	}

	/**
	 * @return whether the type fits the free space of the level's node, as far as known without looking at its spaces: by the answer above, when
	 *         the box placed above misses the type's witness
	 */
	byte known(int level, int type) {
		while (knownValid <= level) {
			byte[] k = known[knownValid];
			if(k == null) {
				known[knownValid] = new byte[types];
				witness[knownValid] = new int[types * STRIDE];
			} else {
				Arrays.fill(k, UNKNOWN);
			}
			knownValid++;
		}
		byte[] k = known[level];
		if(k[type] != UNKNOWN) {
			return k[type];
		}
		if(level > 0) {
			byte[] above = known[level - 1];
			if(above[type] == NONE) {
				// the free space only shrinks
				k[type] = NONE;
				return NONE;
			}
			if(above[type] == FITS) {
				int offset = type * STRIDE;
				int[] aboveWitness = witness[level - 1];
				int b = (level - 1) * STRIDE;
				if(aboveWitness[offset + 3] <= boxes[b] || aboveWitness[offset] >= boxes[b + 3] || aboveWitness[offset + 4] <= boxes[b + 1] || aboveWitness[offset + 1] >= boxes[b + 4]
						|| aboveWitness[offset + 5] <= boxes[b + 2] || aboveWitness[offset + 2] >= boxes[b + 5]) {
					// the box placed above misses the witness, which is still empty
					System.arraycopy(aboveWitness, offset, witness[level], offset, STRIDE);
					k[type] = FITS;
					return FITS;
				}
			}
		}
		return UNKNOWN;
	}

	/** the type fits the level's free space at an empty region found elsewhere (a free point): kept as its witness */
	void fitsAt(int level, int type, int x, int y, int z, int dx, int dy, int dz) {
		int offset = type * STRIDE;
		int[] w = witness[level];
		w[offset] = x;
		w[offset + 1] = y;
		w[offset + 2] = z;
		w[offset + 3] = x + dx;
		w[offset + 4] = y + dy;
		w[offset + 5] = z + dz;
		known[level][type] = FITS;
	}

	/** @return whether the type fits the free space of the level's node in any of its rotations, looking at its spaces when not known */
	boolean fits(int level, int type) {
		byte answer = known(level, type);
		if(answer != UNKNOWN) {
			return answer == FITS;
		}
		byte[] k = known[level];
		int offset = type * STRIDE;
		int[] w = witness[level];
		if(valid <= level && sizes[valid - 1] != -1) {
			// a space of the nearest level computed above which holds the type and misses the boxes placed since is still empty: a witness without
			// computing the level's spaces
			int above = valid - 1;
			int[] s = spaces[above];
			int size = sizes[above];
			for (BoxStackValue rotation : stackValues[type]) {
				int dx = rotation.getDx();
				int dy = rotation.getDy();
				int dz = rotation.getDz();
				for (int i = 0; i < size * STRIDE; i += STRIDE) {
					if(s[i + 3] - s[i] >= dx && s[i + 4] - s[i + 1] >= dy && s[i + 5] - s[i + 2] >= dz && misses(s, i, above, level)) {
						System.arraycopy(s, i, w, offset, STRIDE);
						k[type] = FITS;
						return true;
					}
				}
			}
		}
		while (valid <= level) {
			split(valid - 1);
			valid++;
		}
		int size = sizes[level];
		if(size == -1) {
			// unknown: taken to fit, with the whole container as the witness, so that the levels below look again
			System.arraycopy(spaces[0], 0, w, offset, STRIDE);
			k[type] = FITS;
			return true;
		}
		int[] s = spaces[level];
		for (BoxStackValue rotation : stackValues[type]) {
			int dx = rotation.getDx();
			int dy = rotation.getDy();
			int dz = rotation.getDz();
			for (int i = 0; i < size * STRIDE; i += STRIDE) {
				if(s[i + 3] - s[i] >= dx && s[i + 4] - s[i + 1] >= dy && s[i + 5] - s[i + 2] >= dz) {
					System.arraycopy(s, i, w, offset, STRIDE);
					k[type] = FITS;
					return true;
				}
			}
		}
		k[type] = NONE;
		return false;
	}

	/** @return whether the space misses the boxes placed at the levels from one level to another (exclusive) */
	private boolean misses(int[] s, int i, int from, int to) {
		for (int b = from * STRIDE; b < to * STRIDE; b += STRIDE) {
			if(s[i + 3] > boxes[b] && s[i] < boxes[b + 3] && s[i + 4] > boxes[b + 1] && s[i + 1] < boxes[b + 4] && s[i + 5] > boxes[b + 2] && s[i + 2] < boxes[b + 5]) {
				return false;
			}
		}
		return true;
	}

	/** the number of spaces of the level, or -1 for unknown; computes the levels not yet computed */
	int size(int level) {
		while (valid <= level) {
			split(valid - 1);
			valid++;
		}
		return sizes[level];
	}

	/** @return whether the space holds some box remaining below the level, in some rotation */
	private boolean holdsAny(int[] remaining, int dx, int dy, int dz) {
		if(dx < pieceDx || dy < pieceDy || dz < pieceDz || (long)dx * dy * dz < pieceVolume) {
			return false;
		}
		for (int t = 0; t < remaining.length; t++) {
			if(remaining[t] > 0) {
				for (BoxStackValue rotation : stackValues[t]) {
					if(dx >= rotation.getDx() && dy >= rotation.getDy() && dz >= rotation.getDz()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** the spaces of the level below the parent: the parent's spaces split by the box placed at the parent, less the ones within others */
	private void split(int parent) {
		int child = parent + 1;
		int[] left = remaining[child];
		if(left == null) {
			left = remaining[parent].clone();
			remaining[child] = left;
		} else {
			System.arraycopy(remaining[parent], 0, left, 0, left.length);
		}
		left[placedType[parent]]--;
		int parentSize = sizes[parent];
		if(parentSize == -1) {
			sizes[child] = -1;
			return;
		}
		// the least a piece must hold
		pieceDx = Integer.MAX_VALUE;
		pieceDy = Integer.MAX_VALUE;
		pieceDz = Integer.MAX_VALUE;
		pieceVolume = Long.MAX_VALUE;
		for (int t = 0; t < left.length; t++) {
			if(left[t] > 0) {
				for (BoxStackValue rotation : stackValues[t]) {
					pieceDx = Math.min(pieceDx, rotation.getDx());
					pieceDy = Math.min(pieceDy, rotation.getDy());
					pieceDz = Math.min(pieceDz, rotation.getDz());
				}
				pieceVolume = Math.min(pieceVolume, stackValues[t][0].getBox().getVolume());
			}
		}
		int[] p = spaces[parent];
		int[] c = spaces[child];
		if(c == null) {
			c = new int[Math.max(16, Math.min(MAX_SPACES, parentSize + 8)) * STRIDE];
			spaces[child] = c;
		}
		int offset = parent * STRIDE;
		int bMinX = boxes[offset];
		int bMinY = boxes[offset + 1];
		int bMinZ = boxes[offset + 2];
		int bMaxX = boxes[offset + 3];
		int bMaxY = boxes[offset + 4];
		int bMaxZ = boxes[offset + 5];
		// the spaces the box does not touch stay, and stay maximal
		int count = 0;
		for (int i = 0; i < parentSize * STRIDE; i += STRIDE) {
			if(p[i + 3] <= bMinX || p[i] >= bMaxX || p[i + 4] <= bMinY || p[i + 1] >= bMaxY || p[i + 5] <= bMinZ || p[i + 2] >= bMaxZ) {
				if(count * STRIDE == c.length) {
					c = grow(child, c);
				}
				System.arraycopy(p, i, c, count * STRIDE, STRIDE);
				count++;
			}
		}
		int kept = count;
		// the others, cut by the box: up to six pieces each, at the sides of the box
		for (int i = 0; i < parentSize * STRIDE; i += STRIDE) {
			if(p[i + 3] <= bMinX || p[i] >= bMaxX || p[i + 4] <= bMinY || p[i + 1] >= bMaxY || p[i + 5] <= bMinZ || p[i + 2] >= bMaxZ) {
				continue;
			}
			int minX = p[i];
			int minY = p[i + 1];
			int minZ = p[i + 2];
			int maxX = p[i + 3];
			int maxY = p[i + 4];
			int maxZ = p[i + 5];
			if(bMinX > minX) {
				count = add(child, count, kept, left, minX, minY, minZ, bMinX, maxY, maxZ);
			}
			if(bMaxX < maxX) {
				count = add(child, count, kept, left, bMaxX, minY, minZ, maxX, maxY, maxZ);
			}
			if(bMinY > minY) {
				count = add(child, count, kept, left, minX, minY, minZ, maxX, bMinY, maxZ);
			}
			if(bMaxY < maxY) {
				count = add(child, count, kept, left, minX, bMaxY, minZ, maxX, maxY, maxZ);
			}
			if(bMinZ > minZ) {
				count = add(child, count, kept, left, minX, minY, minZ, maxX, maxY, bMinZ);
			}
			if(bMaxZ < maxZ) {
				count = add(child, count, kept, left, minX, minY, bMaxZ, maxX, maxY, maxZ);
			}
			if(count == -1) {
				sizes[child] = -1;
				return;
			}
		}
		sizes[child] = count;
	}

	/**
	 * Add a piece to the level's spaces, unless it holds no remaining box or is within one of them; the pieces within it are removed (a kept
	 * space, maximal above, is never within a piece).
	 *
	 * @return the number of spaces, or -1 for too many
	 */
	private int add(int level, int count, int kept, int[] remaining, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		if(count == -1 || !holdsAny(remaining, maxX - minX, maxY - minY, maxZ - minZ)) {
			return count;
		}
		int[] c = spaces[level];
		for (int i = 0; i < kept * STRIDE; i += STRIDE) {
			if(c[i] <= minX && c[i + 1] <= minY && c[i + 2] <= minZ && maxX <= c[i + 3] && maxY <= c[i + 4] && maxZ <= c[i + 5]) {
				return count;
			}
		}
		for (int i = kept * STRIDE; i < count * STRIDE; i += STRIDE) {
			if(c[i] <= minX && c[i + 1] <= minY && c[i + 2] <= minZ && maxX <= c[i + 3] && maxY <= c[i + 4] && maxZ <= c[i + 5]) {
				// within a piece
				return count;
			}
			if(minX <= c[i] && minY <= c[i + 1] && minZ <= c[i + 2] && c[i + 3] <= maxX && c[i + 4] <= maxY && c[i + 5] <= maxZ) {
				// a piece within it: replaced by the last
				count--;
				System.arraycopy(c, count * STRIDE, c, i, STRIDE);
				i -= STRIDE;
			}
		}
		if(count == MAX_SPACES) {
			return -1;
		}
		if(count * STRIDE == c.length) {
			c = grow(level, c);
		}
		int offset = count * STRIDE;
		c[offset] = minX;
		c[offset + 1] = minY;
		c[offset + 2] = minZ;
		c[offset + 3] = maxX;
		c[offset + 4] = maxY;
		c[offset + 5] = maxZ;
		return count + 1;
	}

	private int[] grow(int level, int[] c) {
		int[] grown = new int[Math.min(MAX_SPACES * STRIDE, c.length * 2)];
		System.arraycopy(c, 0, grown, 0, c.length);
		spaces[level] = grown;
		return grown;
	}
}
