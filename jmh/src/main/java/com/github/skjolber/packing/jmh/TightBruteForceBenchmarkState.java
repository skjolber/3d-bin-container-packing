package com.github.skjolber.packing.jmh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;

/**
 * Random tight problems: many box types of few boxes each, which usually do not all fit the container, in two containers (the second a little
 * smaller), so the arrangements of partial orders and the bounds decide. Twenty instances per box count, the same for each run (the generator of
 * the core module's {@code TreeTestInstances.tight}).
 */
@State(Scope.Benchmark)
public class TightBruteForceBenchmarkState {

	public static final int INSTANCES = 20;
	public static final int THREADS = 4;

	@Param(value = { "7", "8" })
	private int boxes = 7;

	private BruteForcePackager bruteForcePackager;
	private FastBruteForcePackager fastBruteForcePackager;
	private ParallelBruteForcePackager parallelBruteForcePackager;
	private ExecutorService pool;
	private List<BenchmarkSet> bruteForceSets = new ArrayList<>();
	private List<BenchmarkSet> fastSets = new ArrayList<>();
	private List<BenchmarkSet> parallelSets = new ArrayList<>();

	@Setup
	public void init() {
		bruteForcePackager = TreeOptions.apply(BruteForcePackager.newBuilder()).build();
		fastBruteForcePackager = TreeOptions.threads(FastBruteForcePackager.newBuilder()).build();
		pool = Executors.newFixedThreadPool(THREADS, new DefaultThreadFactory());
		parallelBruteForcePackager = TreeOptions.apply(ParallelBruteForcePackager.newBuilder().withExecutorService(pool).withParallelizationCount(THREADS * 16)).build();
		for (int seed = 1000; seed < 1000 + INSTANCES; seed++) {
			List<BoxItem> boxItems = new ArrayList<>();
			Container container = tight(seed, boxes, boxes, boxItems);
			ContainerItem.Builder builder = ContainerItem.newListBuilder().withContainer(container, 8);
			Container second = Container.newBuilder().withId("second" + seed).withEmptyWeight(1)
					.withSize(Math.max(3, container.getLoadDx() - 1), container.getLoadDy(), Math.max(3, container.getLoadDz() - 1)).withMaxLoadWeight(100_000).build();
			builder.withContainer(second, 8);
			List<ContainerItem> containers = builder.build();
			bruteForceSets.add(new BenchmarkSet(bruteForcePackager, copy(boxItems), containers));
			fastSets.add(new BenchmarkSet(fastBruteForcePackager, copy(boxItems), containers));
			parallelSets.add(new BenchmarkSet(parallelBruteForcePackager, copy(boxItems), containers));
		}
	}

	@TearDown
	public void shutdown() {
		bruteForcePackager.close();
		fastBruteForcePackager.close();
		parallelBruteForcePackager.close();
		pool.shutdownNow();
	}

	private static List<BoxItem> copy(List<BoxItem> boxItems) {
		List<BoxItem> copy = new ArrayList<>(boxItems.size());
		for (BoxItem boxItem : boxItems) {
			copy.add(new BoxItem(boxItem.getBox(), boxItem.getCount()));
		}
		return copy;
	}

	private static int size(Random random, int max) {
		return Math.max(1 + random.nextInt(max), 1 + random.nextInt(max));
	}

	/** as TreeTestInstances.tight in the core module's tests: the container, and the box items added to the list */
	static Container tight(long seed, int minBoxes, int maxBoxes, List<BoxItem> boxItems) {
		Random random = new Random(seed);
		int cx = 4 + random.nextInt(6);
		int cy = 4 + random.nextInt(6);
		int cz = 4 + random.nextInt(6);
		int max = Math.min(cx, Math.min(cy, cz));
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
		return Container.newBuilder().withId("c" + seed).withEmptyWeight(1).withSize(cx, cy, cz).withMaxLoadWeight(maxLoadWeight).build();
	}

	public List<BenchmarkSet> getBruteForceSets() {
		return bruteForceSets;
	}

	public List<BenchmarkSet> getFastSets() {
		return fastSets;
	}

	public List<BenchmarkSet> getParallelSets() {
		return parallelSets;
	}
}
