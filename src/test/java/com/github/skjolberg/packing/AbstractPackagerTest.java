package com.github.skjolberg.packing;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public abstract class AbstractPackagerTest {


	public static void validate(Container pack) {
		for(Level level : pack.getLevels()) {
			level.validate();
		}
	}
	
	public static void print(Container fits) {
		System.out.println();
		System.out.println(Visualizer.visualize(fits, 100, 2));
		System.out.println();
	}
	
	public void runsLimitedTimeSeconds(Packager bruteForcePackager, long duration) {
		List<Box> containers = new ArrayList<Box>();
		containers.add(new Box(5000, 1000, 1000));

		List<Box> products1 = new ArrayList<Box>();
		
		for(int i = 0; i < 100000; i++) {
			Box box = new Box(Integer.toString(i), 5, 10, 10);
			for(int k = 0; k < i % 2; k++) {
				box.rotate3D();
			}
			products1.add(box);
		}
		
		long time = System.currentTimeMillis();
		Container fits1 = bruteForcePackager.pack(products1, time + duration);
		assertNull(fits1);
		
		assertTrue("Used " + (System.currentTimeMillis() - time), System.currentTimeMillis() - time >= duration);
		assertTrue("Used " + (System.currentTimeMillis() - time), System.currentTimeMillis() - time <= duration + 100);
	}
	
	

}
