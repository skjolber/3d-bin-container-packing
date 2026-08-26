package com.github.skjolber.packing.points2d;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;
import com.github.skjolber.packing.ep.points2d.Point2DFlagList;

public class Point2DFlagListTest {

	@Test
	public void removeFlaggedClearsVacatedReferences() {
		Point2DFlagList list = list(3);
		list.flag(1);

		list.removeFlagged();

		assertNull(list.getPoints()[2]);
	}

	@Test
	public void copyIntoClearsDestinationTail() {
		Point2DFlagList source = list(1);
		Point2DFlagList destination = list(3);

		source.copyInto(destination);

		assertNull(destination.getPoints()[1]);
		assertNull(destination.getPoints()[2]);
	}

	@Test
	public void resetClearsReferencesAfterClear() {
		Point2DFlagList list = list(3);
		list.clear();

		list.reset();

		assertNull(list.getPoints()[0]);
		assertNull(list.getPoints()[1]);
		assertNull(list.getPoints()[2]);
	}

	private Point2DFlagList list(int size) {
		Point2DFlagList list = new Point2DFlagList(size);
		for (int i = 0; i < size; i++) {
			list.add(new DefaultPoint2D(i, i, 0, 10, 10, 0));
		}
		return list;
	}
}
