package com.github.skjolberg.packing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class LargestAreaFitFirstPackager2DTest extends AbstractPackagerTest {

	@Test
	public void testStackingSquaresOnSquare() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("A", 5, 5, 1));
		products.add(new Box("B", 5, 5, 1));
		products.add(new Box("C", 5, 5, 1));
		products.add(new Box("D", 5, 5, 1));
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		validate(fits);
		print(fits);
	}
	

	@Test
	public void testStackingSquaresOnSquareNo3DRotate() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
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
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("E", 5, 10, 1));
		products.add(new Box("F", 5, 10, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);
		validate(fits);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRotate2D() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("E", 10, 5, 1));
		products.add(new Box("F", 5, 10, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);
		validate(fits);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangle() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 5, 10, 1));
		products.add(new Box("K", 5, 5, 1));
		products.add(new Box("L", 5, 5, 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		print(fits);
		validate(fits);
	}
	
	@Test
	public void testStackingRectanglesOnSquareRectangleVolumeFirst() {
		
		List<Dimension> containers = new ArrayList<Dimension>();
		containers.add(new Dimension(10, 10, 3));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 6, 10, 2));
		products.add(new Box("L", 4, 10, 1));
		products.add(new Box("K", 4, 10, 2));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 2);

		assertEquals(1, fits.getLevels().get(fits.getLevels().size() - 1).getHeight());
		
		print(fits);
		validate(fits);
	}
	
	@Test
	public void testStackingBinary1() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(2, 2, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers);
		
		List<Box> products = new ArrayList<Box>();

		for(int i = 0; i < 4; i++) {
			products.add(new Box("K", 1, 1, 1));
		}
		
		Container fits = packager.pack(products);
		assertNotNull(fits);
		assertEquals(fits.getLevels().size(), 1);
		
		assertThat(fits.get(0, 0).getSpace().getX(), is(0));
		assertThat(fits.get(0, 0).getSpace().getY(), is(0));

		assertThat(fits.get(0, 1).getSpace().getX(), is(0));
		assertThat(fits.get(0, 1).getSpace().getY(), is(1));

		assertThat(fits.get(0, 3).getSpace().getX(), is(1));
		assertThat(fits.get(0, 3).getSpace().getY(), is(1));

		assertThat(fits.get(0, 2).getSpace().getX(), is(1));
		assertThat(fits.get(0, 2).getSpace().getY(), is(0));
		
		print(fits);
		validate(fits);
	}

	@Test
	public void testStackingBinary2() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(8, 8, 1));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
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
		validate(fits);

	}


	@Test
	public void testStackingTooHigh() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 5));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 10, 10, 6));

		Container fits = packager.pack(products);
		assertNull(fits);
	}

	@Test
	public void testStackingTooHighLevel() {
		
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(10, 10, 5));
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);
		
		List<Box> products = new ArrayList<Box>();

		products.add(new Box("J", 10, 10, 5));

		products.add(new Box("J", 5, 10, 1));
		products.add(new Box("K", 5, 5, 1));
		products.add(new Box("L", 5, 5, 1));

		Container fits = packager.pack(products);
		assertNull(fits);
	}	
	
	@Test
	public void testIssue4() {
		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(Arrays.asList(new Dimension("Plane", 1335, 285, 247)), false, true);
		List<Box> products = new ArrayList<Box>();
		products.add(new Box("72407",20,30,15));
		products.add(new Box("74809",100,120,52));
		products.add(new Box("71535",30,40,15));
		products.add(new Box("74780",20,30,15));
		products.add(new Box("74760_1",30,20,15));
		products.add(new Box("74760_2",30,20,15));
		products.add(new Box("74757",30,40,15));
		products.add(new Box("74808",100,120,75));
		products.add(new Box("73770",20,30,15));
		products.add(new Box("74844_1",60,40,28));
		products.add(new Box("74844_2",60,40,28));
		products.add(new Box("74844_3",60,40,28));
		products.add(new Box("74846",30,40,15));
		products.add(new Box("73767_1",60,50,15));
		products.add(new Box("73767_2",60,50,15));
		products.add(new Box("74848",20,30,15));
		products.add(new Box("74850",20,30,15));
		products.add(new Box("74852",20,30,15));
		products.add(new Box("74787",100,120,154));
		products.add(new Box("74806_1",30,20,15));
		products.add(new Box("74806_2",30,20,15));
		products.add(new Box("74806_3",30,20,15));
		products.add(new Box("74806_4",30,20,15));
		products.add(new Box("74806_5",30,20,15));
		products.add(new Box("74806_6",30,20,15));
		products.add(new Box("74806_7",30,20,15));
		products.add(new Box("74806_8",30,20,15));
		products.add(new Box("74806_9",30,20,15));
		products.add(new Box("74781_1",30,20,15));
		products.add(new Box("74781_2",30,20,15));
		products.add(new Box("74781_3",30,20,15));
		products.add(new Box("74775_1",30,20,15));
		products.add(new Box("74775_2",30,20,15));
		products.add(new Box("74775_3",30,20,15));
		products.add(new Box("74775_4",30,20,15));
		products.add(new Box("74756",100,120,75));
		products.add(new Box("74797",30,40,15));
		products.add(new Box("74835",100,120,133));
		products.add(new Box("74834",100,120,36));
		products.add(new Box("74833",100,120,30));
		products.add(new Box("74784_1",30,20,15));
		products.add(new Box("74784_2",30,20,15));
		products.add(new Box("74782",100,120,50));
		products.add(new Box("74765",30,40,15));
		products.add(new Box("74769",12,30,15));
		products.add(new Box("74802",80,120,35));
		products.add(new Box("73769",20,30,15));
		products.add(new Box("74799_1",40,30,15));
		products.add(new Box("74799_2",40,30,15));
		products.add(new Box("74799_3",40,30,15));
		products.add(new Box("74799_4",40,30,15));
		products.add(new Box("74799_5",40,30,15));
		products.add(new Box("74799_6",40,30,15));
		products.add(new Box("74827",100,120,125));
		products.add(new Box("74800_1",60,40,28));
		products.add(new Box("74800_2",60,40,28));
		products.add(new Box("74800_3",60,40,28));
		products.add(new Box("74800_4",60,40,28));
		products.add(new Box("74800_5",60,40,28));
		products.add(new Box("74800_6",60,40,28));
		products.add(new Box("74800_7",60,40,28));
		products.add(new Box("74823",100,120,129));
		products.add(new Box("74853",20,30,15));
		products.add(new Box("74839_1",120,100,130));
		products.add(new Box("74839_2",120,100,130));
		products.add(new Box("74839_3",120,100,130));
		products.add(new Box("74849_1",120,100,127));
		products.add(new Box("74849_2",120,100,127));
		products.add(new Box("74777_1",120,100,35));
		products.add(new Box("74777_2",120,100,35));
		products.add(new Box("72583",60,114,64));
		products.add(new Box("74737",100,120,116));
		products.add(new Box("74774_1",121,101,54));
		products.add(new Box("74774_2",121,101,54));
		products.add(new Box("74774_3",121,101,54));
		products.add(new Box("74772",100,120,130));
		products.add(new Box("72434",20,30,15));
		products.add(new Box("74831",100,120,129));
		products.add(new Box("74832",30,40,15));
		products.add(new Box("74778_1",120,100,101));
		products.add(new Box("74778_2",120,100,101));
		products.add(new Box("74778_3",120,100,101));
		products.add(new Box("74778_4",120,100,101));
		products.add(new Box("74845_1",115,60,65));
		products.add(new Box("74845_2",115,60,65));
		products.add(new Box("74776_1",120,100,128));
		products.add(new Box("74776_2",120,100,128));
		products.add(new Box("72385",20,30,15));
		products.add(new Box("74779_1",120,100,36));
		products.add(new Box("74779_2",120,100,36));
		products.add(new Box("74779_3",120,100,36));
		products.add(new Box("74779_4",120,100,36));
		products.add(new Box("72422",50,60,15));
		products.add(new Box("BRE72563_1",40,30,15));
		products.add(new Box("BRE72563_2",40,30,15));
		products.add(new Box("BRE72563_3",40,30,15));
		products.add(new Box("BRE72563_4",40,30,15));
		products.add(new Box("72578",100,120,75));
		products.add(new Box("71555",100,120,52));
		products.add(new Box("74830_1",40,30,15));
		products.add(new Box("74830_2",40,30,15));
		products.add(new Box("74816",100,120,48));

		Container pack = packager.pack(products);
		assertNotNull(pack);

		validate(pack);
		print(pack);

	}

	@Test
	public void testIssue5() {
        java.util.List<Box> containers = new ArrayList<Box>();
        containers.add(new Box("Plane", 1355, 285, 247));
        LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, false);
        List<Box> products = new ArrayList<Box>();

        products.add(new Box("72407",97,193,48));
        products.add(new Box("74809",97,193,48));
        products.add(new Box("71535",97,193,48));
        products.add(new Box("74780",97,110,46));
        products.add(new Box("74760",97,110,46));
        products.add(new Box("74757",97,110,46));
        products.add(new Box("74808",59,56,174));
        products.add(new Box("73770",59,56,174));
        products.add(new Box("74844",59,56,174));
        products.add(new Box("74846",59,56,174));
        products.add(new Box("73767",59,56,174));
        products.add(new Box("74848",59,56,174));
        products.add(new Box("74850",59,56,174));
        products.add(new Box("74852",59,56,174));
        products.add(new Box("74787",59,56,174));
        products.add(new Box("74806",59,56,174));
        products.add(new Box("74781",61,90,220));
        products.add(new Box("74775",74,93,216));
        products.add(new Box("74756",61,90,220));
        products.add(new Box("74797",61,90,220));
        products.add(new Box("74835",61,90,220));
        products.add(new Box("74834",74,93,216));
        
		Container pack = packager.pack(products);
		assertNotNull(pack);

		validate(pack);
		
		print(pack);
		validate(pack);
	}
	
	@Test
	public void noFitFullHeightRotation2D () {
	   List<Box> containers = new ArrayList<Box>();
	   containers.add(new Box(100, 10, 20));
	   LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, false, true);

	   List<Box> products = new ArrayList<Box>();

	   products.add(new Box("J", 10, 10, 10));

	   products.add(new Box("J", 10, 10, 20));

	   Container fits = packager.pack(products);
	   assertNotNull(fits);
	   
	   validate(fits);
	}
	
	@Test
	@Ignore
	public void testRunsForLimitedTimeSeconds() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(500, 10, 10));
		runsLimitedTimeSeconds(new LargestAreaFitFirstPackager(containers, true, true), 5000);
	}
	

}
