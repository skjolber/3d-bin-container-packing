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

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.BruteForcePackagerBuilder;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

@State(Scope.Benchmark)
public class PermutationPackagerState {

	private final int threadPoolSize;
	private final int nth;
	
	private ExecutorService pool;
	
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
		
		this.pool = Executors.newFixedThreadPool(threadPoolSize);
	}
	
	@Setup(Level.Trial)
	public void init() {
		// these does not really result in successful stacking, but still should run as expected
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();
		
		List<BouwkampCodes> codesForCount = directory.codesForCount(10);
		for(BouwkampCodes c : codesForCount) {
			for(BouwkampCode bkpLine : c.getCodes()) {
				List<Container> containers = new ArrayList<>();
				containers.add(toContainer(bkpLine));
		
				List<BoxItem> products = new ArrayList<>();
				
				List<Box> boxes = toBoxes(bkpLine);
				for(Box box : boxes) {
					products.add(new BoxItem(box, 1));
				}

				// single-threaded
				BruteForcePackagerBuilder singleThreadBuilder = BruteForcePackager.newBuilder();
				singleThreadBuilder.withContainers(containers);
				
				bruteForcePackager.add(new BenchmarkSet(singleThreadBuilder, products));
				
				singleThreadBuilder.withCheckpointsPerDeadlineCheck(nth);
				bruteForcePackagerNth.add(new BenchmarkSet(singleThreadBuilder, products));

				// multi-threaded
				BruteForcePackagerBuilder multiThreadBuilder = BruteForcePackager.newBuilder().withContainers(containers);

				multiThreadBuilder.withCheckpointsPerDeadlineCheck(1);
				multiThreadBuilder.withThreads(threadPoolSize);
				multiThreadBuilder.withExecutorService(pool);
				
				parallelBruteForcePackager.add(new BenchmarkSet(multiThreadBuilder, products));
				
				multiThreadBuilder.withCheckpointsPerDeadlineCheck(nth);
				parallelBruteForcePackagerNth.add(new BenchmarkSet(multiThreadBuilder, products));
				
				break;
			}
		}
	}

	
	public Container toContainer(BouwkampCode code) {
		return new Container(code.getWidth(), code.getDepth(), 2 * Math.max(code.getDepth(), code.getWidth()), 0);
	}
	
	public List<Box> toBoxes(BouwkampCode code) {
		List<Box> boxes = new ArrayList<>();
		
		for (Integer integer : code.getLines()) {
			boxes.add(new Box(integer, integer, 2 * Math.max(code.getDepth(), code.getWidth()), 0));
		}
		
		return boxes;
	}
	
	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		pool.shutdown();
		
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
