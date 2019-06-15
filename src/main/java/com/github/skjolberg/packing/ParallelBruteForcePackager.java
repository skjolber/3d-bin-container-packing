package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

import com.github.skjolberg.packing.impl.Adapter;
import com.github.skjolberg.packing.impl.BruteForceResult;
import com.github.skjolberg.packing.impl.PackResult;
import com.github.skjolberg.packing.impl.PermutationRotation;
import com.github.skjolberg.packing.impl.PermutationRotationIterator;

public class ParallelBruteForcePackager extends BruteForcePackager {

	private final ExecutorService executorService;
	private final int threads;

	public ParallelBruteForcePackager(List<Container> containers, int threads) {
		this(containers, Executors.newFixedThreadPool(threads), threads);
	}

	public ParallelBruteForcePackager(List<Container> containers, ExecutorService executorService, int threads) {
		super(containers);
		
		this.executorService = executorService;
		this.threads = threads;
	}

	@Override
	protected Adapter adapter(BooleanSupplier interrupt) {
		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach
		
		return new BruteForceAdapter()
	}

	@Override
	protected Adapter adapter(BooleanSupplier interrupt) {
		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach
		
		return new Adapter() {

			private List<Placement> placements;
			private PermutationRotationIterator[] iterators;
			private List<Container> containers;

			@Override
			public PackResult attempt(int i) {
				return ParallelBruteForcePackager.this.pack(placements, containers.get(i), iterators[i], interrupt);
			}

			@Override
			public void initialize(List<BoxItem> boxes, List<Container> containers) {
				this.containers = containers;
				PermutationRotation[] rotations = PermutationRotationIterator.toRotationMatrix(boxes, rotate3D);
				int count = 0;
				for (PermutationRotation permutationRotation : rotations) {
					count += permutationRotation.getCount();
				}

				placements = getPlacements(count);

				iterators = new PermutationRotationIterator[containers.size()];
				for (int i = 0; i < containers.size(); i++) {
					iterators[i] = new PermutationRotationIterator(containers.get(i), rotations);
				}
			}

			@Override
			public Container accepted(PackResult result) {
				BruteForceResult bruteForceResult = (BruteForceResult) result;

				Container container = bruteForceResult.getContainer();

				if (bruteForceResult.isRemainder()) {
					int[] permutations = bruteForceResult.getRotator().getPermutations();
					List<Integer> p = new ArrayList<>(bruteForceResult.getCount());
					for (int i = 0; i < bruteForceResult.getCount(); i++) {
						p.add(permutations[i]);
					}
					for (PermutationRotationIterator it : iterators) {
						if (it == bruteForceResult.getRotator()) {
							it.removePermutations(bruteForceResult.getCount());
						} else {
							it.removePermutations(p);
						}
					}
					placements = placements.subList(bruteForceResult.getCount(), this.placements.size());
				} else {
					placements = Collections.emptyList();
				}

				return container;
			}

			@Override
			public boolean hasMore(PackResult result) {
				BruteForceResult bruteForceResult = (BruteForceResult) result;
				return placements.size() > bruteForceResult.getCount();
			}

		};
	}
	
}
