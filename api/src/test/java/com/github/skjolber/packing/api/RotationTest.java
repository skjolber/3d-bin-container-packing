package com.github.skjolber.packing.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RotationTest {

	@Test
	void rightAtNinetyDegreesIsRotated() {
		Rotation rotation = Rotation.newBuilder().withRightAtNinetyDegrees().build();

		assertTrue(rotation.isYZ90());
		assertFalse(rotation.isYZ0());
		assertSame(Surface.RIGHT, rotation.getRight90());
		assertNull(rotation.getRight0());
	}

	@Test
	void rightAtZeroDegreesIsNotRotated() {
		Rotation rotation = Rotation.newBuilder().withRightAtZeroDegrees().build();

		assertTrue(rotation.isYZ0());
		assertFalse(rotation.isYZ90());
		assertSame(Surface.RIGHT, rotation.getRight0());
		assertNull(rotation.getRight90());
	}

	@Test
	void rightAtZeroAndNinetyDegreesEqualsRight() {
		Rotation both = Rotation.newBuilder().withRightAtZeroDegrees().withRightAtNinetyDegrees().build();
		Rotation right = Rotation.newBuilder().withRight().build();

		assertSame(Surface.RIGHT, both.getRight0());
		assertSame(Surface.RIGHT, both.getRight90());
		assertSame(Surface.RIGHT, right.getRight0());
		assertSame(Surface.RIGHT, right.getRight90());
	}

	@Test
	void ninetyDegreeVariantsAreRotatedAndZeroDegreeVariantsAreNot() {
		Rotation.Builder zero = Rotation.newBuilder()
				.withTopAtZeroDegrees()
				.withBottomAtZeroDegrees()
				.withLeftAtZeroDegrees()
				.withRightAtZeroDegrees()
				.withFrontAtZeroDegrees()
				.withRearAtZeroDegrees();
		Rotation.Builder ninety = Rotation.newBuilder()
				.withTopAtNinetyDegrees()
				.withBottomAtNinetyDegrees()
				.withLeftAtNinetyDegrees()
				.withRightAtNinetyDegrees()
				.withFrontAtNinetyDegrees()
				.withRearAtNinetyDegrees();

		Rotation zeroRotation = zero.build();
		assertTrue(zeroRotation.is0());
		assertFalse(zeroRotation.is90());

		Rotation ninetyRotation = ninety.build();
		assertTrue(ninetyRotation.is90());
		assertFalse(ninetyRotation.is0());
		assertSame(Surface.TOP, ninetyRotation.getTop90());
		assertSame(Surface.BOTTOM, ninetyRotation.getBottom90());
		assertSame(Surface.LEFT, ninetyRotation.getLeft90());
		assertSame(Surface.RIGHT, ninetyRotation.getRight90());
		assertSame(Surface.FRONT, ninetyRotation.getFront90());
		assertSame(Surface.REAR, ninetyRotation.getRear90());
	}

	/**
	 * The yz plane is the bottom, so dx is the height. At zero degrees dy lies along y, at ninety degrees dz does.
	 */
	@Test
	void rightAtNinetyDegreesGivesTheRotatedStackValue() {
		Box zero = Box.newBuilder().withSize(1, 2, 3).withRotation(Rotation.newBuilder().withRightAtZeroDegrees().build()).withWeight(1).build();
		Box ninety = Box.newBuilder().withSize(1, 2, 3).withRotation(Rotation.newBuilder().withRightAtNinetyDegrees().build()).withWeight(1).build();

		assertEquals(1, zero.getStackValues().length);
		assertEquals(1, ninety.getStackValues().length);

		BoxStackValue zeroValue = zero.getStackValue(0);
		assertEquals(3, zeroValue.getDx());
		assertEquals(2, zeroValue.getDy());
		assertEquals(1, zeroValue.getDz());

		BoxStackValue ninetyValue = ninety.getStackValue(0);
		assertEquals(2, ninetyValue.getDx());
		assertEquals(3, ninetyValue.getDy());
		assertEquals(1, ninetyValue.getDz());
	}
}
