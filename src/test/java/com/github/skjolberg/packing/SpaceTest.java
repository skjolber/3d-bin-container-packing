package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SpaceTest {

	@Test
	public void testEquals() {
		assertEquals(new Space(1, 2, 3, 4, 5, 6), new Space(1, 2, 3, 4, 5, 6));
	}
}
