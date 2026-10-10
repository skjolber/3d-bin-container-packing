package com.github.skjolber.packing.points;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

/**
 * Order-insensitive view of the free points of a 3D point calculator, for comparing with the free points of version 4.
 * <p>
 * The 3D calculator uses a canonical total order for the points it moves, where version 4 left ties to a quicksort, so the two do not have the same points in the same order.
 * The free points are therefore compared as sets of rows ({@linkplain #canonical(int[])}), and by the free space which they cover ({@linkplain #firstUncovered(int[][], int[][], long, long)}).
 * <p>
 * A point is a row of {@linkplain #POINT} numbers (the coordinates, then the supports). The coordinates are inclusive, so a point has the size
 * {@code max - min + 1} in each direction.
 */
final class CanonicalPoints3D {

	/** The coordinates and supports of a point: min x, y, z, max x, y, z, then six supports */
	static final int POINT = 12;

	private CanonicalPoints3D() {
	}

	/**
	 * @return the points in calculator order, {@linkplain #POINT} numbers each
	 */
	static int[] flatten(DefaultPointCalculator3D calculator) {
		int[] values = new int[calculator.size() * POINT];
		for(int i = 0; i < calculator.size(); i++) {
			SimplePoint3D p = calculator.get(i);
			put(values, i * POINT, p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ());
			put(values, i * POINT + 6, p.isSupportedXYPlane(), p.isSupportedXZPlane(), p.isSupportedYZPlane(), p.isSupportedXYPlane(p.getMaxX(), p.getMaxY()),
					p.isSupportedXZPlane(p.getMaxX(), p.getMaxZ()), p.isSupportedYZPlane(p.getMaxY(), p.getMaxZ()));
		}
		return values;
	}

	/**
	 * @return the points in calculator order, {@linkplain #POINT} numbers each
	 */
	static int[] flatten(com.github.skjolber.packing.v4.ep.points3d.DefaultPointCalculator3D calculator) {
		int[] values = new int[calculator.size() * POINT];
		for(int i = 0; i < calculator.size(); i++) {
			com.github.skjolber.packing.v4.ep.points3d.SimplePoint3D p = calculator.get(i);
			put(values, i * POINT, p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ());
			put(values, i * POINT + 6, p.isSupportedXYPlane(), p.isSupportedXZPlane(), p.isSupportedYZPlane(), p.isSupportedXYPlane(p.getMaxX(), p.getMaxY()),
					p.isSupportedXZPlane(p.getMaxX(), p.getMaxZ()), p.isSupportedYZPlane(p.getMaxY(), p.getMaxZ()));
		}
		return values;
	}

	/**
	 * @return the points as rows, in the canonical order: by the numbers of each row, in order (i.e. min x, y, z, then max x, y, z, then the supports).
	 */
	static int[][] canonical(int[] values) {
		int[][] rows = new int[values.length / POINT][];
		for(int i = 0; i < rows.length; i++) {
			rows[i] = Arrays.copyOfRange(values, i * POINT, (i + 1) * POINT);
		}
		Arrays.sort(rows, Arrays::compare);
		return rows;
	}

	/**
	 * @param first points in the canonical order
	 * @param second points in the canonical order
	 * @return the points (with supports) of the first which are not in the second, in the canonical order
	 */
	static int[][] difference(int[][] first, int[][] second) {
		int[][] difference = new int[first.length][];
		int count = 0;
		int j = 0;
		for(int i = 0; i < first.length; i++) {
			while(j < second.length && Arrays.compare(second[j], first[i]) < 0) {
				j++;
			}
			if(j >= second.length || Arrays.compare(second[j], first[i]) != 0) {
				difference[count++] = first[i];
			}
		}
		return Arrays.copyOf(difference, count);
	}

	/**
	 * Find a point whose free space is not covered. This is checked in two steps, the cheap one first: a point which is within (contained in) a single point of the cover is
	 * covered. Otherwise the point is covered if every part of it is within some point of the cover, i.e. if it is within the union of the points of the cover (the free space is
	 * identical, but may be tiled into different points), see {@linkplain #remainder(int[], int[][])}.
	 * <p>
	 * A point which is smaller than the limits can hold none of the remaining boxes, and is not checked: its area (size x times size y) is less than the minimum area, or its volume is
	 * less than the minimum volume.
	 *
	 * @param points the points
	 * @param cover the points which should cover them
	 * @param minArea the minimum area limit in force (the points which do not reach it are not checked)
	 * @param minVolume the minimum volume limit in force (the points which do not reach it are not checked)
	 * @return the first point which is not covered, or null
	 */
	static int[] firstUncovered(int[][] points, int[][] cover, long minArea, long minVolume) {
		points: for(int[] row : points) {
			if(isBelowLimits(row, minArea, minVolume)) {
				continue;
			}
			for(int[] other : cover) {
				if(isWithin(row, other)) {
					continue points;
				}
			}
			if(remainder(row, cover).isEmpty()) {
				continue;
			}
			return row;
		}
		return null;
	}

