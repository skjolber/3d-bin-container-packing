package com.github.skjolber.packing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Box;

class BoxTest {

	@Test
	void testCanHold() {
		
		Box box = new Box(350, 150, 400, 0);
		Assert.assertTrue(box.canHold3D(350, 50, 400));
		Assert.assertTrue(box.canHold3D(50, 400, 350));
		Assert.assertTrue(box.canHold3D(400, 350, 50));
		
		Assert.assertTrue(box.canHold3D(50, 350, 400));
		Assert.assertTrue(box.canHold3D(400, 50, 350));
		Assert.assertTrue(box.canHold3D(350, 400, 50));
	}
	
	@Test
	void testFitInFootprintRotate() {
		
		Box box = new Box(1, 6, 3, 0);
		
		Assert.assertTrue(box.fitRotate2D(1, 6));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(6));
		Assert.assertTrue(box.fitRotate2D(6, 1));
		assertThat(box.getWidth(), is(6));
		assertThat(box.getDepth(), is(1));
		
		Assert.assertFalse(box.fitRotate2D(1, 3));
		Assert.assertFalse(box.fitRotate2D(3, 1));

	}
	
	@Test
	void testSmallestFootprintMinimum() {
		
		Box box = new Box(1, 1, 10, 0);
		
		Assert.assertTrue(box.fitRotate3DSmallestFootprint(1, 1, 10));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(1));
		assertThat(box.getHeight(), is(10));

		Assert.assertTrue(box.fitRotate3DSmallestFootprint(10, 1, 1));
		assertThat(box.getWidth(), is(10));
		assertThat(box.getDepth(), is(1));
		assertThat(box.getHeight(), is(1));

		Assert.assertTrue(box.fitRotate3DSmallestFootprint(1, 10, 1));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(10));
		assertThat(box.getHeight(), is(1));

	}

	@Test
	void testSmallestFootprintMinimum2() {
		
		Box box = new Box(1, 1, 10, 0);
		
		Assert.assertTrue(box.fitRotate3DSmallestFootprint(1, 5, 10)); // standing
		assertThat(box.currentSurfaceArea(), is(1));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(1));

		Assert.assertTrue(box.fitRotate3DSmallestFootprint(10, 1, 5)); // lie down
		assertThat(box.currentSurfaceArea(), is(10));
		assertThat(box.getWidth(), is(10));
		assertThat(box.getDepth(), is(1));

		Assert.assertTrue(box.fitRotate3DSmallestFootprint(5, 10, 1)); // lie down
		assertThat(box.currentSurfaceArea(), is(10));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(10));

		Assert.assertTrue(box.fitRotate3DSmallestFootprint(5, 10, 10)); // standing
		assertThat(box.currentSurfaceArea(), is(1));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(1));

	}

}
