package com.github.skjolberg.packing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Assert;
import org.junit.Test;

import com.github.skjolberg.packing.Box;

public class BoxTest {

	@Test
	public void testCanHold() {
		
		Box box = new Box(350, 150, 400);
		Assert.assertTrue(box.canHold(350, 50, 400));
		Assert.assertTrue(box.canHold(50, 400, 350));
		Assert.assertTrue(box.canHold(400, 350, 50));
		
		Assert.assertTrue(box.canHold(50, 350, 400));
		Assert.assertTrue(box.canHold(400, 50, 350));
		Assert.assertTrue(box.canHold(350, 400, 50));
	}
	
	@Test
	public void testfitInFootprintRotate() {
		
		Box box = new Box(1, 6, 3);
		
		Assert.assertTrue(box.fitInFootprintRotate(1, 6));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(6));
		Assert.assertTrue(box.fitInFootprintRotate(6, 1));
		assertThat(box.getWidth(), is(6));
		assertThat(box.getDepth(), is(1));
		
		Assert.assertFalse(box.fitInFootprintRotate(1, 3));
		Assert.assertFalse(box.fitInFootprintRotate(3, 1));

	}
	
	@Test
	public void testSmallestFootprintMinimum() {
		
		Box box = new Box(1, 1, 10);
		
		Assert.assertTrue(box.rotateSmallestFootprint(1, 1, 10));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(1));
		assertThat(box.getHeight(), is(10));

		Assert.assertTrue(box.rotateSmallestFootprint(10, 1, 1));
		assertThat(box.getWidth(), is(10));
		assertThat(box.getDepth(), is(1));
		assertThat(box.getHeight(), is(1));

		Assert.assertTrue(box.rotateSmallestFootprint(1, 10, 1));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(10));
		assertThat(box.getHeight(), is(1));

	}

	@Test
	public void testSmallestFootprintMinimum2() {
		
		Box box = new Box(1, 1, 10);
		
		Assert.assertTrue(box.rotateSmallestFootprint(1, 5, 10)); // standing
		assertThat(box.currentSurfaceArea(), is(1));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(1));

		Assert.assertTrue(box.rotateSmallestFootprint(10, 1, 5)); // lie down
		assertThat(box.currentSurfaceArea(), is(10));
		assertThat(box.getWidth(), is(10));
		assertThat(box.getDepth(), is(1));

		Assert.assertTrue(box.rotateSmallestFootprint(5, 10, 1)); // lie down
		assertThat(box.currentSurfaceArea(), is(10));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(10));

		Assert.assertTrue(box.rotateSmallestFootprint(5, 10, 10)); // standing
		assertThat(box.currentSurfaceArea(), is(1));
		assertThat(box.getWidth(), is(1));
		assertThat(box.getDepth(), is(1));

	}

}
