package com.github.skjolber.packing.jmh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.generator.Item;
import com.github.skjolber.packing.test.generator.ItemIO;

/**
 * 
 * This benchmark is heavy on permutations (not rotations)
 * 
 */

@State(Scope.Benchmark)
public class EgyPackagerState {

	private final int threadPoolSize;

	private ExecutorService pool1;
	private ExecutorService pool2;

	private List<BenchmarkSet> parallelBruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> bruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> plainPackager = new ArrayList<>();
	private List<BenchmarkSet> fastBruteForcePackager = new ArrayList<>();

	private static List<BoxItem> stackableItems3D;
	private static List<ContainerItem> containers;

	static {
		Path path = Paths.get("src", "main", "resources", "egy.json");

		if(!Files.exists(path)) {
			path = Paths.get("jmh", "src", "main", "resources", "egy.json");
		}

		try {
			List<Item> items = ItemIO.read(path);

			containers = ContainerItem.newListBuilder().withContainer(getContainer(items)).build();

			stackableItems3D = getStackableItems3D(items);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public EgyPackagerState() {
		this(8);
	}

	public EgyPackagerState(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;

		this.pool1 = Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory());
		this.pool2 = Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory());
	}

	@Setup(Level.Trial)
	public void init() {
		ParallelBoxItemBruteForcePackager parallelPackager = ParallelBoxItemBruteForcePackager.newBuilder().withExecutorService(pool2).withParallelizationCount(threadPoolSize * 16)
				.build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();

		PlainPackager plainPackager = PlainPackager.newBuilder().build();

		FastBruteForcePackager fastPackager = FastBruteForcePackager.newBuilder().build();

		// single-threaded
		this.bruteForcePackager.add(new BenchmarkSet(packager, stackableItems3D, containers));

		this.plainPackager.add(new BenchmarkSet(plainPackager, stackableItems3D, containers));

		this.fastBruteForcePackager.add(new BenchmarkSet(fastPackager, stackableItems3D, containers));

		// multi-threaded
		this.parallelBruteForcePackager.add(new BenchmarkSet(parallelPackager, stackableItems3D, containers));
	}

	public static Container getContainer(List<Item> items) {

		long originalVolume = 0;
		for (Item item : items) {
			originalVolume += item.getVolume();
		}

		double multiplier = 5;
		while (true) {
			long volume = (long)(originalVolume * multiplier);

			long side = (long)Math.pow(volume, (1 / 3));
			while (side * side * side < volume) {
				side++;
			}

			int length = (int)side;

			List<ContainerItem> containers = ContainerItem.newListBuilder().withContainer(
					Container.newBuilder().withDescription("Container").withEmptyWeight(1).withSize(length, length, length).withMaxLoadWeight(length * length * length)
							.withStack(new Stack()).build())
					.build();

			List<BoxItem> stackableItems3D = getStackableItems3D(items);

			FastBruteForcePackager fastPackager = FastBruteForcePackager.newBuilder().build();

			PackagerResult build = fastPackager
					.newResultBuilder()
					.withContainerItems(containers)
					.withMaxContainerCount(1)
					.withBoxItems(stackableItems3D)
					.withDeadline(System.currentTimeMillis() + 5000)
					.build();
			if(build.isSuccess()) {
				System.out.println("Got container " + volume + " from " + originalVolume);
				return containers.get(0).getContainer();
			}
			multiplier += 0.05;
			System.out.println("Try " + multiplier);
		}
	}

	private static List<BoxItem> getStackableItems3D(List<Item> items) {
		List<BoxItem> products = new ArrayList<>();
		for (Item item : items) {
			products.add(new BoxItem(Box.newBuilder().withDescription(item.toString()).withSize(item.getDx(), item.getDy(), item.getDz()).withRotate3D().withWeight(1).build(), item.getCount()));
		}

		return products;
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		pool1.shutdown();
		pool2.shutdown();

		for (BenchmarkSet benchmarkSet : parallelBruteForcePackager) {
			benchmarkSet.getPackager().close();
		}
		for (BenchmarkSet benchmarkSet : bruteForcePackager) {
			benchmarkSet.getPackager().close();
		}
		for (BenchmarkSet benchmarkSet : plainPackager) {
			benchmarkSet.getPackager().close();
		}
		for (BenchmarkSet benchmarkSet : fastBruteForcePackager) {
			benchmarkSet.getPackager().close();
		}

		try {
			Thread.sleep(500);
		} catch (Exception e) {
			// ignore
		}
	}

	public List<BenchmarkSet> getBruteForcePackager() {
		return bruteForcePackager;
	}

	public List<BenchmarkSet> getParallelBruteForcePackager() {
		return parallelBruteForcePackager;
	}

	public List<BenchmarkSet> getPlainPackager() {
		return plainPackager;
	}

	public List<BenchmarkSet> getFastBruteForcePackager() {
		return fastBruteForcePackager;
	}

}
