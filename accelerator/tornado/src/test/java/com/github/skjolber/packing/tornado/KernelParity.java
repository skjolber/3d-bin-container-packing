package com.github.skjolber.packing.tornado;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.accelerator.AcceleratorWinner;
import com.github.skjolber.packing.accelerator.FlatEngine;
import com.github.skjolber.packing.accelerator.FlatProblem;

/**
 * Run by hand, like {@linkplain GpuSmoke} (see the README): the winner of the device must be the winner of the CPU engine, for random problems and for different
 * windows and budgets. The CPU engine is pinned to the packager's loop by the tests of the core module.
 * <p>
 * Arguments: first seed, number of seeds, fewest boxes, most boxes, window, node budget, max points, target items, CPU threads, {@code true} to search each problem
 * twice on the device (the second search reuses the compiled kernel), and the local work size. Prints the mismatches, and exits with 1 if there is one.
 */
public class KernelParity {

	static final class Instance {

		final Container container;
		final List<BoxItem> boxItems;

		Instance(Container container, List<BoxItem> boxItems) {
			this.container = container;
			this.boxItems = boxItems;
		}
	}

	private static int size(Random random, int max) {
		return Math.max(1 + random.nextInt(max), 1 + random.nextInt(max));
	}

	/** Tight containers, so that not all boxes fit, with duplicates, rotations and weights, and some where the weight limit is what limits the load. */
	static Instance random(long seed, int minBoxes, int maxBoxes, int minSide, int maxSide) {
		Random random = new Random(seed);

		int cx = minSide + random.nextInt(maxSide - minSide + 1);
		int cy = minSide + random.nextInt(maxSide - minSide + 1);
		int cz = minSide + random.nextInt(maxSide - minSide + 1);
		int max = Math.min(cx, Math.min(cy, cz));

		List<BoxItem> boxItems = new ArrayList<>();
		int boxes = minBoxes + random.nextInt(maxBoxes - minBoxes + 1);
		int remaining = boxes;
		int itemCount = 3 + random.nextInt(6);
		int totalWeight = 0;
		for (int i = 0; i < itemCount && remaining > 0; i++) {
			int count = i == itemCount - 1 ? remaining : Math.min(remaining, 1 + random.nextInt(2));
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

		int maxLoadWeight = seed % 3 == 0 ? Math.max(4, totalWeight * (50 + random.nextInt(40)) / 100) : 100_000;
		Container container = Container.newBuilder().withId("c" + seed).withEmptyWeight(1).withSize(cx, cy, cz).withMaxLoadWeight(maxLoadWeight).build();
		return new Instance(container, boxItems);
	}

	static FlatProblem problem(Instance instance) {
		return problem(instance, false);
	}

	/** @param anyBest true if any arrangement of the best value will do */
	static FlatProblem problem(Instance instance, boolean anyBest) {
		return problem(instance, anyBest, false);
	}

	/** @param allPoints true for the search of {@code BruteForcePackager}, every box at every free point where it fits */
	static FlatProblem problem(Instance instance, boolean anyBest, boolean allPoints) {
		Container container = instance.container;
		List<BoxItem> copy = new ArrayList<>();
		for (BoxItem boxItem : instance.boxItems) {
			copy.add(new BoxItem(boxItem.getBox(), boxItem.getCount()));
		}
		BoxItemPermutationRotationIterator iterator = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(copy)
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();
		return FlatProblem.of(iterator, container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), container.getMaxLoadVolume(), container.getMaxLoadWeight(), iterator.length(), anyBest,
				allPoints);
	}

	/** @return true if the arrangements have the same value: count, load volume, load weight and whether all boxes are placed */
	static boolean sameValue(AcceleratorWinner a, AcceleratorWinner b) {
		return a.getCount() == b.getCount() && a.getLoadVolume() == b.getLoadVolume() && a.getLoadWeight() == b.getLoadWeight() && a.isFullyPacked() == b.isFullyPacked();
	}

	static boolean same(AcceleratorWinner a, AcceleratorWinner b) {
		if(a.getCount() != b.getCount()) {
			return false;
		}
		if(a.getCount() == 0) {
			return true;
		}
		return a.getLoadVolume() == b.getLoadVolume() && a.getLoadWeight() == b.getLoadWeight() && a.isFullyPacked() == b.isFullyPacked()
				&& Arrays.equals(a.getPermutation(), b.getPermutation()) && Arrays.equals(Arrays.copyOf(a.getRotations(), a.getCount()), Arrays.copyOf(b.getRotations(), b.getCount()));
	}

