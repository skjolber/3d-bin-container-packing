package com.github.skjolber.packing.packer.bruteforce.reference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;

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
	 * Boxes which fill the container exactly, so that a packing of all boxes exists: the container is split recursively
	 * into two pieces by a plane (guillotine cuts) until there are the wanted number of pieces, and each piece is a box
	 * in its original orientation. A box can also be rotated by its rotation (the original orientation is always one of
	 * them), and equal pieces are the same box item, i.e. duplicates.
	 *
	 * @param seed seed
	 * @param maxBoxes maximum number of boxes (sum of the counts), at least 2
	 * @param maxStates maximum number of permutation and rotation states, a bound on the cost of enumerating them
	 * @return a scenario in which all boxes fit together
	 */
	static ReferenceScenario guillotine(long seed, int maxBoxes, long maxStates) {
		for (int attempt = 0; attempt < 1000; attempt++) {
			ReferenceScenario scenario = guillotine(new Random(seed * 1000 + attempt), "guillotine seed " + seed, maxBoxes);
			if(scenario.countStates() <= maxStates) {
				return scenario;
			}
		}
		throw new IllegalStateException("No guillotine scenario for seed " + seed);
	}

	private static ReferenceScenario guillotine(Random random, String name, int maxBoxes) {
		int[] size = new int[] { 2 + random.nextInt(5), 2 + random.nextInt(5), 1 + random.nextInt(4) };
		ReferenceScenario scenario = new ReferenceScenario(name, size[0], size[1], size[2]);

		List<int[]> pieces = new ArrayList<>();
		pieces.add(size);

		int target = 2 + random.nextInt(maxBoxes - 1);
		while (pieces.size() < target) {
			// pieces which can be cut: at least one side of two or more
			List<int[]> cuttable = new ArrayList<>();
			for (int[] piece : pieces) {
				if(piece[0] > 1 || piece[1] > 1 || piece[2] > 1) {
					cuttable.add(piece);
				}
			}
			if(cuttable.isEmpty()) {
				break;
			}
			int[] piece = cuttable.get(random.nextInt(cuttable.size()));

			int axis;
			do {
				axis = random.nextInt(3);
			} while (piece[axis] < 2);

			// half of the cuts in the middle, so that there are equal pieces
			int at = piece[axis] % 2 == 0 && random.nextBoolean() ? piece[axis] / 2 : 1 + random.nextInt(piece[axis] - 1);

			int[] first = piece.clone();
			int[] second = piece.clone();
			first[axis] = at;
			second[axis] = piece[axis] - at;

			pieces.remove(piece);
			pieces.add(first);
			pieces.add(second);
		}

		// equal pieces with the same rotation are one box item
		Map<String, Spec> items = new LinkedHashMap<>();
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (int[] piece : pieces) {
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
			String key = piece[0] + "x" + piece[1] + "x" + piece[2] + "/" + (rotation == Rotation.THREE_D ? "3D" : rotation == Rotation.TWO_D ? "2D" : "none");
			counts.merge(key, 1, Integer::sum);
			items.putIfAbsent(key, new Spec("box-" + items.size(), piece[0], piece[1], piece[2], rotation, 1, 1));
		}
		for (Map.Entry<String, Spec> entry : items.entrySet()) {
			Spec spec = entry.getValue();
			scenario.add(spec.id(), spec.dx(), spec.dy(), spec.dz(), spec.rotation(), counts.get(entry.getKey()));
		}
		return scenario;
	}

	/**
	 * A squared rectangle (in Bouwkamp notation) as squares of height one in a container of height one, with equal squares
	 * as one box item. All boxes fit together, as the squares tile the rectangle.
	 *
	 * @param code the squared rectangle
	 * @return a scenario
	 */
	static ReferenceScenario bouwkamp(BouwkampCode code) {
		Map<Integer, Integer> frequencies = new TreeMap<>();
		for (BouwkampCodeLine line : code.getLines()) {
			for (Integer square : line.getSquares()) {
				frequencies.merge(square, 1, Integer::sum);
			}
		}
		// as the Bouwkamp tests of the packagers: squares of height one in a container of height one
		ReferenceScenario scenario = new ReferenceScenario("Bouwkamp " + code.getName() + " (order " + code.getOrder() + ")", code.getWidth(), code.getDepth(), 1);
		for (Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
			scenario.add(Integer.toString(entry.getKey()), entry.getKey(), entry.getKey(), 1, Rotation.THREE_D, entry.getValue());
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
