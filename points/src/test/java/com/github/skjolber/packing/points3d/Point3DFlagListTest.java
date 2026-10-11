package com.github.skjolber.packing.points3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

/**
 * The hot paths which shrink a list ({@link Point3DFlagList#removeFlagged()}, {@link Point3DFlagList#copyInto(Point3DFlagList)}
 * and {@link Point3DFlagList#resetWithoutFlags()}) do not null the vacated point slots: every reader is bounded by the size,
 * so the contract is the exact content of the first {@link Point3DFlagList#size()} entries, all-false flags at or after the size,
 * and a bounded number of retained stale references (never more than {@link Point3DFlagList#getCapacity()}, and only points which
 * were put into this list).
 */
public class Point3DFlagListTest {

	@Test
	public void removeFlaggedCompactsSurvivorsInOrder() {
		List<SimplePoint3D> points = new ArrayList<>();
		Point3DFlagList list = list(5, points);
		list.flag(1);
		list.flag(3);

		assertEquals(2, list.removeFlagged());

		assertEquals(3, list.size());
		assertSame(points.get(0), list.get(0));
		assertSame(points.get(2), list.get(1));
		assertSame(points.get(4), list.get(2));
		assertNoFlags(list, 5);
	}

	@Test
	public void removeFlaggedOfTheTailLeavesThePrefixUntouched() {
		List<SimplePoint3D> points = new ArrayList<>();
		Point3DFlagList list = list(3, points);
		list.flag(1);
		list.flag(2);

		assertEquals(2, list.removeFlagged());

		assertEquals(1, list.size());
		assertSame(points.get(0), list.get(0));
		assertNoFlags(list, 3);
	}

	@Test
	public void removeFlaggedWithoutFlagsChangesNothing() {
		List<SimplePoint3D> points = new ArrayList<>();
		Point3DFlagList list = list(3, points);

		assertEquals(0, list.removeFlagged());

		assertEquals(3, list.size());
		for (int i = 0; i < 3; i++) {
			assertSame(points.get(i), list.get(i));
		}
	}

	@Test
	public void removeFlaggedOverwritesStaleSlotsWhenTheListIsFilledAgain() {
		List<SimplePoint3D> points = new ArrayList<>();
		Point3DFlagList list = list(4, points);
		list.flag(0);
		list.flag(2);
		list.removeFlagged();

		SimplePoint3D added = new DefaultPoint3D(7, 7, 7, 10, 10, 10);
		list.add(added);

		assertEquals(3, list.size());
		assertSame(points.get(1), list.get(0));
		assertSame(points.get(3), list.get(1));
		assertSame(added, list.get(2));
		// the added entry is a normal entry: not flagged, so a following pass keeps it
		assertEquals(0, list.removeFlagged());
		assertEquals(3, list.size());
	}

	@Test
	public void removeFlaggedRetainsOnlyOwnPointsWithinTheCapacity() {
		List<SimplePoint3D> points = new ArrayList<>();
		Point3DFlagList list = list(5, points);
		int capacity = list.getCapacity();
		list.flag(0);
		list.flag(1);
		list.flag(2);

		list.removeFlagged();

		assertEquals(2, list.size());
		assertEquals(capacity, list.getCapacity());
		assertRetainsOnly(list, points);
	}

	@Test
	public void copyIntoShrinksTheDestinationToTheSource() {
		List<SimplePoint3D> sourcePoints = new ArrayList<>();
		Point3DFlagList source = list(1, sourcePoints);
		List<SimplePoint3D> destinationPoints = new ArrayList<>();
		Point3DFlagList destination = list(3, destinationPoints);

		source.copyInto(destination);

		assertEquals(1, destination.size());
		assertSame(sourcePoints.get(0), destination.get(0));
		assertEquals(source, destination);
	}

	@Test
	public void copyIntoClearsTheFlagsOfTheDestinationTail() {
		Point3DFlagList source = list(1, new ArrayList<>());
		Point3DFlagList destination = list(3, new ArrayList<>());
		destination.flag(1);
		destination.flag(2);

		source.copyInto(destination);

		assertNoFlags(destination, 3);
		// an appended entry is not flagged, as add does not write the flag
		destination.add(new DefaultPoint3D(5, 5, 5, 10, 10, 10));
		assertEquals(0, destination.removeFlagged());
		assertEquals(2, destination.size());
	}

