package com.github.skjolber.packing.ep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;
import com.github.skjolber.packing.ep.points2d.Point2DFlagList;
import com.github.skjolber.packing.ep.points2d.Point2DList;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.Point3DArray;
import com.github.skjolber.packing.ep.points3d.Point3DFlagList;
import com.github.skjolber.packing.ep.points3d.Point3DList;

public class CustomCollectionEqualsTest {

	@Test
	void listsWithDifferentSizesAreNotEqual() {
		PlacementList emptyPlacements = new PlacementList();
		PlacementList onePlacement = new PlacementList();
		onePlacement.add(new Placement());
		assertNotEquals(emptyPlacements, onePlacement);
		assertNotEquals(onePlacement, emptyPlacements);

		DefaultPoint2D point2D = new DefaultPoint2D(0, 0, 0, 1, 1, 0);
		Point2DList empty2D = new Point2DList();
		Point2DList one2D = new Point2DList();
		one2D.add(point2D);
		assertNotEquals(empty2D, one2D);
		assertNotEquals(one2D, empty2D);

		Point2DFlagList emptyFlag2D = new Point2DFlagList();
		Point2DFlagList oneFlag2D = new Point2DFlagList();
		oneFlag2D.add(point2D);
		assertNotEquals(emptyFlag2D, oneFlag2D);
		assertNotEquals(oneFlag2D, emptyFlag2D);

		DefaultPoint3D point3D = new DefaultPoint3D(0, 0, 0, 1, 1, 1);
		Point3DList empty3D = new Point3DList();
		Point3DList one3D = new Point3DList();
		one3D.add(point3D);
		assertNotEquals(empty3D, one3D);
		assertNotEquals(one3D, empty3D);

		Point3DFlagList emptyFlag3D = new Point3DFlagList();
		Point3DFlagList oneFlag3D = new Point3DFlagList();
		oneFlag3D.add(point3D);
		assertNotEquals(emptyFlag3D, oneFlag3D);
		assertNotEquals(oneFlag3D, emptyFlag3D);
	}

	@Test
	void point3DArraysCompareToPoint3DArrays() {
		Point3DArray first = new Point3DArray();
		first.set(new DefaultPoint3D(0, 0, 0, 1, 1, 1), 0);

		Point3DArray second = new Point3DArray();
		second.set(new DefaultPoint3D(0, 0, 0, 1, 1, 1), 0);

		assertEquals(first, second);
		assertEquals(second, first);

		assertNotEquals(first, new Point3DArray());
		assertNotEquals(first, new Point3DList());
	}
}
