package com.github.skjolber.packing;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Space;

class SpaceTest {

	@Test
	void testEquals() {
		assertEquals(new Space(1, 2, 3, 4, 5, 6), new Space(1, 2, 3, 4, 5, 6));
	}
}
