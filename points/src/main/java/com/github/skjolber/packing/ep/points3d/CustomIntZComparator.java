package com.github.skjolber.packing.ep.points3d;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

/**
 * Canonical total order for the points which are moved in the z direction, i.e. to the z coordinate after a placement (<code>zz</code>). The arguments are indexes into the values.
 * <p>
 * The moved form of a point has min z = <code>zz</code> and keeps the min and max x and y, and the max z, of the source point. The calculator adds the moved forms in this order, and
 * skips a moved form which is eclipsed by one that was already added (<code>eclipsesMovedZ</code>), so this order decides which points remain. It is a total order, so the result does not
 * depend on the sorting algorithm (a stable sort and a quicksort give the same result), nor on the order of the points before they are sorted.
 * <p>
 * The key of a source point, in order of significance:
 * <ol>
 * <li>min x, ascending
 * <li>min y, ascending
 * <li>volume proxy <code>dx * dy * maxZ</code>, descending (not the exact volume, but monotone in the extents)
 * <li>max x, max y and max z, each descending
 * <li>supports of the moved form, <i>richer first</i>: the planes which the move to <code>zz</code> keeps, with the xz plane before the yz plane
 * (so a moved form with both is first, then xz only, then yz only, then neither)
 * <li>min z, ascending
 * <li>supports of the source point: xy plane, then xz plane, then yz plane (present first)
 * </ol>
 * Keys 1-4 are <b>monotone with eclipsing</b>: if the moved form of a point A eclipses the moved form of a point B, i.e. A's max x, max y and max z are at least B's, and A's min x and
 * min y are at most B's, then A sorts strictly before B, unless the two moved forms are geometrically identical (the same min x and y, and max x, y and z). So a moved form is never added
 * before a form which eclipses it, and what remains is exactly the set of maximal moved forms, whatever the order of the points before sorting.
 * (For a point which can be moved the max z is above 0, so the volume proxy of key 3 is already strictly larger for the eclipsing point, unless the moved forms are identical; key 4 orders
 * the points which do not eclipse each other, so that the order is total, and keeps it monotone for points which cannot be moved.)
 * Keys 5-7 only apply to moved forms which are geometrically identical: the first of them is added, the others are dropped as eclipsed, so key 5 decides that the form which
 * keeps the richest supports remains. Keys 6 and 7 are only for totality: two distinct points compare equal only if they have the same coordinates and the same supporting planes
 * (the same placements: placements do not overlap, so two planes of the same kind at one corner are the same placement), i.e. if they cannot be told apart.
 * <p>
 * Keys 1-3 are those of the earlier, partial order, which left ties to the quicksort, which handled them in an arbitrary order.
 */
public class CustomIntZComparator implements IntComparator {

	private static final long serialVersionUID = 1L;

	private Point3DFlagList values;

	/** the z coordinate which the points are moved to */
	private int zz;

	@Override
	public int compare(int value1, int value2) {
		return compare(values.get(value1), values.get(value2));
	}

	public int compare(SimplePoint3D o1, SimplePoint3D o2) {
		if(o1.getMinX() < o2.getMinX()) {
			return -1;
		} else if(o1.getMinX() != o2.getMinX()) {
			return 1;
		}

		if(o1.getMinY() < o2.getMinY()) {
			return -1;
		} else if(o1.getMinY() != o2.getMinY()) {
			return 1;
		}

		// not exact volume, but good enough for comparison: monotone in the extents
		long volume1 = o1.getArea() * o1.getMaxZ();
		long volume2 = o2.getArea() * o2.getMaxZ();

		// inline -Long.compare(volume1, volume2)
		if(volume1 != volume2) {
			return (volume2 < volume1) ? -1 : 1;
		}

		return compareTied(o1, o2);
	}

	/** The keys after the volume proxy; only reached for points with the same min x and y, and the same volume proxy. */
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
		int supports1 = supports(o1.moveZ(zz));
		int supports2 = supports(o2.moveZ(zz));
		if(supports1 != supports2) {
			return supports1 > supports2 ? -1 : 1;
		}

		// only for totality
		if(o1.getMinZ() != o2.getMinZ()) {
			return o1.getMinZ() < o2.getMinZ() ? -1 : 1;
		}
		supports1 = supports(o1);
		supports2 = supports(o2);
		if(supports1 != supports2) {
			return supports1 > supports2 ? -1 : 1;
		}
		return 0;
	}

	/** The xy plane is worth more than the xz plane, which is worth more than the yz plane. */
	private static int supports(SimplePoint3D point) {
		return (point.isSupportedXYPlane() ? 4 : 0) + (point.isSupportedXZPlane() ? 2 : 0) + (point.isSupportedYZPlane() ? 1 : 0);
	}

	/**
	 * @param values the points which the indexes refer to
	 * @param zz the z coordinate which the points are moved to
	 */
	public void setValues(Point3DFlagList values, int zz) {
		this.values = values;
		this.zz = zz;
	}
}
