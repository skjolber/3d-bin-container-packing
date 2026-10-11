package com.github.skjolber.packing.ep.points3d;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

/**
 * Canonical total order for the points which are moved in the x direction, i.e. to the x coordinate after a placement (<code>xx</code>). The arguments are indexes into the values.
 * <p>
 * The moved form of a point has min x = <code>xx</code> and keeps the min and max y and z, and the max x, of the source point. The calculator adds the moved forms in this order, and
 * skips a moved form which is eclipsed by one that was already added (<code>eclipsesMovedX</code>), so this order decides which points remain. It is a total order, so the result does not
 * depend on the sorting algorithm (a stable sort and a quicksort give the same result), nor on the order of the points before they are sorted.
 * <p>
 * The key of a source point, in order of significance:
 * <ol>
 * <li>min y, ascending
 * <li>min z, ascending
 * <li>volume proxy <code>dy * dz * maxX</code>, descending (not the exact volume, but monotone in the extents; computed as a long)
 * <li>max x, max y and max z, each descending
 * <li>supports of the moved form, <i>richer first</i>: the planes which the move to <code>xx</code> keeps, with the xy plane before the xz plane
 * (so a moved form with both is first, then xy only, then xz only, then neither)
 * <li>min x, ascending
 * <li>supports of the source point: xy plane, then xz plane, then yz plane (present first)
 * </ol>
 * Keys 1-4 are <b>monotone with eclipsing</b>: if the moved form of a point A eclipses the moved form of a point B, i.e. A's max x, max y and max z are at least B's, and A's min y and
 * min z are at most B's, then A sorts strictly before B, unless the two moved forms are geometrically identical (the same min y and z, and max x, y and z). So a moved form is never added
 * before a form which eclipses it, and what remains is exactly the set of maximal moved forms, whatever the order of the points before sorting.
 * (For a point which can be moved the max x is above 0, so the volume proxy of key 3 is already strictly larger for the eclipsing point, unless the moved forms are identical; key 4 orders
 * the points which do not eclipse each other, so that the order is total, and keeps it monotone for points which cannot be moved.)
 * Keys 5-7 only apply to moved forms which are geometrically identical: the first of them is added, the others are dropped as eclipsed, so key 5 decides that the form which
 * keeps the richest supports remains. Keys 6 and 7 are only for totality: two distinct points compare equal only if they have the same coordinates and the same supporting planes
 * (the same placements: placements do not overlap, so two planes of the same kind at one corner are the same placement), i.e. if they cannot be told apart.
 * <p>
 * Keys 1-3 are those of the earlier, partial order (except that the volume proxy no longer wraps), which left ties to the quicksort, which handled them in an arbitrary order.
 */
public class CustomIntXComparator implements IntComparator {

	private static final long serialVersionUID = 1L;

	private Point3DFlagList values;

	/** the x coordinate which the points are moved to */
	private int xx;

	@Override
	public int compare(int value1, int value2) {
		return compare(values.get(value1), values.get(value2));
	}

	public int compare(SimplePoint3D o1, SimplePoint3D o2) {
		if(o1.getMinY() < o2.getMinY()) {
			return -1;
		} else if(o1.getMinY() != o2.getMinY()) {
			return 1;
		}

		if(o1.getMinZ() < o2.getMinZ()) {
			return -1;
		} else if(o1.getMinZ() != o2.getMinZ()) {
			return 1;
		}

		// not exact volume, but good enough for comparison: monotone in the extents.
		// calculated as a long, so that it does not wrap for large containers
		long volume1 = (long)o1.getDy() * o1.getDz() * o1.getMaxX();
		long volume2 = (long)o2.getDy() * o2.getDz() * o2.getMaxX();

		// inline -Long.compare(volume1, volume2)
		if(volume1 != volume2) {
			return (volume2 < volume1) ? -1 : 1;
		}

		return compareTied(o1, o2);
	}

	/** The keys after the volume proxy; only reached for points with the same min y and z, and the same volume proxy. */
	private int compareTied(SimplePoint3D o1, SimplePoint3D o2) {
		if(o1.getMaxX() != o2.getMaxX()) {
			return o1.getMaxX() > o2.getMaxX() ? -1 : 1;
		}
		if(o1.getMaxY() != o2.getMaxY()) {
			return o1.getMaxY() > o2.getMaxY() ? -1 : 1;
		}
		if(o1.getMaxZ() != o2.getMaxZ()) {
			return o1.getMaxZ() > o2.getMaxZ() ? -1 : 1;
		}

		// the moved forms are geometrically identical: richest supports first
		int supports1 = supports(o1.moveX(xx));
		int supports2 = supports(o2.moveX(xx));
		if(supports1 != supports2) {
			return supports1 > supports2 ? -1 : 1;
		}

		// only for totality
		if(o1.getMinX() != o2.getMinX()) {
			return o1.getMinX() < o2.getMinX() ? -1 : 1;
		}
		supports1 = supports(o1);
		supports2 = supports(o2);
		if(supports1 != supports2) {
			return supports1 > supports2 ? -1 : 1;
		}
		return 0;
	}

	/** The xy plane is worth more than the xz plane. */
	private static int supports(SimplePoint3D point) {
		return (point.isSupportedXYPlane() ? 4 : 0) + (point.isSupportedXZPlane() ? 2 : 0) + (point.isSupportedYZPlane() ? 1 : 0);
	}

	/**
	 * @param values the points which the indexes refer to
	 * @param xx the x coordinate which the points are moved to
	 */
	public void setValues(Point3DFlagList values, int xx) {
		this.values = values;
		this.xx = xx;
	}

	/**
	 * Stable insertion sort of point indexes (into the values) by this comparator. The moved points arrive nearly sorted (they follow the x, y, z order of the points they were moved
	 * from) and the lists are short, so an insertion sort is cheaper than a quicksort. The order is total, so the result is the same as that of any other sort.
	 * <p>
	 * The loop is repeated for each axis rather than shared, so that the comparison is a direct call which the JIT can inline.
	 *
	 * @param indexes the indexes
	 * @param size the number of indexes to sort
	 */
	public void insertionSort(int[] indexes, int size) {
		final Point3DFlagList values = this.values;
		for (int i = 1; i < size; i++) {
			int index = indexes[i];
			SimplePoint3D point = values.get(index);
			int j = i - 1;
			while (j >= 0 && compare(values.get(indexes[j]), point) > 0) {
				indexes[j + 1] = indexes[j];
				j--;
			}
			indexes[j + 1] = index;
		}
	}
}
