package com.github.skjolber.packing.jmh;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * 
 * This benchmark is heavy on permutations (not rotations)
 * 
 */

@State(Scope.Benchmark)
public class TychoPackagerState {

	private final int threadPoolSize;

	private ExecutorService pool1;
	private ExecutorService pool2;

	private List<BenchmarkSet> parallelBruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> bruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> plainPackager = new ArrayList<>();
	private List<BenchmarkSet> fastBruteForcePackager = new ArrayList<>();

	private List<BoxItem> stackableItems3D;

	private List<ContainerItem> containers = ContainerItem.newListBuilder()
			.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(1500, 1900, 4000).withMaxLoadWeight(100).build()).build();

	public TychoPackagerState() {
		this(8);
	}

	public TychoPackagerState(int threadPoolSize) {
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
