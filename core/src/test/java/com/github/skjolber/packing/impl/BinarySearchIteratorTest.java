package com.github.skjolber.packing.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.impl.BinarySearchIterator;

class BinarySearchIteratorTest {

	@Test
	void testHigher() {
		
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
	void testLower() {
		
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
