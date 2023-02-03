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
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

/**
 * 
 * This benchmark is heavy on permutations (not rotations)
 * 
 */

@State(Scope.Benchmark)
public class BouwkampCodePackagerState {

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

	public BouwkampCodePackagerState() {
		this(8, 20000);
	}

	public BouwkampCodePackagerState(int threadPoolSize, int nth) {
		this.threadPoolSize = threadPoolSize;
		this.nth = nth;

		this.pool1 = Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory());
		this.pool2 = Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory());
	}

	@Setup(Level.Trial)
	public void init() {
		// these does not really result in successful stacking, but still should run as expected
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();
		List<BouwkampCodes> codesForCount = directory.codesForCount(9);
		for (BouwkampCodes c : codesForCount) {
			for (BouwkampCode bkpLine : c.getCodes()) {

				/*
				if(!c.getSource().equals("/simpleImperfectSquaredRectangles/o9sisr.bkp") || !bkpLine.getName().equals("15x11A")) {
					continue;
				}
				*/
				List<Container> containers = new ArrayList<>();
				containers.add(BouwkampConverter.getContainer3D(bkpLine));

				List<StackableItem> stackableItems3D = BouwkampConverter.getStackableItems3D(bkpLine);

				ParallelBruteForcePackager parallelPackager = ParallelBruteForcePackager.newBuilder().withExecutorService(pool2).withParallelizationCount(threadPoolSize * 16)
						.withContainers(containers).build();
				ParallelBruteForcePackager parallelPackagerNth = ParallelBruteForcePackager.newBuilder().withExecutorService(pool1).withParallelizationCount(threadPoolSize * 16)
						.withCheckpointsPerDeadlineCheck(nth).withContainers(containers).build();

				BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(containers).build();
				BruteForcePackager packagerNth = BruteForcePackager.newBuilder().withCheckpointsPerDeadlineCheck(nth).withContainers(containers).build();

				PlainPackager plainPackager = PlainPackager.newBuilder().withContainers(containers).build();
				PlainPackager plainPackagerNth = PlainPackager.newBuilder().withCheckpointsPerDeadlineCheck(nth).withContainers(containers).build();

				FastBruteForcePackager fastPackager = FastBruteForcePackager.newBuilder().withContainers(containers).build();

				// single-threaded
				this.bruteForcePackager.add(new BenchmarkSet(packager, stackableItems3D));
				this.bruteForcePackagerNth.add(new BenchmarkSet(packagerNth, stackableItems3D));

				this.plainPackager.add(new BenchmarkSet(plainPackager, stackableItems3D));
				this.plainPackagerNth.add(new BenchmarkSet(plainPackagerNth, stackableItems3D));

				this.fastBruteForcePackager.add(new BenchmarkSet(fastPackager, stackableItems3D));

				// multi-threaded
				this.parallelBruteForcePackager.add(new BenchmarkSet(parallelPackager, stackableItems3D));
				this.parallelBruteForcePackagerNth.add(new BenchmarkSet(parallelPackagerNth, stackableItems3D));
			}
		}
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
