package com.github.skjolberg.packing.impl;

import static org.junit.Assert.assertEquals;

import com.github.skjolberg.packing.impl.Space;
import org.junit.jupiter.api.Test;

public class SpaceTest {

	@Test
	public void testEquals() {
		assertEquals(new Space(1, 2, 3, 4, 5, 6), new Space(1, 2, 3, 4, 5, 6));
	}
}
