package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class RotatorTest {

	@Test
	public void testCount() {
		Box container = new Box(45, 10, 10);
		List<Box> products1 = new ArrayList<Box>();
		
		products1.add(new Box("01", 15, 10, 10));
		products1.add(new Box("02", 15, 10, 10));
		products1.add(new Box("03", 15, 10, 10));

		Rotator rotator = new Rotator(products1, container, true);
		
		long count = rotator.count();
		
		System.out.println("Count is " + count);
		
		int rotate = 0;
		while(rotator.rotate(Integer.MAX_VALUE) != -1) {
			rotate++;
		}
		System.out.println("Rotations: " + rotate);
	}
}
