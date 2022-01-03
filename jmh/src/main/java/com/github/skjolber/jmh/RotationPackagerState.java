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
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.BouwkampCodes;

@State(Scope.Benchmark)
public class RotationPackagerState {

	private int threadPoolSize = 8;
	
	private ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
	
	private ParallelBruteForcePackager parallelBruteForcePackager;
	private ParallelBruteForcePackager parallelBruteForcePackagerNth;
	
	private BruteForcePackager bruteForcePackager;
	private BruteForcePackager bruteForcePackagerNth;
	
	private List<StackableItem> items;
	
	@Setup(Level.Trial)
	public void init() {

		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> list = directory.getSimplePerfectSquaredRectangles(p -> p.contains("o10spsr.bkp"));

		BouwkampCode code = null; 
		for(BouwkampCodes codes : list) {
			code = codes.findCode("115x94A");
			if(code != null) {
				break;
			}
		}
		if(code == null) {
			throw new IllegalArgumentException();
		}
		
		List<Container> containers = new ArrayList<>();
		containers.add(BouwkampConverter.getContainer3D(code));

		parallelBruteForcePackagerNth = ParallelBruteForcePackager.newBuilder().withExecutorService(Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory())).withCheckpointsPerDeadlineCheck(1024).withContainers(containers).build();
		parallelBruteForcePackager = ParallelBruteForcePackager.newBuilder().withExecutorService(Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory())).withParallelizationCount(256).withContainers(containers).build();

		bruteForcePackagerNth = BruteForcePackager.newBuilder().withCheckpointsPerDeadlineCheck(1024).withContainers(containers).build();
		bruteForcePackager = BruteForcePackager.newBuilder().withContainers(containers).build();
		
		// verify that will not be able to package successful
		if(parallelBruteForcePackager.pack(items) != null) {
			throw new RuntimeException();
		}
		// verify that will not be able to package successful
		if(bruteForcePackager.pack(items) != null) {
			throw new IllegalArgumentException();
		}
		
		this.items = BouwkampConverter.getStackableItems3D(code);
	}
	
	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		parallelBruteForcePackager.shutdown();
		parallelBruteForcePackagerNth.shutdown();
		
		Thread.sleep(500);
	}
	
	public ParallelBruteForcePackager getParallelBruteForcePackager() {
		return parallelBruteForcePackager;
	}
	
	public ParallelBruteForcePackager getParallelBruteForcePackagerNth() {
		return parallelBruteForcePackagerNth;
	}	

	public BruteForcePackager getBruteForcePackager() {
		return bruteForcePackager;
	}
	
	public BruteForcePackager getBruteForcePackagerNth() {
		return bruteForcePackagerNth;
	}
	
	public List<StackableItem> getIdentialProducts() {
		return items;
	}
}
