package com.github.skjolber.packing.accelerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;

/**
 * Random small packing problems: tight containers, so that not all boxes fit, with duplicates, rotations and weights, and some where the weight limit
 * is what limits the load.
 */
public final class FlatTestInstances {

	public static final class Instance {

		public final Container container;
		public final List<BoxItem> boxItems;
		public final int boxes;

		Instance(Container container, List<BoxItem> boxItems, int boxes) {
			this.container = container;
			this.boxItems = boxItems;
			this.boxes = boxes;
		}

		/** @return new box items (the packagers may change them) */
		public List<BoxItem> copyBoxItems() {
			List<BoxItem> result = new ArrayList<>(boxItems.size());
			for (BoxItem boxItem : boxItems) {
				result.add(new BoxItem(boxItem.getBox(), boxItem.getCount()));
			}
			return result;
		}
	}

	private FlatTestInstances() {
	}

	private static int size(Random random, int max) {
		return Math.max(1 + random.nextInt(max), 1 + random.nextInt(max));
	}

	/**
	 * @param seed the seed
	 * @param minBoxes the least number of boxes (counting duplicates)
	 * @param maxBoxes the most number of boxes
	 * @return an instance
	 */
	public static Instance random(long seed, int minBoxes, int maxBoxes) {
		Random random = new Random(seed);

		int cx = 3 + random.nextInt(6);
		int cy = 3 + random.nextInt(6);
		int cz = 3 + random.nextInt(6);
		int max = Math.min(cx, Math.min(cy, cz));

		List<BoxItem> boxItems = new ArrayList<>();
		int boxes = minBoxes + random.nextInt(maxBoxes - minBoxes + 1);
		int remaining = boxes;
		int itemCount = 2 + random.nextInt(4);
		int totalWeight = 0;
		for (int i = 0; i < itemCount && remaining > 0; i++) {
			int count = i == itemCount - 1 ? remaining : Math.min(remaining, 1 + random.nextInt(3));
			remaining -= count;
			int weight = 1 + random.nextInt(4);
			totalWeight += count * weight;
			Box.Builder builder = Box.newBuilder().withId(Integer.toString(i)).withSize(size(random, max), size(random, max), size(random, max)).withWeight(weight);
			int rotation = random.nextInt(3);
			if(rotation == 1) {
				builder.withRotate2D();
			} else if(rotation == 2) {
				builder.withRotate3D();
			}
			boxItems.add(new BoxItem(builder.build(), count));
		}

		// every third container is limited by weight, but not so that a box does not fit
		int maxLoadWeight = seed % 3 == 0 ? Math.max(4, totalWeight * (50 + random.nextInt(40)) / 100) : 100_000;
		Container container = Container.newBuilder().withId("c" + seed).withEmptyWeight(1).withSize(cx, cy, cz).withMaxLoadWeight(maxLoadWeight).build();
		return new Instance(container, boxItems, boxes - remaining);
	}
}
