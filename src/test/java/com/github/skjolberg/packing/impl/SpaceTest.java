package com.github.skjolberg.packing.impl;

import static org.junit.Assert.assertEquals;

import com.github.skjolberg.packing.impl.Space;
import org.junit.jupiter.api.Test;

class SpaceTest {

	@Test
	void testEquals() {
		assertEquals(new Space(1, 2, 3, 4, 5, 6), new Space(1, 2, 3, 4, 5, 6));
	}
}