	/**
	 * @return true if the area or the volume of the point is less than the limits
	 */
	static boolean isBelowLimits(int[] row, long minArea, long minVolume) {
		long area = (long)(row[3] - row[0] + 1) * (row[4] - row[1] + 1);
		return area < minArea || area * (row[5] - row[2] + 1) < minVolume;
	}

	/**
	 * @return true if the first is contained in the second
	 */
	static boolean isWithin(int[] row, int[] other) {
		return other[0] <= row[0] && other[1] <= row[1] && other[2] <= row[2] && other[3] >= row[3] && other[4] >= row[4] && other[5] >= row[5];
	}

	/**
	 * Exact: subtract the cover from the point, one box at a time. Each subtraction splits a part which the box overlaps into at most six boxes (x below, x above, then y below and y
	 * above within the x range, then z below and z above within the x and y range). The point is within the union of the cover if nothing remains.
	 *
	 * @param row the point
	 * @param cover the points which should cover it
	 * @return the parts of the point which are not within any point of the cover, as boxes of six numbers (the coordinates, inclusive); empty if the point is covered
	 */
	static List<int[]> remainder(int[] row, int[][] cover) {
		List<int[]> parts = new ArrayList<>();
		parts.add(Arrays.copyOf(row, 6));
		for(int[] other : cover) {
			if(parts.isEmpty()) {
				break;
			}
			if(!isOverlapping(row, other)) {
				continue;
			}
			List<int[]> remaining = new ArrayList<>(parts.size() + 5);
			for(int[] part : parts) {
				subtract(part, other, remaining);
			}
			parts = remaining;
		}
		return parts;
	}

	private static boolean isOverlapping(int[] box, int[] other) {
		return box[0] <= other[3] && other[0] <= box[3] && box[1] <= other[4] && other[1] <= box[4] && box[2] <= other[5] && other[2] <= box[5];
	}

	/**
	 * Add the parts of the box which are not within the other, to the result.
	 */
	private static void subtract(int[] box, int[] other, List<int[]> result) {
		if(!isOverlapping(box, other)) {
			result.add(box);
			return;
		}
		int minX = box[0];
		int minY = box[1];
		int minZ = box[2];
		int maxX = box[3];
		int maxY = box[4];
		int maxZ = box[5];
		if(minX < other[0]) {
			result.add(new int[] { minX, minY, minZ, other[0] - 1, maxY, maxZ });
			minX = other[0];
		}
		if(maxX > other[3]) {
			result.add(new int[] { other[3] + 1, minY, minZ, maxX, maxY, maxZ });
			maxX = other[3];
		}
		if(minY < other[1]) {
			result.add(new int[] { minX, minY, minZ, maxX, other[1] - 1, maxZ });
			minY = other[1];
		}
		if(maxY > other[4]) {
			result.add(new int[] { minX, other[4] + 1, minZ, maxX, maxY, maxZ });
			maxY = other[4];
		}
		if(minZ < other[2]) {
			result.add(new int[] { minX, minY, minZ, maxX, maxY, other[2] - 1 });
		}
		if(maxZ > other[5]) {
			result.add(new int[] { minX, minY, other[5] + 1, maxX, maxY, maxZ });
		}
	}

	/**
	 * @param first points in the canonical order
	 * @param second points in the canonical order
	 * @return the points (with supports) which are in both, in the canonical order
	 */
	static int[][] common(int[][] first, int[][] second) {
		int[][] common = new int[Math.min(first.length, second.length)][];
		int count = 0;
		int i = 0;
		int j = 0;
		while(i < first.length && j < second.length) {
			int compare = Arrays.compare(first[i], second[j]);
			if(compare == 0) {
				common[count++] = first[i];
				i++;
				j++;
			} else if(compare < 0) {
				i++;
			} else {
				j++;
			}
		}
		return Arrays.copyOf(common, count);
	}

	/**
	 * @return the position in the calculator order of the first point equal to the row
	 */
	static int indexOf(int[] values, int[] row) {
		for(int offset = 0; offset < values.length; offset += POINT) {
			if(Arrays.equals(values, offset, offset + POINT, row, 0, POINT)) {
				return offset / POINT;
			}
		}
		throw new IllegalArgumentException("Unknown point " + Arrays.toString(row));
	}

	private static void put(int[] values, int offset, int... coordinates) {
		System.arraycopy(coordinates, 0, values, offset, coordinates.length);
	}

	private static void put(int[] values, int offset, boolean... supports) {
		for(int i = 0; i < supports.length; i++) {
			values[offset + i] = supports[i] ? 1 : 0;
		}
	}
}
