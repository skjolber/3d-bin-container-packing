package com.github.skjolber.packing.jmh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;

/**
 * Box item groups (without a box item order) for the brute-force packagers: a container holds two or three of the
 * groups, so that the packagers search many orders of the groups for each container.
 */
@State(Scope.Benchmark)
public class GroupBruteForceBenchmarkState {

	public static final int THREADS = 4;

	public static final int CONTAINERS = 8;

	@Param(value = { "6", "7", "8" })
	private int groups = 7;

	private ExecutorService pool;

	private BruteForcePackager bruteForcePackager;
	private ParallelBruteForcePackager parallelBruteForcePackager;

	private List<BoxItemGroup> boxItemGroups;
	private List<ContainerItem> containerItems;

	public GroupBruteForceBenchmarkState() {
	}

	public GroupBruteForceBenchmarkState(int groups) {
		this.groups = groups;
	}

	@Setup(Level.Trial)
	public void init() {
		pool = Executors.newFixedThreadPool(THREADS, new DefaultThreadFactory());

		bruteForcePackager = BruteForcePackager.newBuilder().build();
		parallelBruteForcePackager = ParallelBruteForcePackager.newBuilder()
				.withExecutorService(pool)
				.withParallelizationCount(THREADS * 16)
				.build();

		boxItemGroups = createGroups(groups);
		containerItems = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("container").withSize(6, 3, 2).withEmptyWeight(1).withMaxLoadWeight(1000).build(), CONTAINERS)
				.build();
	}

	/**
	 * @return groups of two boxes each, the same for each run
	 */
	public static List<BoxItemGroup> createGroups(int count) {
		Random random = new Random(42);
		List<BoxItemGroup> groups = new ArrayList<>();
		for (int g = 0; g < count; g++) {
			List<BoxItem> items = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				Box box = Box.newBuilder()
						.withId("box-" + g + "-" + i)
						.withSize(1 + random.nextInt(3), 1 + random.nextInt(3), 1 + random.nextInt(2))
						.withRotate2D()
						.withWeight(1)
						.build();
				items.add(new BoxItem(box, 1));
			}
			groups.add(new BoxItemGroup("group-" + g, items));
		}
		return groups;
	}

	@TearDown(Level.Trial)
	public void shutdown() {
		bruteForcePackager.close();
		parallelBruteForcePackager.close();
		pool.shutdownNow();
	}

	public BruteForcePackager getBruteForcePackager() {
		return bruteForcePackager;
	}

	public ParallelBruteForcePackager getParallelBruteForcePackager() {
		return parallelBruteForcePackager;
	}

	public List<BoxItemGroup> getBoxItemGroups() {
		return boxItemGroups;
	}

	public List<ContainerItem> getContainerItems() {
		return containerItems;
	}
}
