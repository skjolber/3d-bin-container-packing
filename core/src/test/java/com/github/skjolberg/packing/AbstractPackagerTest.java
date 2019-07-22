package com.github.skjolberg.packing;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class AbstractPackagerTest {


	static void validate(Container pack) {
		for (Level level : pack.getLevels()) {
			level.validate();
		}
	}
	
	static void validate(List<Container> pack) {
		for (Container container : pack) {
			for (Level level : container.getLevels()) {
				level.validate();
			}
		}
	}

	static void print(Container fits) {
		System.out.println();
		System.out.println(Visualizer.visualize(fits, 90, 2));
		System.out.println();
	}

	void runsLimitedTimeSeconds(Packager bruteForcePackager, long duration) {
		List<Box> containers = new ArrayList<>();
		containers.add(new Box(5000, 1000, 1000, 0));

		List<BoxItem> products1 = new ArrayList<>();

		for (int i = 0; i < 10000; i++) {
			Box box = new Box(Integer.toString(i), 5, 10, 10, 0);
			for (int k = 0; k < i % 2; k++) {
				box.rotate3D();
			}
			products1.add(new BoxItem(box, 1));
		}

		long time = System.currentTimeMillis();
		Container fits1 = bruteForcePackager.pack(products1, time + duration);
		assertNull(fits1);

		assertTrue("Used " + (System.currentTimeMillis() - time), System.currentTimeMillis() - time >= duration);
		assertTrue("Used " + (System.currentTimeMillis() - time), System.currentTimeMillis() - time <= duration + 1000);
	}


	List<BoxItem> listOf6Products() {
		return Arrays.asList(
				new BoxItem(new Box("", 39, 80, 50, 0)),
				new BoxItem(new Box("", 39, 79, 51, 0)),
				new BoxItem(new Box("", 37, 78, 49, 0)),
				new BoxItem(new Box("", 41, 81, 52, 0)),
				new BoxItem(new Box("", 38, 79, 50, 0)),
				new BoxItem(new Box("", 39, 80, 52, 0)));
	}

	List<BoxItem> listOf28Products() {
		return Arrays.asList(
				new BoxItem(new Box("", 720, 620, 78, 0)),
				new BoxItem(new Box("", 611, 31, 791, 0)),
				new BoxItem(new Box("", 611, 31, 791, 0)),
				new BoxItem(new Box("", 656, 18, 2033, 0)),
				new BoxItem(new Box("", 656, 18, 2033, 0)),
				new BoxItem(new Box("", 100, 850, 750, 0)),
				new BoxItem(new Box("", 700, 400, 50, 0)),
				new BoxItem(new Box("", 80, 770, 850, 0)),
				new BoxItem(new Box("", 80, 770, 850, 0)),
				new BoxItem(new Box("", 40, 100, 165, 0)),
				new BoxItem(new Box("", 40, 100, 165, 0)),
				new BoxItem(new Box("", 40, 100, 165, 0)),
				new BoxItem(new Box("", 16, 2500, 11, 0)),
				new BoxItem(new Box("", 16, 2500, 11, 0)),
				new BoxItem(new Box("", 18, 2720, 160, 0)),
				new BoxItem(new Box("", 2500, 650, 30, 0)),
				new BoxItem(new Box("", 700, 400, 50, 0)),
				new BoxItem(new Box("", 75, 650, 1600, 0)),
				new BoxItem(new Box("", 100, 650, 750, 0)),
				new BoxItem(new Box("", 720, 620, 78, 0)),
				new BoxItem(new Box("", 55, 500, 745, 0)),
				new BoxItem(new Box("", 750, 17, 30, 0)),
				new BoxItem(new Box("", 535, 110, 500, 0)),
				new BoxItem(new Box("", 100, 550, 750, 0)),
				new BoxItem(new Box("", 700, 500, 50, 0)),
				new BoxItem(new Box("", 656, 18, 2033, 0)),
				new BoxItem(new Box("", 656, 18, 2033, 0)),
				new BoxItem(new Box("", 100, 850, 750, 0)),
				new BoxItem(new Box("", 700, 400, 50, 0)),
				new BoxItem(new Box("", 80, 770, 850, 0)),
				new BoxItem(new Box("", 80, 770, 850, 0)),
				new BoxItem(new Box("", 40, 100, 165, 0)),
				new BoxItem(new Box("", 40, 100, 165, 0)),
				new BoxItem(new Box("", 40, 100, 165, 0)),
				new BoxItem(new Box("", 16, 2500, 11, 0)),
				new BoxItem(new Box("", 16, 2500, 11, 0)),
				new BoxItem(new Box("", 18, 2720, 160, 0)),
				new BoxItem(new Box("", 2500, 650, 30, 0)),
				new BoxItem(new Box("", 700, 400, 50, 0)),
				new BoxItem(new Box("", 75, 650, 1600, 0)),
				new BoxItem(new Box("", 100, 650, 750, 0)),
				new BoxItem(new Box("", 720, 620, 78, 0)),
				new BoxItem(new Box("", 55, 500, 745, 0)),
				new BoxItem(new Box("", 750, 17, 30, 0)),
				new BoxItem(new Box("", 535, 110, 500, 0)),
				new BoxItem(new Box("", 100, 550, 750, 0)),
				new BoxItem(new Box("", 700, 500, 50, 0)),
				new BoxItem(new Box("", 1000, 1000, 1000, 0)));
	}


	List<BoxItem> listOf20Products() {
		return Arrays.asList(
				new BoxItem(new Box("", 75, 650, 1600, 350)),
				new BoxItem(new Box("", 600, 150, 18, 14)),
				new BoxItem(new Box("", 720, 620, 78, 130)),
				new BoxItem(new Box("", 611, 31, 791, 99)),
				new BoxItem(new Box("", 611, 31, 791, 99)),
				new BoxItem(new Box("", 656, 18, 2033, 143)),
				new BoxItem(new Box("", 656, 18, 2033, 143)),
				new BoxItem(new Box("", 100, 850, 750, 250)),
				new BoxItem(new Box("", 700, 400, 50, 60)),
				new BoxItem(new Box("", 80, 770, 850, 150)),
				new BoxItem(new Box("", 80, 770, 850, 150)),
				new BoxItem(new Box("", 40, 100, 165, 1)),
				new BoxItem(new Box("", 40, 100, 165, 1)),
				new BoxItem(new Box("", 40, 100, 165, 1)),
				new BoxItem(new Box("", 16, 2500, 11, 4)),
				new BoxItem(new Box("", 16, 2500, 11, 4)),
				new BoxItem(new Box("", 18, 2720, 160, 48)),
				new BoxItem(new Box("", 2500, 650, 30, 210)),
				new BoxItem(new Box("", 700, 400, 50, 60)),
				new BoxItem(new Box("", 75, 650, 1600, 350)),
				new BoxItem(new Box("", 100, 650, 750, 220)),
				new BoxItem(new Box("", 720, 620, 78, 130)),
				new BoxItem(new Box("", 55, 500, 745, 90)),
				new BoxItem(new Box("", 750, 17, 30, 26)),
				new BoxItem(new Box("", 535, 110, 500, 35)),
				new BoxItem(new Box("", 100, 550, 750, 200)),
				new BoxItem(new Box("", 700, 500, 50, 75)),
				new BoxItem(new Box("", 1000, 1000, 1000, 100)));
	}
}
