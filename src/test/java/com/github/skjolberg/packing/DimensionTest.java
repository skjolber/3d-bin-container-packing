package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DimensionTest {

	@Test
	public void testEncodeDecode() {
		
		Dimension d = new Dimension(1, 2, 3);
		
		String encode = d.encode();
		
		Dimension decoded = Dimension.decode(encode);
		
		assertEquals(d.getWidth(), decoded.getWidth());
		assertEquals(d.getDepth(), decoded.getDepth());
		assertEquals(d.getHeight(), decoded.getHeight());
	}

}
