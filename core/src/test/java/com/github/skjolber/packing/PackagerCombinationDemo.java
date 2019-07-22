package com.github.skjolber.packing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.LargestAreaFitFirstPackager;

/**
 * Illustrates how the LAFF packager and brute force packager can be used together.
 *
 */
public class PackagerCombinationDemo {

	public static void main(String[] args) {
		List<Container> containers = new ArrayList<>();
		for(int i = 0; i < 10; i++) {
			containers.add(new Container(Integer.toString(i), 5*(i+1), 10, 10, 0));
		}

		Map<String, Integer> map = new HashMap<>();
		for(int i = 0; i < containers.size(); i++) {
			map.put(containers.get(i).getName(), i);
		}

		for(int k = 0; k < 10; k++) {

			System.out.println("************* " + k + " *************");
			LargestAreaFitFirstPackager light = new LargestAreaFitFirstPackager(containers);

			List<BoxItem> products = new ArrayList<>();

			long volume = 0;
			for(int i = 0; i < (k + 1); i++) {
				Box b = new Box(Integer.toString(i), 5, 10, 10, 0);
				products.add(new BoxItem(b, 1));

				volume += b.getVolume();
			}

			Container pack = light.pack(products);

			Integer index;
			if(pack != null) {
				index = map.get(pack.getName());

				if(pack.getVolume() == volume) {
					System.out.println("Full volume match at index " + index);

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

			if(index > 0) {
				long deadline = System.currentTimeMillis() + 5000;

				BruteForcePackager heavy = new BruteForcePackager(containers.subList(0, index), true, true);

				Container heavyResult = heavy.pack(products, deadline);
				// if not to complex, then
				if(heavyResult != null) {
					pack = heavyResult;

					Integer betterIndex = map.get(pack.getName());

					System.out.println("Better match at index " + betterIndex);
				}
			}
		}
	}
}
