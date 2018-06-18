package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContainerTest {

	@Test
	public void testEquals() {
		assertEquals(new Container(1, 1, 1, 0), new Container(new Dimension(1, 1, 1), 1));
	}
}
