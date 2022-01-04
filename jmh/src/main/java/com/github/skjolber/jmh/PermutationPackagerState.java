package com.github.skjolber.jmh;

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
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

@State(Scope.Benchmark)
public class PermutationPackagerState {

	private final int threadPoolSize;
	private final int nth;
	
	private ExecutorService pool1;
	private ExecutorService pool2;
	
	private List<BenchmarkSet> parallelBruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> parallelBruteForcePackagerNth = new ArrayList<>();
	private List<BenchmarkSet> bruteForcePackager = new ArrayList<>();
	private List<BenchmarkSet> bruteForcePackagerNth = new ArrayList<>();

	public PermutationPackagerState() {
		this(8, 20000);
	}

	public PermutationPackagerState(int threadPoolSize, int nth) {
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
		for(BouwkampCodes c : codesForCount) {
			for(BouwkampCode bkpLine : c.getCodes()) {
				
				if(!c.getSource().equals("/simpleImperfectSquaredRectangles/o9sisr.bkp") || !bkpLine.getName().equals("15x11A")) {
					continue;
				}

				List<Container> containers = new ArrayList<>();
				containers.add(BouwkampConverter.getContainer3D(bkpLine));
		
				List<StackableItem> stackableItems3D = BouwkampConverter.getStackableItems3D(bkpLine);
				
				ParallelBruteForcePackager parallelPackager = ParallelBruteForcePackager.newBuilder().withExecutorService(pool2).withParallelizationCount(256).withContainers(containers).build();
				ParallelBruteForcePackager parallelPackagerNth = ParallelBruteForcePackager.newBuilder().withExecutorService(pool1).withCheckpointsPerDeadlineCheck(nth).withContainers(containers).build();

				BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(containers).build();
				BruteForcePackager packagerNth = BruteForcePackager.newBuilder().withCheckpointsPerDeadlineCheck(nth).withContainers(containers).build();
				
				// single-threaded
				bruteForcePackager.add(new BenchmarkSet(packager, stackableItems3D));
				bruteForcePackagerNth.add(new BenchmarkSet(packagerNth, stackableItems3D));

				// multi-threaded
				parallelBruteForcePackager.add(new BenchmarkSet(parallelPackager, stackableItems3D));
				parallelBruteForcePackagerNth.add(new BenchmarkSet(parallelPackagerNth, stackableItems3D));
				
				break;
			}
		}
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		pool1.shutdown();
		pool2.shutdown();
		
		Thread.sleep(500);
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
}
