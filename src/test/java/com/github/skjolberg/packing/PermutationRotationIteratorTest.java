package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PermutationRotationIteratorTest {

	@Test
	public void testCount() {
		for(int i = 1; i <= 10; i++) {
			Box container = new Box(3 * (i + 1), 1, 1);
			List<Box> products1 = new ArrayList<Box>();
			
			for(int k = 0; k < i; k++) {
				products1.add(new Box(Integer.toString(k), 3, 1, 1));
			}
	
			PermutationRotationIterator rotator = new PermutationRotationIterator(products1, container, true);
			
			long count = rotator.countRotations();
			
			int rotate = 0;
			do {
				rotate++;
			} while(rotator.nextRotation());
			
			assertEquals(count, rotate);
		}
	}
	
	@Test
	public void testRotations() {
		Box container = new Box(9, 1, 1);
		
		List<Box> products = new ArrayList<Box>();
		
		products.add(new Box("0", 1, 1, 3));
		products.add(new Box("1", 1, 1, 3));
		products.add(new Box("2", 1, 1, 3));

		PermutationRotationIterator rotator = new PermutationRotationIterator(products, container, true);

		do {
			// check order unchanged
			for(int i = 0; i < products.size(); i++) {
				assertEquals(Integer.toString(i), rotator.get(i).getName());
			}

			// all rotations can fit
			for(int i = 0; i < products.size(); i++) {
				assertTrue(rotator.get(i).fitsInside3D(container));
			}
		} while(rotator.nextRotation());
		
	}
	
	@Test
	public void testPermutations() {
		Box container = new Box(9, 1, 1);
		
		List<Box> products = new ArrayList<Box>();
		
		products.add(new Box("0", 1, 1, 3));
		products.add(new Box("1", 1, 1, 3));
		products.add(new Box("2", 1, 1, 3));
		products.add(new Box("3", 1, 1, 3));
		products.add(new Box("4", 1, 1, 3));

		PermutationRotationIterator rotator = new PermutationRotationIterator(products, container, true);

		int count = 0;
		do {
			count++;
		} while(rotator.nextPermutation());
		
		assertEquals( 5 * 4 * 3 * 2, count);
	}
}
