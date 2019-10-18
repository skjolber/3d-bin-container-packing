package com.github.skjolber.jmh;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.BruteForcePackager;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.ParallelBruteForcePackager;

@State(Scope.Benchmark)
public class PackagerState {

	private int count = 4;
	private int n = 2;
	
	private ExecutorService pool = Executors.newFixedThreadPool(count);
	private BruteForcePackager bruteForcePackager;
	private List<BoxItem> products;
	private List<Container> containers;
	
	@Setup(Level.Trial)
	public void init() {
		containers = new ArrayList<>();
		containers.add(new Container(5 * n, 10, 10, 0));
		
		bruteForcePackager = new ParallelBruteForcePackager(containers, pool, count);

		products = new ArrayList<>();
		for(int i = 0; i < n; i++) {
			Box box = new Box(Integer.toString(i), 5, 10, 10, 0);
			for(int k = 0; k < i % 2; k++) {
				box.rotate3D();
			}
			products.add(new BoxItem(box, 1));
		}
	}
	
	public BruteForcePackager getBruteForcePackager() {
		return bruteForcePackager;
	}

	public List<BoxItem> getProducts() {
		return products;
	}
}
