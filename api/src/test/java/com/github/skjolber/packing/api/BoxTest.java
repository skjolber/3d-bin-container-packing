package com.github.skjolber.packing.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;

public class BoxTest {

	@Test
	public void testBuilder1() {
		
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();
		
		BoxStackValue[] stackValues = box.getStackValues();
		
		assertEquals(stackValues.length, 6);
		
		assertUniqueValues(stackValues);
		
		assertEquals(stackValues[0].getDx(), 1);		
		assertEquals(stackValues[0].getDy(), 2);		
		assertEquals(stackValues[0].getDz(), 3);		

		assertEquals(stackValues[1].getDx(), 2);		
		assertEquals(stackValues[1].getDy(), 1);		
		assertEquals(stackValues[1].getDz(), 3);		
	}
	
	@Test
	public void testBuilder2() {
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate2D().withWeight(1).build();
		
		BoxStackValue[] stackValues = box.getStackValues();
		assertEquals(stackValues.length, 2);
		assertUniqueValues(stackValues);
	}

	private void assertUniqueValues(BoxStackValue[] stackValues) {
		for (int i = 0; i < stackValues.length; i++) {
			BoxStackValue box1 = stackValues[i];
			
			for (int j = 0; j < stackValues.length; j++) {
				BoxStackValue box2 = stackValues[j];

				if(box1 != box2) {
					
					if(box1.dx == box2.dx && box1.dy == box2.dy && box1.dz == box2.dz) {
						fail();
					}
					
				}
				
			}
		}
	}
}
