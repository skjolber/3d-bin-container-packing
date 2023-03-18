package com.github.skjolber.packing.jmh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
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
	private final int nth;

	private ExecutorService pool1;
	private ExecutorService pool2;

	private List<BenchmarkSet> parallelBruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> parallelBruteForcePackagerNth = new ArrayList<>();
	private List<BenchmarkSet> bruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> bruteForcePackagerNth = new ArrayList<>();
	private List<BenchmarkSet> plainPackager = new ArrayList<>();
	private List<BenchmarkSet> plainPackagerNth = new ArrayList<>();
	private List<BenchmarkSet> fastBruteForcePackager = new ArrayList<>();

	private static List<StackableItem> stackableItems3D;
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
		this(8, 20000);
	}

	public EgyPackagerState(int threadPoolSize, int nth) {
		this.threadPoolSize = threadPoolSize;
		this.nth = nth;

		this.pool1 = Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory());
		this.pool2 = Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory());
	}

	@Setup(Level.Trial)
	public void init() {
		ParallelBruteForcePackager parallelPackager = ParallelBruteForcePackager.newBuilder().withExecutorService(pool2).withParallelizationCount(threadPoolSize * 16)
				.build();
		ParallelBruteForcePackager parallelPackagerNth = ParallelBruteForcePackager.newBuilder().withExecutorService(pool1).withParallelizationCount(threadPoolSize * 16)
				.withCheckpointsPerDeadlineCheck(nth).build();

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		BruteForcePackager packagerNth = BruteForcePackager.newBuilder().withCheckpointsPerDeadlineCheck(nth).build();

		PlainPackager plainPackager = PlainPackager.newBuilder().build();
		PlainPackager plainPackagerNth = PlainPackager.newBuilder().withCheckpointsPerDeadlineCheck(nth).build();

		FastBruteForcePackager fastPackager = FastBruteForcePackager.newBuilder().build();

		// single-threaded
		this.bruteForcePackager.add(new BenchmarkSet(packager, stackableItems3D, containers));
		this.bruteForcePackagerNth.add(new BenchmarkSet(packagerNth, stackableItems3D, containers));

		this.plainPackager.add(new BenchmarkSet(plainPackager, stackableItems3D, containers));
		this.plainPackagerNth.add(new BenchmarkSet(plainPackagerNth, stackableItems3D, containers));

		this.fastBruteForcePackager.add(new BenchmarkSet(fastPackager, stackableItems3D, containers));

		// multi-threaded
		this.parallelBruteForcePackager.add(new BenchmarkSet(parallelPackager, stackableItems3D, containers));
		this.parallelBruteForcePackagerNth.add(new BenchmarkSet(parallelPackagerNth, stackableItems3D, containers));
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
					.withStack(new DefaultStack()).build()).build();

			List<StackableItem> stackableItems3D = getStackableItems3D(items);

			FastBruteForcePackager fastPackager = FastBruteForcePackager.newBuilder().build();

			BooleanSupplier booleanSupplier = BooleanSupplierBuilder.builder().withDeadline(System.currentTimeMillis() + 5000, 1).build();
			
			Container pack = fastPackager.pack(stackableItems3D, containers, booleanSupplier);

			if(pack != null) {
				System.out.println("Go container " + volume + " from " + originalVolume);
				return containers.get(0).getContainer();
			}
			multiplier += 0.05;
			System.out.println("Try " + multiplier);
		}
	}

	private static List<StackableItem> getStackableItems3D(List<Item> items) {
		List<StackableItem> products = new ArrayList<>();
		for (Item item : items) {
			products.add(new StackableItem(Box.newBuilder().withDescription(item.toString()).withSize(item.getDx(), item.getDy(), item.getDz()).withRotate3D().withWeight(1).build(), item.getCount()));
		}

		return products;
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		pool1.shutdown();
		pool2.shutdown();

		try {
			Thread.sleep(500);
		} catch (Exception e) {
			// ignore
		}
	}

	public List<BenchmarkSet> getBruteForcePackager() {
		return bruteForcePackager;
	}

	public List<BenchmarkSet> getBruteForcePackagerNth() {
		return bruteForcePackagerNth;
	}

	public List<BenchmarkSet> getParallelBruteForcePackager() {
		return parallelBruteForcePackager;
	}

	public List<BenchmarkSet> getParallelBruteForcePackagerNth() {
		return parallelBruteForcePackagerNth;
	}

	public List<BenchmarkSet> getPlainPackager() {
		return plainPackager;
	}

	public List<BenchmarkSet> getPlainPackagerNth() {
		return plainPackagerNth;
	}

	public List<BenchmarkSet> getFastBruteForcePackager() {
		return fastBruteForcePackager;
	}

}