	@Test
	public void copyIntoRetainsOnlyOwnPointsWithinTheCapacity() {
		List<SimplePoint3D> sourcePoints = new ArrayList<>();
		Point3DFlagList source = list(1, sourcePoints);
		List<SimplePoint3D> destinationPoints = new ArrayList<>();
		Point3DFlagList destination = list(3, destinationPoints);
		int capacity = destination.getCapacity();

		source.copyInto(destination);

		assertEquals(capacity, destination.getCapacity());
		List<SimplePoint3D> own = new ArrayList<>(destinationPoints);
		own.addAll(sourcePoints);
		assertRetainsOnly(destination, own);
	}

	@Test
	public void copyIntoGrowsTheDestinationToTheSource() {
		List<SimplePoint3D> sourcePoints = new ArrayList<>();
		Point3DFlagList source = list(4, sourcePoints);
		source.flag(2);
		Point3DFlagList destination = list(1, new ArrayList<>());

		source.copyInto(destination);

		assertEquals(4, destination.size());
		for (int i = 0; i < 4; i++) {
			assertSame(sourcePoints.get(i), destination.get(i));
			assertEquals(i == 2, destination.isFlag(i));
		}
	}

	@Test
	public void copyIntoOfTheSameSizeReplacesTheContent() {
		List<SimplePoint3D> sourcePoints = new ArrayList<>();
		Point3DFlagList source = list(3, sourcePoints);
		Point3DFlagList destination = list(3, new ArrayList<>());

		source.copyInto(destination);

		assertEquals(source, destination);
		assertEquals(3, destination.size());
	}

	@Test
	public void resetWithoutFlagsEmptiesTheListAndAllowsRefilling() {
		List<SimplePoint3D> points = new ArrayList<>();
		Point3DFlagList list = list(3, points);

		list.resetWithoutFlags();

		assertTrue(list.isEmpty());
		assertEquals(0, list.size());
		assertFalse(list.iterator().hasNext());

		SimplePoint3D added = new DefaultPoint3D(9, 9, 9, 10, 10, 10);
		list.add(added);
		assertEquals(1, list.size());
		assertSame(added, list.get(0));
		assertRetainsOnly(list, points, added);
	}

	@Test
	public void sizeBoundedReadersIgnoreStaleSlots() {
		List<SimplePoint3D> points = new ArrayList<>();
		Point3DFlagList list = list(4, points);
		list.flag(0);
		list.flag(1);
		list.removeFlagged();

		// the slots at index 2 and 3 are stale: iteration, toList, equals and hashCode only see the two survivors
		List<Point> iterated = new ArrayList<>();
		for (Point point : list) {
			iterated.add(point);
		}
		assertEquals(List.of(points.get(2), points.get(3)), iterated);
		assertEquals(iterated, list.toList());

		Point3DFlagList expected = new Point3DFlagList(2);
		expected.add(points.get(2));
		expected.add(points.get(3));
		assertEquals(expected, list);
		assertEquals(expected.hashCode(), list.hashCode());
	}

	private static Point3DFlagList list(int size, List<SimplePoint3D> points) {
		Point3DFlagList list = new Point3DFlagList(size);
		for (int i = 0; i < size; i++) {
			SimplePoint3D point = new DefaultPoint3D(i, i, i, 10, 10, 10);
			points.add(point);
			list.add(point);
		}
		return list;
	}

	/** No flag is set in the first <code>limit</code> slots, i.e. at, and beyond, the size */
	private static void assertNoFlags(Point3DFlagList list, int limit) {
		for (int i = 0; i < limit; i++) {
			assertFalse(list.isFlag(i), "flag " + i);
		}
	}

	/** Slots at or after the size are either empty or hold a point which was put into this list; the array is never larger than getCapacity() */
	private static void assertRetainsOnly(Point3DFlagList list, List<SimplePoint3D> own, SimplePoint3D... more) {
		Point[] slots = list.getPoints();
		assertEquals(list.getCapacity(), slots.length);
		for (int i = list.size(); i < slots.length; i++) {
			if(slots[i] == null) {
				continue;
			}
			boolean known = false;
			for (SimplePoint3D point : own) {
				if(point == slots[i]) {
					known = true;
				}
			}
			for (SimplePoint3D point : more) {
				if(point == slots[i]) {
					known = true;
				}
			}
			assertTrue(known, "stale slot " + i + " holds a point which was never put into the list");
		}
	}
}
