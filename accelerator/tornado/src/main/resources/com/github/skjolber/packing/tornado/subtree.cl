// The search of the permutations of one item (FlatSubtree.run, or FlatTree.run for a tree item, and what they call) in OpenCL C, as a TornadoVM prebuilt
// kernel: one work item is one slot of the window. The kernel is a function-by-function port of the flat package of the core module (FlatGeometry,
// FlatPointOrder, FlatPoints, FlatPermutation, FlatSearch, FlatSubtree and FlatTree): the same names, the same order of statements and the same tie-breaking, so that a change in one
// can be made in the other. The parity of the two is tested by KernelParityTest.
//
// The arrays are TornadoVM device buffers of Java arrays: 16 bytes of header (the length is the int at offset 12), then the elements.

#define HEADER 16

// FlatLayout
#define POINT_INTS 7
#define PLANE_NONE 1023
#define PLACEMENT_INTS 6

#define C_OVERFLOW 0
#define C_BEST_COUNT 7
#define C_NODES 8
#define C_MAX_POINTS 9
#define C_TREE_LEVEL 10
#define C_TREE_DESCEND 11
#define C_TREE_KEY 12
#define CONTROL_INTS 16

// FlatGeometry
#define MIN_X 0
#define MIN_Y 1
#define MIN_Z 2
#define MAX_X 3
#define MAX_Y 4
#define MAX_Z 5
#define PLANES 6

// FlatProblem
#define P_N 0
#define P_TYPES 1
#define P_CX 2
#define P_CY 3
#define P_CZ 4
#define P_ROTATIONS 5
#define P_LIMIT 6
#define P_ALL_FIT 7
#define P_ANY_BEST 8
#define P_KEYS 9
#define P_HEADER 10

// FlatSubtree
#define NOT_STARTED 0
#define DONE 1
#define PAUSED 2
#define OVERFLOW 3

#define OUT_STATUS 0
#define OUT_COUNT 1
#define OUT_FULL 2
#define OUT_CURSOR 3

#define OUT_VOLUME 0
#define OUT_WEIGHT 1
#define OUT_NODES 2
#define OUT_LONGS 3

// the kernel parameters, see TornadoBruteForceAccelerator
#define P_DEPTH 0
#define P_BUDGET 1
#define P_SLOTS 2
#define P_TREE 3
#define P_BOUND 0
#define P_SHARED_VOLUME 1
#define P_SHARED_WEIGHT 2
#define P_SHARED_COUNT 3

// the offsets of FlatLayout, in the order of TornadoBruteForceAccelerator.layoutInts(..)
typedef struct {
	int n;
	int pMax;
	int frameInts;
	int control;
	int placements;
	int frames;
	int flags;
	int moveX;
	int moveY;
	int moveZ;
	int poolX;
	int poolY;
	int poolZ;
	int nextX;
	int nextY;
	int nextZ;
	int headX;
	int headY;
	int headZ;
	int tailX;
	int tailY;
	int tailZ;
	int copyX;
	int copyY;
	int copyZ;
	int perm;
	int rotation;
	int placedRotation;
	int bestRotation;
	int typeAt;
	int fit;
	int placedType;
	int remaining;
	int size;
	int minArea;
	int minVolume;
	int placedVolume;
	int bestVolume;
	int placedWeight;
	int longSize;
} Layout;

#define LAYOUT_INTS 40

static Layout readLayout(__global const int *v) {
	Layout l;
	l.n = v[0];
	l.pMax = v[1];
	l.frameInts = v[2];
	l.control = v[3];
	l.placements = v[4];
	l.frames = v[5];
	l.flags = v[6];
	l.moveX = v[7];
	l.moveY = v[8];
	l.moveZ = v[9];
	l.poolX = v[10];
	l.poolY = v[11];
	l.poolZ = v[12];
	l.nextX = v[13];
	l.nextY = v[14];
	l.nextZ = v[15];
	l.headX = v[16];
	l.headY = v[17];
	l.headZ = v[18];
	l.tailX = v[19];
	l.tailY = v[20];
	l.tailZ = v[21];
	l.copyX = v[22];
	l.copyY = v[23];
	l.copyZ = v[24];
	l.perm = v[25];
	l.rotation = v[26];
	l.placedRotation = v[27];
	l.bestRotation = v[28];
	l.typeAt = v[29];
	l.fit = v[30];
	l.placedType = v[31];
	l.remaining = v[32];
	l.size = v[33];
	l.minArea = v[34];
	l.minVolume = v[35];
	l.placedVolume = v[36];
	l.bestVolume = v[37];
	l.placedWeight = v[38];
	l.longSize = v[39];
	return l;
}

static inline int frameOffset(const Layout *l, int level) {
	return l->frames + level * l->frameInts;
}

static inline int planesYz(int planes) {
	return planes & PLANE_NONE;
}

static inline int planesXz(int planes) {
	return (planes >> 10) & PLANE_NONE;
}

static inline int planesXy(int planes) {
	return (planes >> 20) & PLANE_NONE;
}

static inline int planes(int yz, int xz, int xy) {
	return yz | (xz << 10) | (xy << 20);
}

// FlatLayout.reset
static void reset(const Layout *l, __global int *s, int b) {
	for (int i = 0; i <= l->pMax; i++) {
		s[b + l->flags + i] = 0;
		s[b + l->headX + i] = -1;
		s[b + l->headY + i] = -1;
		s[b + l->headZ + i] = -1;
		s[b + l->tailX + i] = -1;
		s[b + l->tailY + i] = -1;
		s[b + l->tailZ + i] = -1;
	}
	for (int i = 0; i < l->pMax; i++) {
		s[b + l->copyX + i] = -1;
		s[b + l->copyY + i] = -1;
		s[b + l->copyZ + i] = -1;
	}
	for (int i = 0; i < CONTROL_INTS; i++) {
		s[b + l->control + i] = 0;
	}
}

// FlatProblem
static inline int rotationOffsets(int types) {
	return P_HEADER + types;
}

static inline int rotationValues(int types) {
	return P_HEADER + 2 * types + 1;
}

static inline int minAreas(int types) {
	return types;
}

static inline int weightsOffset(int types) {
	return 2 * types;
}

static inline int maxLoad(int types) {
	return 3 * types;
}

static inline int typeOrder(int types, int rotations) {
	return rotationValues(types) + 3 * rotations;
}

static inline int radix(int types, int rotations) {
	return typeOrder(types, rotations) + types;
}

static inline int bounds(int types) {
	return maxLoad(types) + 2;
}

// FlatGeometry
static inline int dx(__global const int *s, int p) {
	return s[p + MAX_X] - s[p + MIN_X] + 1;
}

static inline int dy(__global const int *s, int p) {
	return s[p + MAX_Y] - s[p + MIN_Y] + 1;
}

static inline int dz(__global const int *s, int p) {
	return s[p + MAX_Z] - s[p + MIN_Z] + 1;
}

static inline long area(__global const int *s, int p) {
	return (long)dx(s, p) * dy(s, p);
}

static inline long volume(__global const int *s, int p) {
	return (long)dz(s, p) * dy(s, p) * dx(s, p);
}

static inline bool fits3D(__global const int *s, int p, int sx, int sy, int sz) {
	return !(sx > dx(s, p) || sy > dy(s, p) || sz > dz(s, p));
}

static inline void copyPoint(__global int *s, int from, int to) {
	s[to] = s[from];
	s[to + 1] = s[from + 1];
	s[to + 2] = s[from + 2];
	s[to + 3] = s[from + 3];
	s[to + 4] = s[from + 4];
	s[to + 5] = s[from + 5];
	s[to + 6] = s[from + 6];
}

static inline bool eclipses(__global const int *s, int a, int p) {
	return s[a + MIN_X] <= s[p + MIN_X] && s[p + MAX_X] <= s[a + MAX_X] && s[a + MIN_Y] <= s[p + MIN_Y] && s[p + MAX_Y] <= s[a + MAX_Y] && s[a + MIN_Z] <= s[p + MIN_Z]
			&& s[p + MAX_Z] <= s[a + MAX_Z];
}

static inline bool eclipsesX(__global const int *s, int a, int p) {
	return s[a + MIN_X] <= s[p + MIN_X] && s[p + MAX_X] <= s[a + MAX_X];
}

static inline bool eclipsesY(__global const int *s, int a, int p) {
	return s[a + MIN_Y] <= s[p + MIN_Y] && s[p + MAX_Y] <= s[a + MAX_Y];
}

static inline bool eclipsesZ(__global const int *s, int a, int p) {
	return s[a + MIN_Z] <= s[p + MIN_Z] && s[p + MAX_Z] <= s[a + MAX_Z];
}

static inline bool eclipsesMovedX(__global const int *s, int a, int p, int x) {
	return s[a + MIN_X] <= x && s[p + MAX_X] <= s[a + MAX_X] && eclipsesY(s, a, p) && eclipsesZ(s, a, p);
}

