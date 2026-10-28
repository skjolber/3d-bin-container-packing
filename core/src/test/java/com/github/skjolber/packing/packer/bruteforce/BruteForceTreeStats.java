package com.github.skjolber.packing.packer.bruteforce;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.packer.bruteforce.TreeTestInstances.Instance;

/** Run by hand: the time of the regular packager with the loop and with the tree on random problems. Arguments: first seed, seeds, min boxes, max boxes. */
public class BruteForceTreeStats {

	public static void main(String[] args) {
		int firstSeed = args.length > 0 ? Integer.parseInt(args[0]) : 0;
		int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 10;
		int minBoxes = args.length > 2 ? Integer.parseInt(args[2]) : 7;
		int maxBoxes = args.length > 3 ? Integer.parseInt(args[3]) : 8;
		boolean tight = args.length > 4 && args[4].equals("tight");
		String mode = args.length > 5 ? args[5] : "both";
		BruteForcePackager loop = BruteForcePackager.newBuilder().withTreeSearch(false).build();
		BruteForcePackager tree = BruteForcePackager.newBuilder().build();
		long loopTotal = 0;
		long treeTotal = 0;
		for (int seed = firstSeed; seed < firstSeed + seeds; seed++) {
			Instance instance = tight ? TreeTestInstances.tight(seed, minBoxes, maxBoxes) : TreeTestInstances.random(seed, minBoxes, maxBoxes);
			List<ContainerItem> containerItems = BruteForceTreeSearchTest.containerItems(instance, seed);
			long start = System.nanoTime();
			PackagerResult l = mode.equals("tree") ? null : pack(loop, containerItems, instance.copyBoxItems());
			long loopNanos = System.nanoTime() - start;
			start = System.nanoTime();
			PackagerResult t = mode.equals("loop") ? null : pack(tree, containerItems, instance.copyBoxItems());
			long treeNanos = System.nanoTime() - start;
			if(l == null || t == null) {
				System.out.println("seed " + seed + " boxes " + instance.boxes + ": loop " + loopNanos / 1_000_000 + " ms, tree " + treeNanos / 1_000_000 + " ms");
				continue;
			}
			loopTotal += loopNanos;
			treeTotal += treeNanos;
			String same = BruteForceTreeSearchTest.describe(l).equals(BruteForceTreeSearchTest.describe(t)) ? "same" : "MISMATCH";
			System.out.println("seed " + seed + " boxes " + instance.boxes + ": loop " + loopNanos / 1_000_000 + " ms, tree " + treeNanos / 1_000_000 + " ms, " + same);
		}
		System.out.println("total: loop " + loopTotal / 1_000_000 + " ms, tree " + treeTotal / 1_000_000 + " ms");
		loop.close();
		tree.close();
	}

	private static PackagerResult pack(BruteForcePackager packager, List<ContainerItem> containerItems, List<BoxItem> boxItems) {
		return packager.newResultBuilder().withContainerItems(containerItems).withBoxItems(boxItems).withMaxContainerCount(8).withInterruptDuration(600_000).build();
	}
}
