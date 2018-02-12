package com.github.skjolberg.packing;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class BruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	public void testBruteForce() {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(1200, 580, 400));
		Packager packager = new BruteForcePackager(containers, true, true);

		List<Box> products1 = new ArrayList<Box>();
		
		products1.add(new Box("01", 1200, 150, 400));
		products1.add(new Box("02", 1200, 400, 150));
		products1.add(new Box("03", 1200, 150, 400));
		
		Container fits1 = packager.pack(products1);
		assertNotNull(fits1);
		print(fits1);
	}
}
