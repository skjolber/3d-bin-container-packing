package com.github.skjolber.packing.points3d;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;

public class Point3DFlagListTest {

	@Test
	public void removeFlaggedClearsVacatedReferences() {
		Point3DFlagList list = list(3);
		list.flag(1);

		list.removeFlagged();

		assertNull(list.getPoints()[2]);
	}

	@Test
	public void copyIntoClearsDestinationTail() {
		Point3DFlagList source = list(1);
		Point3DFlagList destination = list(3);

		source.copyInto(destination);

		assertNull(destination.getPoints()[1]);
		assertNull(destination.getPoints()[2]);
	}

	private Point3DFlagList list(int size) {
		Point3DFlagList list = new Point3DFlagList(size);
		for (int i = 0; i < size; i++) {
			list.add(new DefaultPoint3D(i, i, i, 10, 10, 10));
		}
		return list;
	}
}