static inline bool eclipsesMovedY(__global const int *s, int a, int p, int y) {
	return s[a + MIN_Y] <= y && s[p + MAX_Y] <= s[a + MAX_Y] && eclipsesX(s, a, p) && eclipsesZ(s, a, p);
}

static inline bool eclipsesMovedZ(__global const int *s, int a, int p, int z) {
	return s[a + MIN_Z] <= z && s[p + MAX_Z] <= s[a + MAX_Z] && eclipsesX(s, a, p) && eclipsesY(s, a, p);
}

static inline long areaAtX(__global const int *s, int p, int xx) {
	return (long)dy(s, p) * (s[p + MAX_X] - xx + 1);
}

static inline long areaAtY(__global const int *s, int p, int yy) {
	return (long)dx(s, p) * (s[p + MAX_Y] - yy + 1);
}

static inline long areaAtMaxX(__global const int *s, int p, int maxX) {
	return (long)dy(s, p) * (maxX - s[p + MIN_X] + 1);
}

static inline long areaAtMaxY(__global const int *s, int p, int maxY) {
	return (long)dx(s, p) * (maxY - s[p + MIN_Y] + 1);
}

static inline long volumeAtZ(__global const int *s, int p, int zz) {
	return (long)dx(s, p) * dy(s, p) * (s[p + MAX_Z] - zz + 1);
}

static inline long volumeAtMaxZ(__global const int *s, int p, int maxZ) {
	return (long)dx(s, p) * dy(s, p) * (maxZ - s[p + MIN_Z] + 1);
}

static bool isAfterXyz(__global const int *s, int a, int b) {
	for (int i = 0; i < 6; i++) {
		int x = s[a + i];
		int y = s[b + i];
		if (x != y) {
			return x > y;
		}
	}
	return false;
}

// FlatPointOrder
static inline int supports(int planes) {
	return (planesXy(planes) != PLANE_NONE ? 4 : 0) + (planesXz(planes) != PLANE_NONE ? 2 : 0) + (planesYz(planes) != PLANE_NONE ? 1 : 0);
}

static inline int supportsMovedX(__global const int *s, int placementOffset, int planes, int xx) {
	int xy = planesXy(planes);
	int xz = planesXz(planes);
	return (xy != PLANE_NONE && xx <= s[placementOffset + xy * PLACEMENT_INTS + 3] ? 4 : 0) + (xz != PLANE_NONE && xx <= s[placementOffset + xz * PLACEMENT_INTS + 3] ? 2 : 0);
}

static inline int supportsMovedY(__global const int *s, int placementOffset, int planes, int yy) {
	int xy = planesXy(planes);
	int yz = planesYz(planes);
	return (xy != PLANE_NONE && yy <= s[placementOffset + xy * PLACEMENT_INTS + 4] ? 4 : 0) + (yz != PLANE_NONE && yy <= s[placementOffset + yz * PLACEMENT_INTS + 4] ? 1 : 0);
}

static inline int supportsMovedZ(__global const int *s, int placementOffset, int planes, int zz) {
	int xz = planesXz(planes);
	int yz = planesYz(planes);
	return (xz != PLANE_NONE && zz <= s[placementOffset + xz * PLACEMENT_INTS + 5] ? 2 : 0) + (yz != PLANE_NONE && zz <= s[placementOffset + yz * PLACEMENT_INTS + 5] ? 1 : 0);
}

static bool isAfterTied(__global const int *s, int placementOffset, int a, int b, int direction, int moved) {
	for (int i = MAX_X; i <= MAX_Z; i++) {
		int x = s[a + i];
		int y = s[b + i];
		if (x != y) {
			return x < y;
		}
	}

	int supportsA;
	int supportsB;
	if (direction == 0) {
		supportsA = supportsMovedX(s, placementOffset, s[a + PLANES], moved);
		supportsB = supportsMovedX(s, placementOffset, s[b + PLANES], moved);
	} else if (direction == 1) {
		supportsA = supportsMovedY(s, placementOffset, s[a + PLANES], moved);
		supportsB = supportsMovedY(s, placementOffset, s[b + PLANES], moved);
	} else {
		supportsA = supportsMovedZ(s, placementOffset, s[a + PLANES], moved);
		supportsB = supportsMovedZ(s, placementOffset, s[b + PLANES], moved);
	}
	if (supportsA != supportsB) {
		return supportsA < supportsB;
	}

	if (s[a + direction] != s[b + direction]) {
		return s[a + direction] > s[b + direction];
	}
	supportsA = supports(s[a + PLANES]);
	supportsB = supports(s[b + PLANES]);
	return supportsA < supportsB;
}

static bool isAfterX(__global const int *s, int placementOffset, int a, int b, int xx) {
	int ay = s[a + MIN_Y];
	int by = s[b + MIN_Y];
	if (ay != by) {
		return ay > by;
	}
	int az = s[a + MIN_Z];
	int bz = s[b + MIN_Z];
	if (az != bz) {
		return az > bz;
	}

	long volumeA = (long)dy(s, a) * dz(s, a) * s[a + MAX_X];
	long volumeB = (long)dy(s, b) * dz(s, b) * s[b + MAX_X];
	if (volumeA != volumeB) {
		return volumeA < volumeB;
	}

	return isAfterTied(s, placementOffset, a, b, 0, xx);
}

static bool isAfterY(__global const int *s, int placementOffset, int a, int b, int yy) {
	int az = s[a + MIN_Z];
	int bz = s[b + MIN_Z];
	if (az != bz) {
		return az > bz;
	}
	int ax = s[a + MIN_X];
	int bx = s[b + MIN_X];
	if (ax != bx) {
		return ax > bx;
	}

	long volumeA = (long)dx(s, a) * dz(s, a) * s[a + MAX_Y];
	long volumeB = (long)dx(s, b) * dz(s, b) * s[b + MAX_Y];
	if (volumeA != volumeB) {
		return volumeA < volumeB;
	}

	return isAfterTied(s, placementOffset, a, b, 1, yy);
}

static bool isAfterZ(__global const int *s, int placementOffset, int a, int b, int zz) {
	int ax = s[a + MIN_X];
	int bx = s[b + MIN_X];
	if (ax != bx) {
		return ax > bx;
	}
	int ay = s[a + MIN_Y];
	int by = s[b + MIN_Y];
	if (ay != by) {
		return ay > by;
	}

	long volumeA = area(s, a) * s[a + MAX_Z];
	long volumeB = area(s, b) * s[b + MAX_Z];
	if (volumeA != volumeB) {
		return volumeA < volumeB;
	}

	return isAfterTied(s, placementOffset, a, b, 2, zz);
}

// FlatPoints
static inline void setPlacement(const Layout *l, __global int *s, int base, int id, int x, int y, int z, int endX, int endY, int endZ) {
	int o = base + l->placements + id * PLACEMENT_INTS;
	s[o] = x;
	s[o + 1] = y;
	s[o + 2] = z;
	s[o + 3] = endX;
	s[o + 4] = endY;
	s[o + 5] = endZ;
}

static void clearPoints(const Layout *l, __global int *s, int base, int dx, int dy, int dz) {
	setPlacement(l, s, base, l->n, 0, 0, 0, dx - 1, dy - 1, dz - 1);

	int frame = base + frameOffset(l, 0);
	s[frame] = 1;
	int p = frame + 1;
	s[p] = 0;
	s[p + 1] = 0;
	s[p + 2] = 0;
	s[p + 3] = dx - 1;
	s[p + 4] = dy - 1;
	s[p + 5] = dz - 1;
	s[p + 6] = planes(l->n, l->n, l->n);
}

static int filterMinimums(const Layout *l, __global int *s, int base, int level, long minArea, long minVolume) {
	int frame = base + frameOffset(l, level);
	int size = s[frame];
	int values = frame + 1;

	int target = 0;
	for (int i = 0; i < size; i++) {
		int p = values + i * POINT_INTS;
		if (!(volume(s, p) < minVolume || area(s, p) < minArea)) {
			if (target != i) {
				copyPoint(s, p, values + target * POINT_INTS);
			}
			target++;
		}
	}
	s[frame] = target;
	return target;
}

static inline bool supportedXY(__global const int *s, int placements, int id, int x, int y) {
	if (id == PLANE_NONE) {
		return false;
	}
	int o = placements + id * PLACEMENT_INTS;
	return s[o] <= x && x <= s[o + 3] && s[o + 1] <= y && y <= s[o + 4];
}

static inline bool supportedXZ(__global const int *s, int placements, int id, int x, int z) {
	if (id == PLANE_NONE) {
		return false;
	}
	int o = placements + id * PLACEMENT_INTS;
	return s[o] <= x && x <= s[o + 3] && s[o + 2] <= z && z <= s[o + 5];
}

