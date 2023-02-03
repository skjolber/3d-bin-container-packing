package com.github.skjolber.packing.jmh.iterator;

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
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.ParallelPermutationRotationIteratorList;
import com.github.skjolber.packing.iterator.ParallelPermutationRotationIteratorListBuilder;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.test.generator.Item;
import com.github.skjolber.packing.test.generator.ItemIO;

/**
 * 
 * This benchmark is heavy on permutations (not rotations)
 * 
 */

@State(Scope.Benchmark)
public class IteratorState {

	private final int threadPoolSize;
	private ExecutorService pool1;

	private ParallelPermutationRotationIteratorList parallelIterator;
	private DefaultPermutationRotationIterator iterator;

	public IteratorState() {
		this(16);
	}

	public IteratorState(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;

		this.pool1 = Executors.newFixedThreadPool(threadPoolSize, new DefaultThreadFactory());
	}

	@Setup(Level.Trial)
	public void init() throws IOException {

		Path path = Paths.get("src", "main", "resources", "iterate.json");

		if(!Files.exists(path)) {
			path = Paths.get("jmh", "src", "main", "resources", "iterate.json");
		}

		List<StackableItem> stackableItems3D = getStackableItems3D(ItemIO.read(path));

		int x = 0;
		int y = 0;
		int z = 0;

		int weight = 0;
		for (StackableItem stackableItem : stackableItems3D) {
			StackValue[] stackValues = stackableItem.getStackable().getStackValues();
			for (StackValue stackValue : stackValues) {
				if(x < stackValue.getDx()) {
					x = stackValue.getDx();
				}
				if(y < stackValue.getDy()) {
					y = stackValue.getDy();
				}
				if(z < stackValue.getDz()) {
					z = stackValue.getDz();
				}
			}
			weight += stackableItem.getCount() * stackableItem.getStackable().getWeight();
		}

		this.parallelIterator = new ParallelPermutationRotationIteratorListBuilder()
				.withStackableItems(stackableItems3D)
				.withLoadSize(new Dimension(x, y, z))
				.withParallelizationCount(threadPoolSize)
				.withMaxLoadWeight(weight)
				.build();

		this.iterator = DefaultPermutationRotationIterator.newBuilder()
				.withStackableItems(stackableItems3D)
				.withLoadSize(new Dimension(x, y, z))
				.withMaxLoadWeight(weight)
				.build();
	}

	@TearDown(Level.Trial)
	public void shutdown() throws InterruptedException {
		pool1.shutdown();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			// ignore
		} catch (Exception e) {
			// ignore
		}
	}

	private static List<StackableItem> getStackableItems3D(List<Item> items) {
		List<StackableItem> products = new ArrayList<>();
		for (Item item : items) {
			products.add(new StackableItem(Box.newBuilder().withDescription(item.toString()).withSize(item.getDx(), item.getDy(), item.getDz()).withRotate3D().withWeight(1).build(), item.getCount()));
		}

		return products;
	}

	public ParallelPermutationRotationIteratorList getParallelIterator() {
		return parallelIterator;
	}

	public ExecutorService getPool() {
		return pool1;
	}

	public DefaultPermutationRotationIterator getIterator() {
		return iterator;
	}

}
