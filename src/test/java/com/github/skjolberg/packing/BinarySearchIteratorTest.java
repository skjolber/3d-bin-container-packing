package com.github.skjolberg.packing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BinarySearchIteratorTest {

	@Test
	public void testHigher() {
		
		BinarySearchIterator iterator = new BinarySearchIterator(0, 15);
		
		int mid = -1;
		
		while(iterator.hasNext()) {
			
			int next = iterator.next();
			
			assertTrue(next > mid);
			
			mid = next;
			
			iterator.higher();
		}
		
		assertEquals(15, mid);
	}
	

	@Test
	public void testLower() {
		
		BinarySearchIterator iterator = new BinarySearchIterator(0, 15);
		
		int mid = Integer.MAX_VALUE;
		
		while(iterator.hasNext()) {
			
			int next = iterator.next();
			
			assertTrue(next < mid);
			
			mid = next;
			
			iterator.lower();
		}
		
		assertEquals(0, mid);
	}
}
