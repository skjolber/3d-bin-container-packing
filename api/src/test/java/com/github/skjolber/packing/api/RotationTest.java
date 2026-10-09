package com.github.skjolber.packing.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RotationTest {

	@Test
	void testRightAtZeroDegrees() {
		Rotation rotation = Rotation.newBuilder().withRightAtZeroDegrees().build();

		assertNotNull(rotation.getRight0());
		assertNull(rotation.getRight90());

		assertTrue(rotation.isYZ0());
		assertFalse(rotation.isYZ90());
	}

	@Test
	void testRightAtNinetyDegrees() {
		Rotation rotation = Rotation.newBuilder().withRightAtNinetyDegrees().build();

		assertNull(rotation.getRight0());
		assertNotNull(rotation.getRight90());

		assertFalse(rotation.isYZ0());
		assertTrue(rotation.isYZ90());
	}

	@Test
	void testRightAtZeroAndNinetyDegreesDiffer() {
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotation(Rotation.newBuilder().withRightAtZeroDegrees().build()).withWeight(1).build();
		Box rotated = Box.newBuilder().withSize(1, 2, 3).withRotation(Rotation.newBuilder().withRightAtNinetyDegrees().build()).withWeight(1).build();

		assertEquals(1, box.getStackValues().length);
		assertEquals(3, box.getStackValue(0).getDx());
		assertEquals(2, box.getStackValue(0).getDy());
		assertEquals(1, box.getStackValue(0).getDz());

		assertEquals(1, rotated.getStackValues().length);
		assertEquals(2, rotated.getStackValue(0).getDx());
		assertEquals(3, rotated.getStackValue(0).getDy());
		assertEquals(1, rotated.getStackValue(0).getDz());
	}

	@Test
	void testRightWithoutDegreesIsBothZeroAndNinety() {
		Rotation rotation = Rotation.newBuilder().withRight().build();

		assertNotNull(rotation.getRight0());
		assertNotNull(rotation.getRight90());
	}

	@Test
	void testNinetyDegreeMethodsOnlyAddRotatedSurfaces() {
		Rotation top = Rotation.newBuilder().withTopAtNinetyDegrees().build();
		assertNull(top.getTop0());
		assertNotNull(top.getTop90());

		Rotation bottom = Rotation.newBuilder().withBottomAtNinetyDegrees().build();
		assertNull(bottom.getBottom0());
		assertNotNull(bottom.getBottom90());

		Rotation left = Rotation.newBuilder().withLeftAtNinetyDegrees().build();
		assertNull(left.getLeft0());
		assertNotNull(left.getLeft90());

		Rotation right = Rotation.newBuilder().withRightAtNinetyDegrees().build();
		assertNull(right.getRight0());
		assertNotNull(right.getRight90());

		Rotation front = Rotation.newBuilder().withFrontAtNinetyDegrees().build();
		assertNull(front.getFront0());
		assertNotNull(front.getFront90());

		Rotation rear = Rotation.newBuilder().withRearAtNinetyDegrees().build();
		assertNull(rear.getRear0());
		assertNotNull(rear.getRear90());
	}

	@Test
	void testZeroDegreeMethodsOnlyAddUnrotatedSurfaces() {
		Rotation top = Rotation.newBuilder().withTopAtZeroDegrees().build();
		assertNotNull(top.getTop0());
		assertNull(top.getTop90());

		Rotation bottom = Rotation.newBuilder().withBottomAtZeroDegrees().build();
		assertNotNull(bottom.getBottom0());
		assertNull(bottom.getBottom90());

		Rotation left = Rotation.newBuilder().withLeftAtZeroDegrees().build();
		assertNotNull(left.getLeft0());
		assertNull(left.getLeft90());

		Rotation right = Rotation.newBuilder().withRightAtZeroDegrees().build();
		assertNotNull(right.getRight0());
		assertNull(right.getRight90());

		Rotation front = Rotation.newBuilder().withFrontAtZeroDegrees().build();
		assertNotNull(front.getFront0());
		assertNull(front.getFront90());

		Rotation rear = Rotation.newBuilder().withRearAtZeroDegrees().build();
		assertNotNull(rear.getRear0());
		assertNull(rear.getRear90());
	}
}
