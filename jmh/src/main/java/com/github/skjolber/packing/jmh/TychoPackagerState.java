package com.github.skjolber.packing.jmh;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackableItem;
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
public class TychoPackagerState {

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

	private List<StackableItem> stackableItems3D;

	private List<ContainerItem> containers = ContainerItem.newListBuilder()
			.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1500, 1900, 4000).withMaxLoadWeight(100).build()).build();

	public TychoPackagerState() {
		this(8, 20000);
	}

	public TychoPackagerState(int threadPoolSize, int nth) {
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