	public static void main(String[] args) throws Exception {
		long firstSeed = args.length > 0 ? Long.parseLong(args[0]) : 1;
		int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 20;
		int minBoxes = args.length > 2 ? Integer.parseInt(args[2]) : 8;
		int maxBoxes = args.length > 3 ? Integer.parseInt(args[3]) : 9;
		int window = args.length > 4 ? Integer.parseInt(args[4]) : TornadoBruteForceAccelerator.DEFAULT_WINDOW;
		int budget = args.length > 5 ? Integer.parseInt(args[5]) : TornadoBruteForceAccelerator.DEFAULT_NODE_BUDGET;
		int maxPoints = args.length > 6 ? Integer.parseInt(args[6]) : TornadoBruteForceAccelerator.DEFAULT_MAX_POINTS;
		int targetItems = args.length > 7 ? Integer.parseInt(args[7]) : TornadoBruteForceAccelerator.DEFAULT_TARGET_ITEMS;
		int threads = args.length > 8 ? Integer.parseInt(args[8]) : Runtime.getRuntime().availableProcessors();
		boolean repeat = args.length > 9 && Boolean.parseBoolean(args[9]);
		int localWorkSize = args.length > 10 ? Integer.parseInt(args[10]) : TornadoBruteForceAccelerator.DEFAULT_LOCAL_WORK_SIZE;
		// "exact": the tree kernel against the loop of the CPU engine, the same winner; "any": the tree kernel and the tree of the CPU engine, any best
		// arrangement, the same value; "loop": the loop kernel against the loop of the CPU engine
		// "allpointsexact" and "allpointsany": the search of BruteForcePackager (every box at every free point), the tree kernel against the CPU tree engine
		String mode = args.length > 11 ? args[11] : "exact";
		boolean allPoints = mode.startsWith("allpoints");
		boolean anyBest = mode.equals("any") || mode.equals("allpointsany");
		boolean tree = !mode.equals("loop");
		boolean freeSpace = !(args.length > 12 && args[12].equals("nospace"));

		TornadoBruteForceAccelerator device = TornadoBruteForceAccelerator.newBuilder().withMinBoxes(1).withWindow(window).withNodeBudget(budget).withMaxPoints(maxPoints)
				.withTargetItems(targetItems).withLocalWorkSize(localWorkSize).withTree(tree).withFreeSpace(freeSpace).build();
		System.out.println("Device: " + device.getDevice().orElseThrow(() -> new IllegalStateException("No device found")) + ", window " + window + ", budget " + budget + ", max points "
				+ maxPoints + ", target items " + targetItems + ", local work size " + localWorkSize + ", " + mode + (freeSpace ? "" : ", no free space") + "; cpu threads " + threads);
		FlatEngine cpu = FlatEngine.newBuilder().withMinBoxes(1).withThreads(threads).withTree(anyBest || allPoints).withFreeSpace(freeSpace).build();

		int mismatches = 0;
		int gaveUp = 0;
		long cpuNanos = 0;
		long deviceNanos = 0;
		for (int i = 0; i < seeds; i++) {
			long seed = firstSeed + i;
			Instance instance = random(seed, minBoxes, maxBoxes, 4, 9);
			FlatProblem problem = problem(instance, anyBest, allPoints);

			long start = System.nanoTime();
			Optional<AcceleratorWinner> expected = cpu.search(problem, 0L, () -> false);
			long cpuTime = System.nanoTime() - start;
			cpuNanos += cpuTime;

			start = System.nanoTime();
			Optional<AcceleratorWinner> actual = device.search(problem, 0L, () -> false);
			long deviceTime = System.nanoTime() - start;
			deviceNanos += deviceTime;
			long repeatTime = -1;
			if(repeat) {
				// the same shape again: the compiled kernel and the device memory are reused
				start = System.nanoTime();
				device.search(problem, 0L, () -> false);
				repeatTime = System.nanoTime() - start;
			}

			if(device.getLastFailure() != null) {
				System.out.println("seed " + seed + ": device failure");
				device.getLastFailure().printStackTrace(System.out);
				System.exit(2);
			}
			String line = "seed " + seed + " n=" + problem.n() + ": cpu " + cpuTime / 1_000_000 + " ms (" + expected.map(AcceleratorWinner::getNodes).orElse(-1L) + " nodes), device "
					+ deviceTime / 1_000_000 + " ms" + (repeat ? " (again " + repeatTime / 1_000_000 + " ms)" : "");
			if(actual.isEmpty()) {
				gaveUp++;
				System.out.println(line + ", device gave up");
			} else if(expected.isEmpty() || !(anyBest ? sameValue(actual.get(), expected.get()) : same(actual.get(), expected.get()))) {
				mismatches++;
				System.out.println(line + ", MISMATCH: expected " + describe(expected) + ", got " + describe(actual));
			} else if(seeds <= 40) {
				System.out.println(line + ", same: " + describe(actual));
			}
		}
		System.out.println("seeds " + seeds + ", " + mode + ", mismatches " + mismatches + ", device gave up " + gaveUp + ", cpu " + cpuNanos / 1_000_000 + " ms, device " + deviceNanos / 1_000_000 + " ms, rounds "
				+ device.getRounds() + ", splits " + device.getSplits() + ", on device " + device.getNanosOnDevice() / 1_000_000 + " ms");
		System.exit(mismatches == 0 ? 0 : 1);
	}

	static String describe(Optional<AcceleratorWinner> winner) {
		if(winner.isEmpty()) {
			return "none";
		}
		AcceleratorWinner w = winner.get();
		return "count " + w.getCount() + " volume " + w.getLoadVolume() + " weight " + w.getLoadWeight() + " full " + w.isFullyPacked() + " nodes " + w.getNodes();
	}
}
