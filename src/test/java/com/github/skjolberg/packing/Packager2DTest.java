package com.github.skjolberg.packing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.skjolberg.packing.Box;
import com.github.skjolberg.packing.Container;
import com.github.skjolberg.packing.Dimension;
import com.github.skjolberg.packing.Packager;

public class Packager2DTest {

	@Test
	public void testStackingSquaresOnSquare() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("A", 5, 5, 1));
		products.add(new Box("B", 5, 5, 1));
		products.add(new Box("C", 5, 5, 1));
		products.add(new Box("D", 5, 5, 1));
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);
	}
	

	@Test
	public void testStackingSquaresOnSquareNo3DRotate() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("A", 1, 5, 5)); // does not fit in height
		products.add(new Box("B", 5, 5, 1));
		products.add(new Box("C", 5, 5, 1));
		products.add(new Box("D", 5, 5, 1));
		
		Container fits = packager.pack(products);
		assertNull(fits);
	}

	
	@Test
	public void testStackingRectanglesOnSquare() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("E", 5, 10, 1));
		products.add(new Box("F", 5, 10, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRotate2D() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("E", 10, 5, 1));
		products.add(new Box("F", 5, 10, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangle() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 5, 10, 1));
		products.add(new Box("K", 5, 5, 1));
		products.add(new Box("L", 5, 5, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {
		
		List<Dimension> containers = new ArrayList<Dimension>();
		containers.add(new Dimension(10, 10, 3));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 6, 10, 2));
		products.add(new Box("L", 4, 10, 1));
		products.add(new Box("K", 4, 10, 2));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 2);

		assertEquals(1, fits.getLevels().get(fits.getLevels().size() - 1).getHeight());
		
		print(fits);

	}
	
	@Test
	public void testStackingBinary1() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(2, 2, 1));
		Packager packager = new Packager(containers);
		
		List<Box> products = new ArrayList<Box>();

		for(int i = 0; i < 4; i++) {
			products.add(new Box("K", 1, 1, 1));
		}
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		assertThat(fits.get(0, 0).getSpace().getX(), is(0));
		assertThat(fits.get(0, 0).getSpace().getY(), is(0));

		assertThat(fits.get(0, 1).getSpace().getX(), is(1));
		assertThat(fits.get(0, 1).getSpace().getY(), is(0));

		assertThat(fits.get(0, 2).getSpace().getX(), is(0));
		assertThat(fits.get(0, 2).getSpace().getY(), is(1));

		assertThat(fits.get(0, 3).getSpace().getX(), is(1));
		assertThat(fits.get(0, 3).getSpace().getY(), is(1));
		
		print(fits);
	}

	@Test
	public void testStackingBinary2() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(8, 8, 1));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 4, 4, 1)); // 16
		
		for(int i = 0; i < 8; i++) { // 32
			products.add(new Box("K", 2, 2, 1));
		}
		for(int i = 0; i < 16; i++) { // 16
			products.add(new Box("K", 1, 1, 1));
		}
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);

	}


	private void print(Container fits) {
		System.out.println();
		System.out.println(Visualizer.visualize(fits, fits.getWidth() * 2, 2));
		System.out.println();
	}


	@Test
	public void testStackingTooHigh() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 5));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 10, 10, 6));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingTooHighLevel() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 5));
		Packager packager = new Packager(containers, false);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 10, 10, 5));

		products.add(new Box("J", 5, 10, 1));
		products.add(new Box("K", 5, 5, 1));
		products.add(new Box("L", 5, 5, 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}	
	
}