static inline bool supportedYZ(__global const int *s, int placements, int id, int y, int z) {
	if (id == PLANE_NONE) {
		return false;
	}
	int o = placements + id * PLACEMENT_INTS;
	return s[o + 1] <= y && y <= s[o + 4] && s[o + 2] <= z && z <= s[o + 5];
}

static int binarySearchPlusMinX(__global const int *s, int values, int size, int low, int key) {
	int high = size - 1;
	int found = -1;
	while (low <= high && found == -1) {
		int mid = (int)(((unsigned int)(low + high)) >> 1);
		int midVal = s[values + mid * POINT_INTS];
		if (midVal < key) {
			low = mid + 1;
		} else if (midVal != key) {
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

static inline bool canMoveX(__global const int *s, int p, int xx, long minArea, long minVolume) {
	if (s[p + MAX_X] < xx) {
		return false;
	}
	long a = areaAtX(s, p, xx);
	if (a >= minArea) {
		return true;
	}
	return !(a * dz(s, p) < minVolume);
}

static inline bool canMoveY(__global const int *s, int p, int yy, long minArea, long minVolume) {
	if (s[p + MAX_Y] < yy) {
		return false;
	}
	long a = areaAtY(s, p, yy);
	if (a >= minArea) {
		return true;
	}
	return !(a * dz(s, p) < minVolume);
}

static inline bool canMoveZ(__global const int *s, int p, int zz, long minVolume) {
	if (s[p + MAX_Z] < zz) {
		return false;
	}
	return !(volumeAtZ(s, p, zz) < minVolume);
}

static inline bool isConstrainedAtMaxX(__global const int *s, int p, int maxX, long minArea, long minVolume) {
	long a = areaAtMaxX(s, p, maxX);
	if (a >= minArea) {
		return false;
	}
	return a * dz(s, p) < minVolume;
}

static inline bool isConstrainedAtMaxY(__global const int *s, int p, int maxY, long minArea, long minVolume) {
	long a = areaAtMaxY(s, p, maxY);
	if (a >= minArea) {
		return false;
	}
	return a * dz(s, p) < minVolume;
}

static bool isEclipsed(__global const int *s, int out, int count, int candidate) {
	int minX = s[candidate];
	int index = count - 1;
	while (index >= 0 && s[out + index * POINT_INTS] > minX) {
		index--;
	}
	bool eclipsed = false;
	while (index >= 0 && !eclipsed) {
		eclipsed = eclipses(s, out + index * POINT_INTS, candidate);
		index--;
	}
	return eclipsed;
}

static bool isEclipsedAtXX(__global const int *s, int out, int count, int candidate, int xx) {
	bool eclipsed = false;
	bool done = false;
	int index = count - 1;
	while (index >= 0 && !done) {
		int o = out + index * POINT_INTS;
		if (s[o] < xx) {
			done = true;
		} else if (eclipses(s, o, candidate)) {
			eclipsed = true;
			done = true;
		}
		index--;
	}
	return eclipsed;
}

static void sortX(__global int *s, int list, int size, int values, int placements, int xx) {
	for (int i = 1; i < size; i++) {
		int index = s[list + i];
		int point = values + index * POINT_INTS;
		int j = i - 1;
		while (j >= 0 && isAfterX(s, placements, values + s[list + j] * POINT_INTS, point, xx)) {
			s[list + j + 1] = s[list + j];
			j--;
		}
		s[list + j + 1] = index;
	}
}

static void sortY(__global int *s, int list, int size, int values, int placements, int yy) {
	for (int i = 1; i < size; i++) {
		int index = s[list + i];
		int point = values + index * POINT_INTS;
		int j = i - 1;
		while (j >= 0 && isAfterY(s, placements, values + s[list + j] * POINT_INTS, point, yy)) {
			s[list + j + 1] = s[list + j];
			j--;
		}
		s[list + j + 1] = index;
	}
}

static void sortZ(__global int *s, int list, int size, int values, int placements, int zz) {
	for (int i = 1; i < size; i++) {
		int index = s[list + i];
		int point = values + index * POINT_INTS;
		int j = i - 1;
		while (j >= 0 && isAfterZ(s, placements, values + s[list + j] * POINT_INTS, point, zz)) {
			s[list + j + 1] = s[list + j];
			j--;
		}
		s[list + j + 1] = index;
	}
}

static inline void chain(__global int *s, int head, int tail, int next, int target, int entry) {
	s[next + entry] = -1;
	int last = s[tail + target];
	if (last == -1) {
		s[head + target] = entry;
	} else {
		s[next + last] = entry;
	}
	s[tail + target] = entry;
}

// FlatPoints.add: the number of points in the next frame, or -1 if there were more points than the layout holds
static int addPoint(const Layout *l, __global int *s, int base, int level, int pointIndex, int placementId, long minArea, long minVolume) {
	const int placements = base + l->placements;
	const int pl = placements + placementId * PLACEMENT_INTS;
	const int placementX = s[pl];
	const int placementY = s[pl + 1];
	const int placementZ = s[pl + 2];
	const int placementEndX = s[pl + 3];
	const int placementEndY = s[pl + 4];
	const int placementEndZ = s[pl + 5];

	const int inFrame = base + frameOffset(l, level);
	const int outFrame = base + frameOffset(l, level + 1);
	const int size = s[inFrame];
	const int values = inFrame + 1;
	const int out = outFrame + 1;
	const int flags = base + l->flags;

	const int xx = placementEndX + 1;
	const int yy = placementEndY + 1;
	const int zz = placementEndZ + 1;

	int source = values + pointIndex * POINT_INTS;
	int sourcePlanes = s[source + PLANES];
	bool supportedXy = s[source + MIN_Z] == placementZ && supportedXY(s, placements, planesXy(sourcePlanes), placementEndX, placementEndY);
	bool supportedXz = s[source + MIN_Y] == placementY && supportedXZ(s, placements, planesXz(sourcePlanes), placementEndX, placementEndZ);
	bool supportedYz = s[source + MIN_X] == placementX && supportedYZ(s, placements, planesYz(sourcePlanes), placementEndY, placementEndZ);
	bool supported = supportedXy && supportedXz && supportedYz;

	int endIndex = binarySearchPlusMinX(s, values, size, pointIndex + 1, placementEndX);

	int startIndex;
	if (supportedYz) {
		startIndex = pointIndex;
		while (startIndex > 0 && s[values + (startIndex - 1) * POINT_INTS] == placementX) {
			startIndex--;
		}
	} else {
		startIndex = 0;
	}

	// classify
	const int listX = base + l->moveX;
	const int listY = base + l->moveY;
	const int listZ = base + l->moveZ;
	int moveCountX = 0;
	int moveCountY = 0;
	int moveCountZ = 0;

	for (int i = startIndex; i < endIndex; i++) {
		int p = values + i * POINT_INTS;
		if (s[p + MIN_Y] <= placementEndY && s[p + MIN_Z] <= placementEndZ) {
			bool swallowed = s[p + MIN_X] >= placementX && s[p + MIN_Y] >= placementY && s[p + MIN_Z] >= placementZ;
			if (swallowed) {
				s[flags + i] = 1;
			}
			if (swallowed || !supported) {
				if (canMoveX(s, p, xx, minArea, minVolume)) {
					s[listX + moveCountX] = i;
					moveCountX++;
				}
				if (canMoveY(s, p, yy, minArea, minVolume)) {
					s[listY + moveCountY] = i;
					moveCountY++;
				}
				if (canMoveZ(s, p, zz, minVolume)) {
					s[listZ + moveCountZ] = i;
					moveCountZ++;
				}
			}
		}
	}

	// move x
	const int poolX = base + l->poolX;
	int poolCountX = 0;
	if (moveCountX > 0) {
		sortX(s, listX, moveCountX, values, placements, xx);

		int target = endIndex;
		for (int k = 0; k < moveCountX; k++) {
			int currentIndex = s[listX + k];
			int p = values + currentIndex * POINT_INTS;

			bool eclipsed = false;
			int a = 0;
			while (a < poolCountX && !eclipsed) {
				eclipsed = eclipsesMovedX(s, poolX + a * POINT_INTS, p, xx);
				a++;
			}
			if (!eclipsed) {
				int added = poolX + poolCountX * POINT_INTS;
				int pointPlanes = s[p + PLANES];
				int xz = planesXz(pointPlanes);
				int xy = planesXy(pointPlanes);
				if (xz != PLANE_NONE && xx > s[placements + xz * PLACEMENT_INTS + 3]) {
					xz = PLANE_NONE;
				}
				if (xy != PLANE_NONE && xx > s[placements + xy * PLACEMENT_INTS + 3]) {
					xy = PLANE_NONE;
				}
				int yz = PLANE_NONE;
				if (!(s[p + MIN_Y] < placementY || s[p + MIN_Z] < placementZ)) {
					yz = placementId;
				}
				s[added] = xx;
				s[added + 1] = s[p + 1];
				s[added + 2] = s[p + 2];
				s[added + 3] = s[p + 3];
				s[added + 4] = s[p + 4];
				s[added + 5] = s[p + 5];
				s[added + PLANES] = planes(yz, xz, xy);

				while (target < size && isAfterXyz(s, added, values + target * POINT_INTS)) {
					target++;
				}
				chain(s, base + l->headX, base + l->tailX, base + l->nextX, target, poolCountX);
				poolCountX++;
			}
		}
	}

	// move y
	const int poolY = base + l->poolY;
	int poolCountY = 0;
	if (moveCountY > 0) {
		sortY(s, listY, moveCountY, values, placements, yy);

		for (int k = 0; k < moveCountY; k++) {
			int currentIndex = s[listY + k];
			int p = values + currentIndex * POINT_INTS;

			bool eclipsed = false;
			int a = 0;
			while (a < poolCountY && !eclipsed) {
				eclipsed = eclipsesMovedY(s, poolY + a * POINT_INTS, p, yy);
				a++;
			}
			if (!eclipsed) {
				int added = poolY + poolCountY * POINT_INTS;
				int pointPlanes = s[p + PLANES];
				int yz = planesYz(pointPlanes);
				int xy = planesXy(pointPlanes);
				if (yz != PLANE_NONE && yy > s[placements + yz * PLACEMENT_INTS + 4]) {
					yz = PLANE_NONE;
				}
				if (xy != PLANE_NONE && yy > s[placements + xy * PLACEMENT_INTS + 4]) {
					xy = PLANE_NONE;
				}
				int xz = PLANE_NONE;
				if (!(s[p + MIN_X] < placementX || s[p + MIN_Z] < placementZ)) {
					xz = placementId;
				}
				s[added] = s[p];
				s[added + 1] = yy;
				s[added + 2] = s[p + 2];
				s[added + 3] = s[p + 3];
				s[added + 4] = s[p + 4];
				s[added + 5] = s[p + 5];
				s[added + PLANES] = planes(yz, xz, xy);

				int target = currentIndex + 1;
				while (target < size && isAfterXyz(s, added, values + target * POINT_INTS)) {
					target++;
				}
				chain(s, base + l->headY, base + l->tailY, base + l->nextY, target, poolCountY);
				poolCountY++;
			}
		}
	}

	// move z
	const int poolZ = base + l->poolZ;
	int poolCountZ = 0;
	if (moveCountZ > 0) {
		sortZ(s, listZ, moveCountZ, values, placements, zz);

		for (int k = 0; k < moveCountZ; k++) {
			int currentIndex = s[listZ + k];
			int p = values + currentIndex * POINT_INTS;

			bool eclipsed = false;
			int a = 0;
			while (a < poolCountZ && !eclipsed) {
				eclipsed = eclipsesMovedZ(s, poolZ + a * POINT_INTS, p, zz);
				a++;
			}
			if (!eclipsed) {
				int added = poolZ + poolCountZ * POINT_INTS;
				int pointPlanes = s[p + PLANES];
				int yz = planesYz(pointPlanes);
				int xz = planesXz(pointPlanes);
				if (yz != PLANE_NONE && zz > s[placements + yz * PLACEMENT_INTS + 5]) {
					yz = PLANE_NONE;
				}
				if (xz != PLANE_NONE && zz > s[placements + xz * PLACEMENT_INTS + 5]) {
					xz = PLANE_NONE;
				}
				int xy = PLANE_NONE;
				if (!(s[p + MIN_X] < placementX || s[p + MIN_Y] < placementY)) {
					xy = placementId;
				}
				s[added] = s[p];
				s[added + 1] = s[p + 1];
				s[added + 2] = zz;
				s[added + 3] = s[p + 3];
				s[added + 4] = s[p + 4];
				s[added + 5] = s[p + 5];
				s[added + PLANES] = planes(yz, xz, xy);

				int target = currentIndex + 1;
				while (target < size && isAfterXyz(s, added, values + target * POINT_INTS)) {
					target++;
				}
				chain(s, base + l->headZ, base + l->tailZ, base + l->nextZ, target, poolCountZ);
				poolCountZ++;
			}
		}
	}

	// constrain the max values of the points which the placement cuts into
	const int copyX = base + l->copyX;
	const int copyY = base + l->copyY;
	const int copyZ = base + l->copyZ;

	if (supported) {
		// not necessary
	} else if (supportedXy && supportedXz) {
		// directly left of the placement
		int limit = 0;
		int scan = endIndex - 1;
		while (scan >= 0 && limit == 0) {
			if (s[flags + scan] == 0 && s[values + scan * POINT_INTS] < placementX) {
				limit = scan + 1;
			}
			scan--;
		}
		for (int i = 0; i < limit; i++) {
			int p = values + i * POINT_INTS;
			if (s[flags + i] == 0 && placementY <= s[p + MIN_Y] && s[p + MIN_Y] <= placementEndY && placementZ <= s[p + MIN_Z] && s[p + MIN_Z] <= placementEndZ && placementX <= s[p + MAX_X]) {
				long a = (placementX - s[p + MIN_X]) * (long)dy(s, p);
				if (a >= minArea) {
					s[copyX + i] = placementX - 1;
				}
				s[flags + i] = 1;
			}
		}
	} else if (supportedXy && supportedYz) {
		// directly in front of the placement
		for (int i = startIndex; i < endIndex; i++) {
			int p = values + i * POINT_INTS;
			if (s[flags + i] == 0 && s[p + MIN_Y] < placementY && placementY <= s[p + MAX_Y] && placementZ <= s[p + MIN_Z] && s[p + MIN_Z] <= placementEndZ && placementX <= s[p + MIN_X]
					&& s[p + MIN_X] <= placementEndX) {
				long a = (placementY - s[p + MIN_Y]) * (long)dx(s, p);
				if (a >= minArea) {
					s[copyY + i] = placementY - 1;
				}
				s[flags + i] = 1;
			}
		}
	} else if (supportedXz && supportedYz) {
		// directly below the placement
		for (int i = startIndex; i < endIndex; i++) {
			int p = values + i * POINT_INTS;
			if (s[flags + i] == 0 && s[p + MIN_Z] < placementZ && placementZ <= s[p + MAX_Z] && placementY <= s[p + MIN_Y] && s[p + MIN_Y] <= placementEndY && placementX <= s[p + MIN_X]
					&& s[p + MIN_X] <= placementEndX) {
				s[copyZ + i] = placementZ - 1;
				s[flags + i] = 1;
			}
		}
	} else {
		for (int i = 0; i < endIndex; i++) {
			int p = values + i * POINT_INTS;
			if (!(placementEndX < s[p + MIN_X] || placementEndY < s[p + MIN_Y] || placementEndZ < s[p + MIN_Z] || placementX > s[p + MAX_X] || placementY > s[p + MAX_Y]
					|| placementZ > s[p + MAX_Z])) {
				if (s[p + MIN_X] < placementX) {
					int copyMaxX = placementX - 1;
					if (!isConstrainedAtMaxX(s, p, copyMaxX, minArea, minVolume)) {
						s[copyX + i] = copyMaxX;
					}
				}
				if (s[p + MIN_Y] < placementY) {
					int copyMaxY = placementY - 1;
					if (!isConstrainedAtMaxY(s, p, copyMaxY, minArea, minVolume)) {
						s[copyY + i] = copyMaxY;
					}
				}
				if (s[p + MIN_Z] < placementZ) {
					int copyMaxZ = placementZ - 1;
					if (!(volumeAtMaxZ(s, p, copyMaxZ) < minVolume)) {
						s[copyZ + i] = copyMaxZ;
					}
				}
				s[flags + i] = 1;
			}
		}
	}

	// merge, in x order
	const int headX = base + l->headX;
	const int headY = base + l->headY;
	const int headZ = base + l->headZ;
	const int tailX = base + l->tailX;
	const int tailY = base + l->tailY;
	const int tailZ = base + l->tailZ;
	const int nextX = base + l->nextX;
	const int nextY = base + l->nextY;
	const int nextZ = base + l->nextZ;
	const int pMax = l->pMax;
	const int spare = out + pMax * POINT_INTS;

	int count = 0;
	bool overflow = false;
	for (int i = 0; i < endIndex; i++) {
		for (int round = 0; round < 2; round++) {
			int head = round == 0 ? headZ : headY;
			int next = round == 0 ? nextZ : nextY;
			int pool = round == 0 ? poolZ : poolY;
			int entry = s[head + i];
			while (entry != -1) {
				int p = pool + entry * POINT_INTS;
				if (!isEclipsed(s, out, count, p)) {
					if (count < pMax) {
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
		if (s[flags + i] == 0) {
			if (count < pMax) {
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
			if (max != -1) {
				copyPoint(s, p, spare);
				s[spare + MAX_X + round] = max;
				if (!isEclipsed(s, out, count, spare)) {
					if (count < pMax) {
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
			if (!isEclipsed(s, out, count, p)) {
				if (count < pMax) {
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
			bool eclipsed;
			if (planesYz(s[p + PLANES]) != PLANE_NONE) {
				eclipsed = isEclipsedAtXX(s, out, count, p, xx);
			} else {
				eclipsed = isEclipsed(s, out, count, p);
			}
			if (!eclipsed) {
				if (count < pMax) {
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

		if (i < size) {
			if (s[flags + i] == 0) {
				if (count < pMax) {
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
	if (overflow) {
		s[base + l->control + C_OVERFLOW] = 1;
		return -1;
	}
	return count;
}

// FlatSearch
static int bestPoint(__global const int *s, int frame, int dx, int dy, int dz) {
	const int size = s[frame];
	const int values = frame + 1;

	int best = -1;
	long bestArea = 0L;
	int bestMinZ = 0;
	for (int k = 0; k < size; k++) {
		int p = values + k * POINT_INTS;
		if (fits3D(s, p, dx, dy, dz)) {
			long a = area(s, p);
			// the candidate is not better if it has less area, or a higher z, or the same area and less volume
			if (best == -1 || !(bestArea < a || bestMinZ < s[p + MIN_Z] || (bestArea == a && volume(s, values + best * POINT_INTS) < volume(s, p)))) {
				best = k;
				bestArea = a;
				bestMinZ = s[p + MIN_Z];
			}
		}
	}
	return best;
}

static int offer(const Layout *l, __global int *s, __global long *sl, int base, int longBase, int count) {
	sl[longBase + l->bestVolume] = sl[longBase + l->placedVolume + count];
	int from = base + l->placedRotation;
	int to = base + l->bestRotation;
	for (int i = 0; i < count; i++) {
		s[to + i] = s[from + i];
	}
	return count;
}

// the number of placed boxes, or -1 if there were more free points than the layout holds (the scratch is then reset)
static int searchPermutation(const Layout *l, __global const int *problem, __global const long *problemLongs, __global int *s, __global long *sl, int base, int longBase, int maxPackableCount) {
	const int types = problem[P_TYPES];
	const int n = problem[P_N];
	const int rotationOffsetsIndex = rotationOffsets(types);
	const int rotationValuesIndex = rotationValues(types);

	const int permBase = base + l->perm;
	const int rotationBase = base + l->rotation;
	const int placedRotationBase = base + l->placedRotation;
	const int control = base + l->control;
	const int minAreaBase = longBase + l->minArea;
	const int minVolumeBase = longBase + l->minVolume;
	const int placedVolumeBase = longBase + l->placedVolume;
	const int length = maxPackableCount;

	sl[longBase + l->bestVolume] = 0L;
	s[control + C_BEST_COUNT] = 0;
	if (length == 0) {
		return 0;
	}

	// the smallest area of the boxes from each level on, in any rotation, and the smallest volume of all the boxes of the permutation from each level on
	long minArea = LONG_MAX;
	for (int i = length - 1; i >= 0; i--) {
		long a = problemLongs[minAreas(types) + s[permBase + i]];
		if (a < minArea) {
			minArea = a;
		}
		sl[minAreaBase + i] = minArea;
	}
	long minVolume = LONG_MAX;
	for (int i = n - 1; i >= 0; i--) {
		long v = problemLongs[s[permBase + i]];
		if (v < minVolume) {
			minVolume = v;
		}
		sl[minVolumeBase + i] = minVolume;
	}

	clearPoints(l, s, base, problem[P_CX], problem[P_CY], problem[P_CZ]);
	if (sl[minAreaBase] != 0L || sl[minVolumeBase] != 0L) {
		filterMinimums(l, s, base, 0, sl[minAreaBase], sl[minVolumeBase]);
	}
	sl[placedVolumeBase] = 0L;

	int bestCount = 0;
	int nodes = 0;
	int maxPoints = s[control + C_MAX_POINTS];

	int level = 0;
	bool descend = true;
	bool overflow = false;
	while (level >= 0 && !overflow) {
		bool unwind;
		if (descend && level == length) {
			// every box is placed
			if (level > bestCount) {
				bestCount = offer(l, s, sl, base, longBase, level);
			}
			unwind = true;
		} else if (!descend && bestCount == length) {
			// the best arrangement places all boxes: unwind without trying more rotations
			unwind = true;
		} else {
			if (descend) {
				s[rotationBase + level] = 0;
			}

			// the next rotation of the box of the level which has a position
			const int type = s[permBase + level];
			const int firstRotation = problem[rotationOffsetsIndex + type];
			const int rotationCount = problem[rotationOffsetsIndex + type + 1] - firstRotation;
			const int frame = base + frameOffset(l, level);

			int rotationIndex = s[rotationBase + level];
			int pointIndex = -1;
			int dx = 0;
			int dy = 0;
			int dz = 0;
			while (pointIndex == -1 && rotationIndex < rotationCount) {
				int rotation = rotationValuesIndex + 3 * (firstRotation + rotationIndex);
				dx = problem[rotation];
				dy = problem[rotation + 1];
				dz = problem[rotation + 2];
				rotationIndex++;
				pointIndex = bestPoint(s, frame, dx, dy, dz);
			}
			s[rotationBase + level] = rotationIndex;

			if (pointIndex != -1) {
				// place the box and continue with the next level
				int point = frame + 1 + pointIndex * POINT_INTS;
				int x = s[point];
				int y = s[point + 1];
				int z = s[point + 2];
				setPlacement(l, s, base, level, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
				int count = addPoint(l, s, base, level, pointIndex, level, sl[minAreaBase + level], sl[minVolumeBase + level]);
				if (count < 0) {
					overflow = true;
				} else {
					if (count > maxPoints) {
						maxPoints = count;
					}
					int nextLevel = level + 1;
					if (nextLevel < length) {
						long nextArea = sl[minAreaBase + nextLevel];
						long nextVolume = sl[minVolumeBase + nextLevel];
						if (nextArea != sl[minAreaBase + level] || nextVolume != sl[minVolumeBase + level]) {
							filterMinimums(l, s, base, nextLevel, nextArea, nextVolume);
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
				if (level > bestCount) {
					bestCount = offer(l, s, sl, base, longBase, level);
				}
				unwind = true;
			}
		}
		if (unwind) {
			level--;
			descend = false;
		}
	}

	if (overflow) {
		reset(l, s, base);
		s[control + C_OVERFLOW] = 1;
		return -1;
	}
	s[control + C_BEST_COUNT] = bestCount;
	s[control + C_NODES] += nodes;
	s[control + C_MAX_POINTS] = maxPoints;
	return bestCount;
}

// FlatPermutation
static void sortRange(__global int *values, int from, int to) {
	for (int i = from + 1; i < to; i++) {
		int value = values[i];
		int j = i - 1;
		while (j >= from && values[j] > value) {
			values[j + 1] = values[j];
			j--;
		}
		values[j + 1] = value;
	}
}

// the index which was changed, or -1 if there is no next permutation (the permutation is then unchanged)
static int nextPermutation(__global int *permutation, int offset, int n, int maxIndex) {
	int changed = -1;
	int index = maxIndex;
	while (index >= 0) {
		int current = permutation[offset + index];

		// the smallest type to the right of the index which is larger than the current
		int minIndex = -1;
		for (int i = index + 1; i < n; i++) {
			int value = permutation[offset + i];
			if (value > current && (minIndex == -1 || value < permutation[offset + minIndex])) {
				minIndex = i;
			}
		}
		if (minIndex == -1) {
			index--;
		} else {
			permutation[offset + index] = permutation[offset + minIndex];
			permutation[offset + minIndex] = current;

			sortRange(permutation, offset + index + 1, offset + n);
			changed = index;
			index = -1;
		}
	}
	return changed;
}

// FlatSubtree
static inline int outInts(int n) {
	return OUT_CURSOR + 3 * n;
}

static inline int outBestPermutation(int n) {
	return OUT_CURSOR + n;
}

static inline int outBestRotations(int n) {
	return OUT_CURSOR + 2 * n;
}

// FlatSubtree.run: the status of the item
static int run(const Layout *l, __global const int *problem, __global const long *problemLongs, int depth, __global const int *prefixes, int item, __global int *s, __global long *sl, int base,
		int longBase, __global int *out, int outBase, __global long *outLongs, int outLongBase, long boundVolume, int budgetNodes) {
	const int n = problem[P_N];
	const int types = problem[P_TYPES];
	const int limit = problem[P_LIMIT];
	const bool allFit = problem[P_ALL_FIT] != 0;
	const long maxLoadVolume = problemLongs[maxLoad(types)];
	const long maxLoadWeight = problemLongs[maxLoad(types) + 1];
	const int weights = weightsOffset(types);

	const int cursor = outBase + OUT_CURSOR;
	const int bestPermutation = outBase + outBestPermutation(n);
	const int bestRotations = outBase + outBestRotations(n);
	const int permBase = base + l->perm;
	const int control = base + l->control;

	int status = out[outBase + OUT_STATUS];
	int result = status;
	bool running = status != DONE && status != OVERFLOW;
	if (running && status == NOT_STARTED) {
		for (int i = 0; i < n; i++) {
			out[cursor + i] = prefixes[item * n + i];
		}
		out[outBase + OUT_COUNT] = 0;
		outLongs[outLongBase + OUT_VOLUME] = 0L;
		outLongs[outLongBase + OUT_WEIGHT] = 0L;
	}

	int nodes = 0;
	while (running) {
		// the volume which the first boxes of the permutation can load at most
		int maxPackableCount;
		long firstVolume = 0L;
		if (allFit) {
			maxPackableCount = n;
		} else {
			long loadVolume = 0L;
			long loadWeight = 0L;
			int count = 0;
			while (count < n) {
				int type = out[cursor + count];
				long v = problemLongs[type];
				long w = problemLongs[weights + type];
				if (v > maxLoadVolume - loadVolume || w > maxLoadWeight - loadWeight) {
					break;
				}
				loadVolume += v;
				loadWeight += w;
				count++;
			}
			maxPackableCount = min(limit, count);
			for (int i = 0; i < maxPackableCount; i++) {
				firstVolume += problemLongs[out[cursor + i]];
			}
		}

		int size;
		if (!allFit && firstVolume < max(boundVolume, outLongs[outLongBase + OUT_VOLUME])) {
			// no rotation of this permutation can load more than the best arrangement
			size = maxPackableCount;
		} else {
			for (int i = 0; i < n; i++) {
				s[permBase + i] = out[cursor + i];
			}
			s[control + C_NODES] = 0;
			size = searchPermutation(l, problem, problemLongs, s, sl, base, longBase, maxPackableCount);
			if (size < 0) {
				out[outBase + OUT_STATUS] = OVERFLOW;
				result = OVERFLOW;
				running = false;
			} else {
				nodes += s[control + C_NODES];
				outLongs[outLongBase + OUT_NODES] += s[control + C_NODES];

				if (size > 0) {
					long v = sl[longBase + l->bestVolume];
					long w = 0L;
					for (int i = 0; i < size; i++) {
						w += problemLongs[weights + out[cursor + i]];
					}
					int count = out[outBase + OUT_COUNT];
					long bestVolume = outLongs[outLongBase + OUT_VOLUME];
					long bestWeight = outLongs[outLongBase + OUT_WEIGHT];
					// the earlier arrangement wins ties
					if (count == 0 || v > bestVolume || (v == bestVolume && (w > bestWeight || (w == bestWeight && size > count)))) {
						out[outBase + OUT_COUNT] = size;
						outLongs[outLongBase + OUT_VOLUME] = v;
						outLongs[outLongBase + OUT_WEIGHT] = w;
						for (int i = 0; i < n; i++) {
							out[bestPermutation + i] = out[cursor + i];
						}
						for (int i = 0; i < size; i++) {
							out[bestRotations + i] = s[base + l->bestRotation + i];
						}
					}
					if (size == n) {
						out[outBase + OUT_FULL] = 1;
						out[outBase + OUT_STATUS] = DONE;
						result = DONE;
						running = false;
					}
				}
			}
		}

		if (running) {
			// the next permutation which changes the boxes up to the first which could not be placed
			int next = nextPermutation(out, cursor, n, size);
			if (next == -1 || next < depth) {
				out[outBase + OUT_STATUS] = DONE;
				result = DONE;
				running = false;
			} else if (nodes >= budgetNodes) {
				out[outBase + OUT_STATUS] = PAUSED;
				result = PAUSED;
				running = false;
			}
		}
	}
	return result;
}

// FlatTree
// The smallest area and volume of the remaining boxes, for the level: the free points below them can hold none of the remaining boxes.
static void minimums(__global const long *problemLongs, __global const int *s, __global long *sl, int remainingBase, int minAreaBase, int minVolumeBase, int types, int minAreasIndex, int level) {
	long minArea = LONG_MAX;
	long minVolume = LONG_MAX;
	for (int t = 0; t < types; t++) {
		if (s[remainingBase + t] > 0) {
			if (problemLongs[minAreasIndex + t] < minArea) {
				minArea = problemLongs[minAreasIndex + t];
			}
			if (problemLongs[t] < minVolume) {
				minVolume = problemLongs[t];
			}
		}
	}
	sl[minAreaBase + level] = minArea;
	sl[minVolumeBase + level] = minVolume;
}

// the next type with boxes remaining after the given one in the type order (or the first, for -1), or -1
static int nextType(__global const int *problem, __global const int *s, int remainingBase, int typeOrderIndex, int types, int after) {
	int index = 0;
	if (after != -1) {
		while (problem[typeOrderIndex + index] != after) {
			index++;
		}
		index++;
	}
	int result = -1;
	while (result == -1 && index < types) {
		int t = problem[typeOrderIndex + index];
		if (s[remainingBase + t] > 0) {
			result = t;
		}
		index++;
	}
	return result;
}

// true if some remaining box fits the load left
static bool canPlaceAny(__global const long *problemLongs, __global const int *s, int remainingBase, int types, int weights, long volumeLeft, long weightLeft) {
	bool result = false;
	for (int t = 0; t < types && !result; t++) {
		if (s[remainingBase + t] > 0 && problemLongs[t] <= volumeLeft && problemLongs[weights + t] <= weightLeft) {
			result = true;
		}
	}
	return result;
}

// whether the prefix (count boxes, with the given volume and weight, and the key of the remaining boxes) plus the most the remaining boxes can add can beat neither the bound volume nor the best
static bool isHopelessTree(__global const long *problemLongs, __global const int *s, int remainingBase, int types, int weights, bool allFit, int keys, int boundsIndex, int key, long volume,
		long weight, int count, long maxLoadVolume, long maxLoadWeight, long boundVolume, int bestCount, long bestVolume, long bestWeight, bool anyBest) {
	long v;
	long w;
	int c;
	if (keys > 0 && !allFit) {
		v = volume + problemLongs[boundsIndex + 3 * key];
		w = weight + problemLongs[boundsIndex + 3 * key + 1];
		c = count + (int)problemLongs[boundsIndex + 3 * key + 2];
	} else {
		long remainingVolume = 0L;
		long remainingWeight = 0L;
		c = count;
		for (int t = 0; t < types; t++) {
			int k = s[remainingBase + t];
			remainingVolume += k * problemLongs[t];
			remainingWeight += k * problemLongs[weights + t];
			c += k;
		}
		v = volume + (remainingVolume < maxLoadVolume - volume ? remainingVolume : maxLoadVolume - volume);
		w = weight + (remainingWeight < maxLoadWeight - weight ? remainingWeight : maxLoadWeight - weight);
	}
	bool result = v < boundVolume;
	if (!result && bestCount > 0) {
		int compare;
		if (v != bestVolume) {
			compare = v < bestVolume ? -1 : 1;
		} else if (w != bestWeight) {
			compare = w < bestWeight ? -1 : 1;
		} else if (c != bestCount) {
			compare = c < bestCount ? -1 : 1;
		} else {
			compare = 0;
		}
		result = compare < (anyBest ? 1 : 0);
	}
	return result;
}

// 1 if the prefix of the placed types followed by the type is after the first level + 1 boxes of the permutation, -1 if before, 0 if equal
static int comparePrefix(__global const int *s, __global const int *out, int placedTypeBase, int permutation, int level, int type) {
	int result = 0;
	for (int i = 0; i < level && result == 0; i++) {
		if (s[placedTypeBase + i] != out[permutation + i]) {
			result = s[placedTypeBase + i] < out[permutation + i] ? -1 : 1;
		}
	}
	if (result == 0 && type != out[permutation + level]) {
		result = type < out[permutation + level] ? -1 : 1;
	}
	return result;
}

static int compareTypes(int a, int b) {
	return a == b ? 0 : (a < b ? -1 : 1);
}

// whether the arrangement of the prefix (count boxes, with the given volume and weight) beats the best: more volume, then weight, then boxes; among equal ones, for the loop's winner,
// the lexicographically first order. When any best arrangement will do, an equal one is not better.
static bool isBetter(__global const int *s, __global const int *out, int placedTypeBase, int remainingBase, int bestPermutation, int n, int count, int failingType,
		long volume, long weight, int bestCount, long bestVolume, long bestWeight, bool anyBest) {
	bool result;
	bool equal = false;
	if (bestCount == 0) {
		result = true;
	} else if (volume != bestVolume) {
		result = volume > bestVolume;
	} else if (weight != bestWeight) {
		result = weight > bestWeight;
	} else if (count != bestCount) {
		result = count > bestCount;
	} else {
		equal = true;
		result = !anyBest;
	}
	if (equal && result) {
		// the order of the arrangement against the order of the best, lexicographically, without writing it
		int compare = 0;
		int index = 0;
		while (compare == 0 && index < count) {
			compare = compareTypes(s[placedTypeBase + index], out[bestPermutation + index]);
			index++;
		}
		if (compare == 0 && failingType != -1 && index < n) {
			compare = compareTypes(failingType, out[bestPermutation + index]);
			index++;
		}
		// the remaining boxes in ascending order: taken of the type so far, of those remaining (less the failing one)
		int t = 0;
		int taken = 0;
		while (compare == 0 && index < n) {
			while (taken == s[remainingBase + t] - (t == failingType ? 1 : 0)) {
				t++;
				taken = 0;
			}
			compare = compareTypes(t, out[bestPermutation + index]);
			taken++;
			index++;
		}
		result = compare < 0;
	}
	return result;
}

// keep the arrangement of the prefix as the best: its order (the prefix, the failing type if any, and the remaining boxes in ascending order), its rotations, count, volume and weight
static void offerTree(const Layout *l, __global const int *s, __global int *out, __global long *outLongs, int outBase, int outLongBase, int base, int placedTypeBase, int remainingBase, int types,
		int count, int failingType, long volume, long weight) {
	const int n = l->n;
	out[outBase + OUT_COUNT] = count;
	outLongs[outLongBase + OUT_VOLUME] = volume;
	outLongs[outLongBase + OUT_WEIGHT] = weight;
	int from = base + l->placedRotation;
	int to = outBase + outBestRotations(n);
	for (int i = 0; i < count; i++) {
		out[to + i] = s[from + i];
	}
	int order = outBase + outBestPermutation(n);
	int index = 0;
	for (int i = 0; i < count; i++) {
		out[order + index] = s[placedTypeBase + i];
		index++;
	}
	if (failingType != -1) {
		out[order + index] = failingType;
		index++;
	}
	for (int t = 0; t < types; t++) {
		int k = s[remainingBase + t] - (t == failingType ? 1 : 0);
		for (int i = 0; i < k; i++) {
			out[order + index] = t;
			index++;
		}
	}
}

// FlatTree.run: the status of the item
static int runTree(const Layout *l, __global const int *problem, __global const long *problemLongs, int depth, __global const int *prefixes, int item, __global int *s, __global long *sl, int base,
		int longBase, __global int *out, int outBase, __global long *outLongs, int outLongBase, long boundVolume, long sharedVolume, long sharedWeight, int sharedCount, int budgetNodes) {
	const int n = problem[P_N];
	const int types = problem[P_TYPES];
	const int rotations = problem[P_ROTATIONS];
	const int limit = problem[P_LIMIT];
	const bool allFit = problem[P_ALL_FIT] != 0;
	const bool anyBest = problem[P_ANY_BEST] != 0;
	const int keys = problem[P_KEYS];
	const int counts = P_HEADER;
	const int rotationOffsetsIndex = rotationOffsets(types);
	const int rotationValuesIndex = rotationValues(types);
	const int typeOrderIndex = typeOrder(types, rotations);
	const int radixIndex = radix(types, rotations);
	const int minAreasIndex = minAreas(types);
	const int weights = weightsOffset(types);
	const long maxLoadVolume = problemLongs[maxLoad(types)];
	const long maxLoadWeight = problemLongs[maxLoad(types) + 1];
	const int boundsIndex = bounds(types);
	const int depthLimit = limit < n ? limit : n;

	const int control = base + l->control;
	const int typeAtBase = base + l->typeAt;
	const int fitBase = base + l->fit;
	const int placedTypeBase = base + l->placedType;
	const int remainingBase = base + l->remaining;
	const int rotationBase = base + l->rotation;
	const int placedRotationBase = base + l->placedRotation;
	const int minAreaBase = longBase + l->minArea;
	const int minVolumeBase = longBase + l->minVolume;
	const int placedVolumeBase = longBase + l->placedVolume;
	const int placedWeightBase = longBase + l->placedWeight;
	const int bestPermutation = outBase + outBestPermutation(n);
	const int prefix = item * n;

	int status = out[outBase + OUT_STATUS];
	int result = status;
	bool running = status != DONE && status != OVERFLOW;
	if (running && status == NOT_STARTED) {
		reset(l, s, base);
		for (int t = 0; t < types; t++) {
			s[remainingBase + t] = problem[counts + t];
		}
		s[control + C_TREE_LEVEL] = 0;
		s[control + C_TREE_DESCEND] = 1;
		s[control + C_TREE_KEY] = keys - 1;
		sl[placedVolumeBase] = 0L;
		sl[placedWeightBase] = 0L;
		out[outBase + OUT_COUNT] = 0;
		out[outBase + OUT_FULL] = 0;
		outLongs[outLongBase + OUT_VOLUME] = 0L;
		outLongs[outLongBase + OUT_WEIGHT] = 0L;
		clearPoints(l, s, base, problem[P_CX], problem[P_CY], problem[P_CZ]);
		minimums(problemLongs, s, sl, remainingBase, minAreaBase, minVolumeBase, types, minAreasIndex, 0);
		if (sl[minAreaBase] != 0L || sl[minVolumeBase] != 0L) {
			filterMinimums(l, s, base, 0, sl[minAreaBase], sl[minVolumeBase]);
		}
	}

	int level = s[control + C_TREE_LEVEL];
	bool descend = s[control + C_TREE_DESCEND] != 0;
	int key = s[control + C_TREE_KEY];
	int bestCount = out[outBase + OUT_COUNT];
	long bestVolume = outLongs[outLongBase + OUT_VOLUME];
	long bestWeight = outLongs[outLongBase + OUT_WEIGHT];
	// the best to beat for the bound: the best of the other items when it is better than this item's own
	int beatCount = bestCount;
	long beatVolume = bestVolume;
	long beatWeight = bestWeight;
	if (sharedCount > 0 && (bestCount == 0 || sharedVolume > bestVolume || (sharedVolume == bestVolume && (sharedWeight > bestWeight || (sharedWeight == bestWeight && sharedCount > bestCount))))) {
		beatCount = sharedCount;
		beatVolume = sharedVolume;
		beatWeight = sharedWeight;
	}
	int nodes = 0;
	while (running && level >= 0) {
		if (descend) {
			// a node: the prefix of the placed boxes of the levels below
			descend = false;
			if (nodes >= budgetNodes) {
				descend = true;
				result = PAUSED;
				running = false;
			} else {
				nodes++;
				long nodeVolume = sl[placedVolumeBase + level];
				long nodeWeight = sl[placedWeightBase + level];
				bool expand = level < depthLimit && canPlaceAny(problemLongs, s, remainingBase, types, weights, maxLoadVolume - nodeVolume, maxLoadWeight - nodeWeight);
				if (expand && (beatCount > 0 || boundVolume > 0L)
						&& isHopelessTree(problemLongs, s, remainingBase, types, weights, allFit, keys, boundsIndex, key, nodeVolume, nodeWeight, level, maxLoadVolume, maxLoadWeight, boundVolume,
								beatCount, beatVolume, beatWeight, anyBest)) {
					// no completion of this prefix can beat the best
					expand = false;
				} else if (!expand && level > 0) {
					// every box which may be placed is placed, or none of the remaining ones fits the load left: a leaf
					if (isBetter(s, out, placedTypeBase, remainingBase, bestPermutation, n, level, -1, nodeVolume, nodeWeight, bestCount, bestVolume, bestWeight, anyBest)) {
						bestCount = level;
						bestVolume = nodeVolume;
						bestWeight = nodeWeight;
						if (beatCount == 0 || nodeVolume > beatVolume || (nodeVolume == beatVolume && (nodeWeight > beatWeight || (nodeWeight == beatWeight && level > beatCount)))) {
							beatCount = level;
							beatVolume = nodeVolume;
							beatWeight = nodeWeight;
						}
						offerTree(l, s, out, outLongs, outBase, outLongBase, base, placedTypeBase, remainingBase, types, level, -1, nodeVolume, nodeWeight);
						if (level == n) {
							out[outBase + OUT_FULL] = 1;
							if (anyBest) {
								result = DONE;
								running = false;
							}
						}
					}
				}
				if (!expand) {
					s[typeAtBase + level] = -1;
				} else {
					s[typeAtBase + level] = level < depth ? prefixes[prefix + level] : nextType(problem, s, remainingBase, typeOrderIndex, types, -1);
					s[rotationBase + level] = 0;
					s[fitBase + level] = 0;
				}
			}
		}

		if (running) {
			const int t = s[typeAtBase + level];
			if (t == -1) {
				// the node is done: back to the level below, where the next rotation of its type follows
				level--;
				if (level >= 0) {
					int placed = s[placedTypeBase + level];
					s[remainingBase + placed]++;
					key += problem[radixIndex + placed];
				}
			} else {
				bool skip = false;
				if (s[rotationBase + level] == 0 && s[fitBase + level] == 0) {
					// the box is new at this node: a box over the load limits ends the arrangement; after a full arrangement only earlier orders can
					// win; a hopeless box is not placed
					long nodeVolume = sl[placedVolumeBase + level];
					long nodeWeight = sl[placedWeightBase + level];
					if (problemLongs[t] > maxLoadVolume - nodeVolume || problemLongs[weights + t] > maxLoadWeight - nodeWeight) {
						if (level > 0 && isBetter(s, out, placedTypeBase, remainingBase, bestPermutation, n, level, t, nodeVolume, nodeWeight, bestCount, bestVolume, bestWeight, anyBest)) {
							bestCount = level;
							bestVolume = nodeVolume;
							bestWeight = nodeWeight;
							if (beatCount == 0 || nodeVolume > beatVolume || (nodeVolume == beatVolume && (nodeWeight > beatWeight || (nodeWeight == beatWeight && level > beatCount)))) {
								beatCount = level;
								beatVolume = nodeVolume;
								beatWeight = nodeWeight;
							}
							offerTree(l, s, out, outLongs, outBase, outLongBase, base, placedTypeBase, remainingBase, types, level, t, nodeVolume, nodeWeight);
						}
						skip = true;
					} else if (bestCount == n && !anyBest && comparePrefix(s, out, placedTypeBase, bestPermutation, level, t) > 0) {
						skip = true;
					} else if (beatCount > 0 || boundVolume > 0L) {
						s[remainingBase + t]--;
						skip = isHopelessTree(problemLongs, s, remainingBase, types, weights, allFit, keys, boundsIndex, key - problem[radixIndex + t], nodeVolume + problemLongs[t],
								nodeWeight + problemLongs[weights + t], level + 1, maxLoadVolume, maxLoadWeight, boundVolume, beatCount, beatVolume, beatWeight, anyBest);
						s[remainingBase + t]++;
					}
				}
				if (skip) {
					s[typeAtBase + level] = level < depth ? -1 : nextType(problem, s, remainingBase, typeOrderIndex, types, t);
					s[rotationBase + level] = 0;
					s[fitBase + level] = 0;
				} else {
					// the next rotation of the box which has a position
					const int firstRotation = problem[rotationOffsetsIndex + t];
					const int rotationCount = problem[rotationOffsetsIndex + t + 1] - firstRotation;
					const int frame = base + frameOffset(l, level);
					int rotationIndex = s[rotationBase + level];
					int pointIndex = -1;
					int dx = 0;
					int dy = 0;
					int dz = 0;
					while (pointIndex == -1 && rotationIndex < rotationCount) {
						int rotation = rotationValuesIndex + 3 * (firstRotation + rotationIndex);
						dx = problem[rotation];
						dy = problem[rotation + 1];
						dz = problem[rotation + 2];
						rotationIndex++;
						pointIndex = bestPoint(s, frame, dx, dy, dz);
					}
					s[rotationBase + level] = rotationIndex;

					if (pointIndex != -1) {
						// place the box and continue with the next level
						int point = frame + 1 + pointIndex * POINT_INTS;
						int x = s[point];
						int y = s[point + 1];
						int z = s[point + 2];
						int nextLevel = level + 1;
						s[remainingBase + t]--;
						key -= problem[radixIndex + t];
						sl[placedVolumeBase + nextLevel] = sl[placedVolumeBase + level] + problemLongs[t];
						sl[placedWeightBase + nextLevel] = sl[placedWeightBase + level] + problemLongs[weights + t];
						s[placedTypeBase + level] = t;
						s[placedRotationBase + level] = rotationIndex - 1;
						s[fitBase + level] = 1;
						// when no box can follow, the free space after the box is never searched
						bool last = nextLevel == depthLimit || !canPlaceAny(problemLongs, s, remainingBase, types, weights, maxLoadVolume - sl[placedVolumeBase + nextLevel],
								maxLoadWeight - sl[placedWeightBase + nextLevel]);
						if (!last) {
							setPlacement(l, s, base, level, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
							int count = addPoint(l, s, base, level, pointIndex, level, sl[minAreaBase + level], sl[minVolumeBase + level]);
							if (count < 0) {
								result = OVERFLOW;
								running = false;
							} else {
								minimums(problemLongs, s, sl, remainingBase, minAreaBase, minVolumeBase, types, minAreasIndex, nextLevel);
								if (sl[minAreaBase + nextLevel] != sl[minAreaBase + level] || sl[minVolumeBase + nextLevel] != sl[minVolumeBase + level]) {
									filterMinimums(l, s, base, nextLevel, sl[minAreaBase + nextLevel], sl[minVolumeBase + nextLevel]);
								}
							}
						}
						level = nextLevel;
						descend = true;
					} else {
						if (s[fitBase + level] == 0 && level > 0) {
							// no rotation of the box has a position: the arrangement ends at this box
							long nodeVolume = sl[placedVolumeBase + level];
							long nodeWeight = sl[placedWeightBase + level];
							if (isBetter(s, out, placedTypeBase, remainingBase, bestPermutation, n, level, t, nodeVolume, nodeWeight, bestCount, bestVolume, bestWeight, anyBest)) {
								bestCount = level;
								bestVolume = nodeVolume;
								bestWeight = nodeWeight;
								if (beatCount == 0 || nodeVolume > beatVolume || (nodeVolume == beatVolume && (nodeWeight > beatWeight || (nodeWeight == beatWeight && level > beatCount)))) {
									beatCount = level;
									beatVolume = nodeVolume;
									beatWeight = nodeWeight;
								}
								offerTree(l, s, out, outLongs, outBase, outLongBase, base, placedTypeBase, remainingBase, types, level, t, nodeVolume, nodeWeight);
							}
						}
						s[typeAtBase + level] = level < depth ? -1 : nextType(problem, s, remainingBase, typeOrderIndex, types, t);
						s[rotationBase + level] = 0;
						s[fitBase + level] = 0;
					}
				}
			}
		}
	}

	if (running) {
		// every node of the item is searched
		result = DONE;
	}
	if (result == OVERFLOW) {
		reset(l, s, base);
		s[control + C_OVERFLOW] = 1;
	}
	s[control + C_TREE_LEVEL] = level;
	s[control + C_TREE_DESCEND] = descend ? 1 : 0;
	s[control + C_TREE_KEY] = key;
	out[outBase + OUT_STATUS] = result;
	outLongs[outLongBase + OUT_NODES] += nodes;
	return result;
}

// The kernel: the parameters after the four of the TornadoVM calling convention are the Java arrays of TornadoBruteForceAccelerator, in its order.
__kernel void search(__global long *_kernel_context, __constant uchar *_constant_region, __local uchar *_local_region, __global int *_atomics,
		__global uchar *layoutBuffer, __global uchar *problemBuffer, __global uchar *problemLongsBuffer, __global uchar *activeBuffer, __global uchar *prefixesBuffer,
		__global uchar *parametersBuffer, __global uchar *longParametersBuffer, __global uchar *sBuffer, __global uchar *slBuffer, __global uchar *outBuffer, __global uchar *outLongsBuffer) {
	__global const int *layoutInts = (__global const int *)(layoutBuffer + HEADER);
	__global const int *problem = (__global const int *)(problemBuffer + HEADER);
	__global const long *problemLongs = (__global const long *)(problemLongsBuffer + HEADER);
	__global const int *active = (__global const int *)(activeBuffer + HEADER);
	__global const int *prefixes = (__global const int *)(prefixesBuffer + HEADER);
	__global const int *parameters = (__global const int *)(parametersBuffer + HEADER);
	__global const long *longParameters = (__global const long *)(longParametersBuffer + HEADER);
	__global int *s = (__global int *)(sBuffer + HEADER);
	__global long *sl = (__global long *)(slBuffer + HEADER);
	__global int *out = (__global int *)(outBuffer + HEADER);
	__global long *outLongs = (__global long *)(outLongsBuffer + HEADER);

	int slot = get_global_id(0);
	if (slot >= parameters[P_SLOTS] || active[slot] == 0) {
		return;
	}
	Layout l = readLayout(layoutInts);
	int n = problem[P_N];
	int base = slot * l.size;
	int longBase = slot * l.longSize;
	if (parameters[P_TREE] != 0) {
		// a tree item keeps its scratch between the rounds: FlatTree.run resets it when the item has not started
		runTree(&l, problem, problemLongs, parameters[P_DEPTH], prefixes, slot, s, sl, base, longBase, out, slot * outInts(n), outLongs, slot * OUT_LONGS, longParameters[P_BOUND],
				longParameters[P_SHARED_VOLUME], longParameters[P_SHARED_WEIGHT], (int)longParameters[P_SHARED_COUNT], parameters[P_BUDGET]);
	} else {
		reset(&l, s, base);
		run(&l, problem, problemLongs, parameters[P_DEPTH], prefixes, slot, s, sl, base, longBase, out, slot * outInts(n), outLongs, slot * OUT_LONGS, longParameters[P_BOUND], parameters[P_BUDGET]);
	}
}
