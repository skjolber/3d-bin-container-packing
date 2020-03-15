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
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.ParallelBruteForcePackager;

@State(Scope.Benchmark)
public class RotationPackagerState {

	private int threadPoolSize = 8;
	private int n = 8;
	private int boxesPerLevel = 4;
	
	private ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
	private ParallelBruteForcePackager parallelBruteForcePackager;
	private ParallelBruteForcePackager parallelBruteForcePackagerNth;
	
	private BruteForcePackager bruteForcePackager;
	private BruteForcePackager bruteForcePackagerNth;
	
	private List<BoxItem> identialProducts;
	
	@Setup(Level.Trial)
	public void init() {

		List<Container> containers = new ArrayList<>();
		int levels = n / boxesPerLevel + (n % boxesPerLevel > 0 ? 1 : 0);

		containers.add(new Container(2 * boxesPerLevel / 2, 1 * (boxesPerLevel / 2), 10 * levels, 0));

		// first levels will be easy to populate
		List<BoxItem> identialProducts = new ArrayList<>();
		for(int i = 0; i < n; i++) {
			Box box = new Box(Integer.toString(i), 1, 2, 10, 0);
			identialProducts.add(new BoxItem(box, 1));
		}
		
		int min = Integer.MAX_VALUE / 2;
		
		parallelBruteForcePackager = new MinimumAcceptableParallelBruteForcePackager(containers, pool, threadPoolSize, true, true, 1, min, false, true);
		parallelBruteForcePackagerNth = new MinimumAcceptableParallelBruteForcePackager(containers, pool, threadPoolSize, true, true, 200000, min, false, true);

		bruteForcePackager = new MinimumAcceptableBruteForcePackager(containers, true, true, 1, min, false, true);
		bruteForcePackagerNth = new MinimumAcceptableBruteForcePackager(containers, true, true, 1000, min, false, true);

		// verify that will not be able to package successful
		if(parallelBruteForcePackager.pack(identialProducts) != null) {
			throw new RuntimeException();
		}
		// verify that will not be able to package successful
		if(bruteForcePackager.pack(identialProducts) != null) {
			throw new IllegalArgumentException();
		}
		
		this.identialProducts = identialProducts;
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
	
	public List<BoxItem> getIdentialProducts() {
		return identialProducts;
	}
}
