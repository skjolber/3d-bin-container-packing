package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Illustrates how the LAFF packager and brute force packager can be used together.
 *
 */

public class PackagerCombinationTest {

	@Test
	public void testBindeStrategy() {
		List<Dimension> containers = new ArrayList<Dimension>();
		for(int i = 0; i < 10; i++) {
			containers.add(new Dimension(Integer.toString(i), 5*(i+1), 10, 10));
		}

		Map<String, Integer> map = new HashMap<>();
		for(int i = 0; i < containers.size(); i++) {
			map.put(containers.get(i).getName(), i);
		}

		for(int k = 0; k < 10; k++) {

			LargestAreaFitFirstPackager light = new LargestAreaFitFirstPackager(containers);
			BruteForcePackager heavy = new BruteForcePackager(containers, true);

			List<Box> products = new ArrayList<Box>();

			long volume = 0;
			for(int i = 0; i < (k + 1); i++) {
				Box b = new Box(Integer.toString(i), 5, 10, 10);
				products.add(b);

				volume += b.getVolume();
			}

			Container pack = light.pack(products);

			Integer index;
			if(pack != null) {
				index = map.get(pack.getName());

				if(pack.getVolume() == volume) {
					System.out.println("Perfect match at index " + index);

					continue;
				} else {
					if(index == 0) {
						System.out.println("Perfect match at index " + index);
					} else {
						System.out.println("Initial match at index " + index);
					}
				}
			} else {
				index = containers.size();
			}

			Box[][] rotationMatrix = PermutationRotationIterator.toRotationMatrix(products, true);
			List<Placement> placements = BruteForcePackager.getPlacements(products.size());

			long deadline = System.currentTimeMillis() + 5000;

			for(int i = 0; i < index; i++) { // TODO: even better with more of a binary search approach
				Dimension dimension = containers.get(i);
				if(dimension.getVolume() < volume) {
					continue;
				}
				PermutationRotationIterator iterator = new PermutationRotationIterator(dimension, rotationMatrix);

				// if not to complex, then
				Container heavyResult = heavy.pack(placements, dimension, iterator, deadline);
				if(heavyResult != null) {
					pack = heavyResult;

					System.out.println("Better match at index " + i);

					break;
				}
			}
		}
	}
}
