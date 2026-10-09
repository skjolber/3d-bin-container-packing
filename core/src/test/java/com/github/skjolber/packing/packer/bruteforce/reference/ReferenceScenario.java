package com.github.skjolber.packing.packer.bruteforce.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Rotation;

/**
 * A container and box items for the differential tests. The box items are created anew for each use, so that the
 * implementations under test cannot influence one another through shared mutable state.
 */

final class ReferenceScenario {

	/** One box item */
	record Spec(String id, int dx, int dy, int dz, Rotation rotation, int weight, int count) {

		BoxItem newBoxItem() {
			return new BoxItem(ReferenceSupport.box(id, dx, dy, dz, rotation, weight), count);
		}

	}

	private final String name;
	private final int dx;
	private final int dy;
	private final int dz;
	private final int maxLoadWeight;
	private final List<Spec> specs = new ArrayList<>();

	ReferenceScenario(String name, int dx, int dy, int dz, int maxLoadWeight) {
		this.name = name;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.maxLoadWeight = maxLoadWeight;
	}

	ReferenceScenario(String name, int dx, int dy, int dz) {
		this(name, dx, dy, dz, ReferenceSupport.WEIGHT_UNLIMITED);
	}

	ReferenceScenario add(String id, int dx, int dy, int dz, Rotation rotation, int count) {
		return add(id, dx, dy, dz, rotation, 1, count);
	}

	ReferenceScenario add(String id, int dx, int dy, int dz, Rotation rotation, int weight, int count) {
		specs.add(new Spec(id, dx, dy, dz, rotation, weight, count));
		return this;
	}

	String name() {
		return name;
	}

	Container newContainer() {
		return ReferenceSupport.container(dx, dy, dz, maxLoadWeight);
	}

	List<BoxItem> newBoxItems() {
		List<BoxItem> items = new ArrayList<>(specs.size());
		for (Spec spec : specs) {
			items.add(spec.newBoxItem());
		}
		return items;
	}

	/**
	 * @return the number of boxes, i.e. the sum of the counts of the box items
	 */
	int boxCount() {
		int count = 0;
		for (Spec spec : specs) {
			count += spec.count();
		}
		return count;
	}

	/**
	 * @return the volume of all boxes
	 */
	long volume() {
		long volume = 0;
		for (Spec spec : specs) {
			volume += (long)spec.dx() * spec.dy() * spec.dz() * spec.count();
		}
		return volume;
	}

	List<Spec> specs() {
		return specs;
	}

	long containerVolume() {
		return (long)dx * dy * dz;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(name).append(" in ").append(dx).append('x').append(dy).append('x').append(dz);
		if(maxLoadWeight != ReferenceSupport.WEIGHT_UNLIMITED) {
			builder.append(" max load weight ").append(maxLoadWeight);
		}
		builder.append(':');
		for (Spec spec : specs) {
			builder.append(' ').append(spec.count()).append('x').append(spec.dx()).append('x').append(spec.dy()).append('x').append(spec.dz());
			builder.append(spec.rotation() == Rotation.THREE_D ? "(3D)" : spec.rotation() == Rotation.TWO_D ? "(2D)" : "");
			if(spec.weight() != 1) {
				builder.append("@").append(spec.weight());
			}
		}
		return builder.toString();
	}

	/**
	 * Random box items in a random container, with at most {@code maxBoxes} boxes in total; boxes of the sizes of the
	 * container (some rotations do not fit, some boxes are too big), mixed rotations and duplicates.
	 *
	 * @param seed seed
	 * @param maxBoxes maximum number of boxes (sum of the counts)
	 * @param maxStates maximum number of permutation and rotation states, a bound on the cost of enumerating them
	 * @param tight whether the container is mostly filled by the boxes (otherwise the boxes are small compared to the container)
	 * @return a scenario
	 */
	static ReferenceScenario random(long seed, int maxBoxes, long maxStates, boolean tight) {
		for (int attempt = 0; attempt < 1000; attempt++) {
			ReferenceScenario scenario = random(new Random(seed * 1000 + attempt), "seed " + seed, maxBoxes, tight);
			if(scenario.countStates() <= maxStates) {
				return scenario;
			}
		}
		throw new IllegalStateException("No scenario for seed " + seed);
	}

	private static ReferenceScenario random(Random random, String name, int maxBoxes, boolean tight) {
		int cx = 3 + random.nextInt(4);
		int cy = 3 + random.nextInt(4);
		int cz = 1 + random.nextInt(4);
		// weights are 1-3; some boxes are heavier than the load weight
		int maxLoadWeight = random.nextInt(4) == 0 ? 2 : ReferenceSupport.WEIGHT_UNLIMITED;
		ReferenceScenario scenario = new ReferenceScenario(name, cx, cy, cz, maxLoadWeight);

		int remaining = 2 + random.nextInt(maxBoxes - 1);
		int sequence = 0;
		while (remaining > 0) {
			int count = 1 + random.nextInt(Math.min(3, remaining));
			int maxSide = tight ? Math.max(cx, Math.max(cy, cz)) : Math.max(1, Math.min(cx, Math.min(cy, cz)) / 2 + 1);
			Rotation rotation;
			switch (random.nextInt(3)) {
				case 0:
					rotation = ReferenceSupport.NO_ROTATION;
					break;
				case 1:
					rotation = Rotation.TWO_D;
					break;
				default:
					rotation = Rotation.THREE_D;
			}
			scenario.add("box-" + sequence++, 1 + random.nextInt(maxSide), 1 + random.nextInt(maxSide), 1 + random.nextInt(maxSide), rotation, 1 + random.nextInt(3), count);
			remaining -= count;
		}
		return scenario;
	}

	/**
	 * @return the number of permutation and rotation states of the boxes which fit the container, saturated at {@link Long#MAX_VALUE}
	 */
	long countStates() {
		ReferencePermutationRotationIterator iterator = ReferencePermutationRotationIterator.newBuilder()
				.withLoadSize(dx, dy, dz)
				.withMaxLoadWeight(maxLoadWeight)
				.withBoxItems(newBoxItems())
				.build();
		if(iterator.length() == 0) {
			return 0;
		}
		long permutations = iterator.countPermutations();
		long rotations = iterator.countRotations();
		if(permutations == -1L || rotations == -1L || permutations > Long.MAX_VALUE / rotations) {
			return Long.MAX_VALUE;
		}
		return permutations * rotations;
	}

}
