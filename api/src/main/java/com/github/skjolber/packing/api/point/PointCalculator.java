package com.github.skjolber.packing.api.point;

import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Placement;

/**
 * Keeps track of the free space of a container as a list of points (the "extreme points" method): the points are the
 * places where the next box can be put. Adding a placement removes and constrains the points which it overlaps, and adds
 * the points which it creates. The points of the calculator are available through {@link PointSource}.
 * <p>
 * Typical use: {@link #clearToSize(int, int, int)} for a container, optionally {@link #setPoints(List)} and {@link #clear()}
 * for initial points (for example obstacles), {@link #setMinimumAreaAndVolumeLimit(long, long)} for the remaining boxes, and
 * then {@link #add(int, Placement)} for each box which is placed.
 * <p>
 * Instances are not thread-safe.
 */
public interface PointCalculator extends PointSource {

	/**
	 * Add a placement at a point, i.e. use up the space of the placement. The placement's position must be within the point.
	 * The points which the placement overlaps are removed or constrained, and the points which it creates are added.
	 *
	 * @param point a point of this calculator
	 * @param placement the placement, with absolute coordinates
	 * @return true if any free points remain after the placement was added, false if the container is full
	 */
	boolean add(Point point, Placement placement);

	/**
	 * Add a placement at a point, see {@link #add(Point, Placement)}.
	 *
	 * @param index the index of a point in this calculator, see {@link #get(int)}
	 * @param placement the placement, with absolute coordinates
	 * @return true if any free points remain after the placement was added, false if the container is full
	 */
	boolean add(int index, Placement placement);

	/**
	 * Set the size of the container and {@linkplain #clear() clear} the calculator: there are no placements, the free
	 * space is the whole container (or the initial points, see {@link #setPoints(List)}) and the minimum area and volume
	 * limits are removed.
	 *
	 * @param dx container size along the x axis
	 * @param dy container size along the y axis
	 * @param dz container size along the z axis
	 */
	void clearToSize(int dx, int dy, int dz);

	/**
	 * Remove all placements, and restore the points of an empty container: the initial points if some were set with
	 * {@link #setPoints(List)}, otherwise one point which spans the whole container. The minimum area and volume limits are
	 * removed.
	 */
	void clear();

	/**
	 * @return the placements which were added since the last {@link #clear()}
	 */
	List<Placement> getPlacements();

	/**
	 * @return the total volume of the boxes of the placements
	 */
	long calculateUsedVolume();

	/**
	 * @return the total weight of the boxes of the placements
	 */
	long calculateUsedWeight();

	/**
	 * Set the initial points, for example the space around obstacles. The points must be within the container (the size
	 * must be set first, see {@link #clearToSize(int, int, int)}). The current points are unchanged: the initial points
	 * are used by the next {@link #clear()}.
	 *
	 * @param points the free points of an empty container
	 */
	void setPoints(List<Point> points);

	/**
	 * Set the initial points, limited to a box (for example a level of the container): points outside the box are skipped, and
	 * the others are limited to the box. The current points are unchanged: the initial points are used by the next
	 * {@link #clear()}.
	 *
	 * @param points the free points of an empty container
	 * @return true if any of the points are within the box, false if there is no free space within it
	 */
	boolean setPoints(List<Point> points, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

	/**
	 * Set the area and volume of the smallest remaining box. Points which cannot hold a box of at least that area and volume
	 * are removed, and are not created by later placements. Removed points do not come back when the limits are lowered;
	 * {@link #clear()} resets the limits (to zero, i.e. no limits).
	 *
	 * @param area the smallest area (footprint) of the remaining boxes, or zero for no limit
	 * @param volume the smallest volume of the remaining boxes, or zero for no limit
	 */
	void setMinimumAreaAndVolumeLimit(long area, long volume);

	/**
	 * Filter the points: the points which satisfy the test are kept, the others are removed. Despite the name, a
	 * point is removed when the test returns false.
	 *
	 * @param test returns true for the points to keep
	 */
	void remove(Predicate<Point> test);

	/**
	 * @return true if there are no free points
	 */
	boolean isEmpty();

}
