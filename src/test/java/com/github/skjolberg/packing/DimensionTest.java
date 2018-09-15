package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

class DimensionTest {

	@Test
	void testEncodeDecode() {
		
		Dimension d = new Dimension(1, 2, 3);
		
		String encode = d.encode();
		
		Dimension decoded = Dimension.decode(encode);
		
		assertEquals(d.getWidth(), decoded.getWidth());
		assertEquals(d.getDepth(), decoded.getDepth());
		assertEquals(d.getHeight(), decoded.getHeight());
	}

	@Test
	void testEquals() {
		assertEquals(new Dimension(1, 1, 1), new Dimension(1,  1,  1));
	}
}
