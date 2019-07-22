package com.github.skjolber.packing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.impl.Adapter;
import com.github.skjolber.packing.impl.BruteForceResult;
import com.github.skjolber.packing.impl.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.impl.PackResult;
import com.github.skjolber.packing.impl.ParallelPermutationRotationIterator;
import com.github.skjolber.packing.impl.ParallelPermutationRotationIteratorAdapter;
import com.github.skjolber.packing.impl.PermutationRotation;
import com.github.skjolber.packing.impl.PermutationRotationIterator;

public class ParallelBruteForcePackager extends BruteForcePackager {

	private final ExecutorCompletionService<PackResult> executorService;
	private final int threads;
	
	public ParallelBruteForcePackager(List<Container> containers, int threads) {
		this(containers, Executors.newFixedThreadPool(threads), threads);
	}

	public ParallelBruteForcePackager(List<Container> containers, ExecutorService executorService, int threads) {
		super(containers);
		
		this.executorService = new ExecutorCompletionService<PackResult>(executorService);
		this.threads = threads;
	}

	private class RunnableAdapter implements Callable<PackResult> {

		private Container container;
		private PermutationRotationIterator iterator;
		private List<Placement> placements;

		private BooleanSupplier interrupt;

		public RunnableAdapter(int placementsCount) {
			this.placements = getPlacements(placementsCount);
		}

		public void setContainer(Container container) {
			this.container = container;
		}
		
		public void setIterator(PermutationRotationIterator iterator) {
			this.iterator = iterator;
		}
		
		public void setInterrupt(BooleanSupplier interrupt) {
			this.interrupt = interrupt;
		}
		
		@Override
		public PackResult call() {
			return ParallelBruteForcePackager.this.pack(placements, container, iterator, interrupt);
		}
	}
	
	private class ParallelAdapter implements Adapter {
		
		private List<Container> containers;
		private ParallelPermutationRotationIterator[] iterators; // per container
		private RunnableAdapter[] runnables; // per thread
		private BooleanSupplier interrupt;

		protected ParallelAdapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			this.interrupt = interrupt;

			PermutationRotation[] rotations = DefaultPermutationRotationIterator.toRotationMatrix(boxes, rotate3D);
			int count = 0;
			for (PermutationRotation permutationRotation : rotations) {
				count += permutationRotation.getCount();
			}

			runnables = new RunnableAdapter[threads];
			for(int i = 0; i < threads; i++) {
				runnables[i] = new RunnableAdapter(count);
			}

			iterators = new ParallelPermutationRotationIterator[containers.size()];
			for (int i = 0; i < containers.size(); i++) {
				iterators[i] = new ParallelPermutationRotationIterator(containers.get(i), rotations, threads);
			}
		}
		
		@Override
		public PackResult attempt(int i) {
			// run on single thread for a small amount of combinations
			ParallelPermutationRotationIterator parallelPermutationRotationIterator = iterators[i];
			if(parallelPermutationRotationIterator.countPermutations() * parallelPermutationRotationIterator.countRotations() > threads * 2) { // somewhat conservative, as the number of rotations is unknown 
				AtomicBoolean localInterrupt = new AtomicBoolean();
				BooleanSupplier booleanSupplier = () -> localInterrupt.get() || interrupt.getAsBoolean();
				
				for (int j = 0; j < runnables.length; j++) {
					RunnableAdapter runnableAdapter = runnables[j];
					runnableAdapter.setContainer(containers.get(i));
					runnableAdapter.setIterator(new ParallelPermutationRotationIteratorAdapter(iterators[i], j));
					runnableAdapter.setInterrupt(booleanSupplier);
					
					executorService.submit(runnableAdapter);
				}
	
				PackResult best = null;
				for (int j = 0; j < runnables.length; j++) {
					try {
						Future<PackResult> future = executorService.take();
						PackResult result = future.get();
						if(result != null) {
							if (best == null || result.packsMoreBoxesThan(best)) {
								best = result;
			
								if (!hasMore(best)) { // will not match any better than this
									// TODO cancel others
									localInterrupt.set(true);
									// don't break, so we're waiting for all the remaining threads to finish
								}
							}
						}
					} catch (InterruptedException e1) {
						// ignore
						return null;
				    } catch (Exception e) {
				    	throw new RuntimeException(e);
				    }
				}
				
				if(interrupt.getAsBoolean()) {
					return null;
				}
				return best;
			}
			// run with linear approach
			return ParallelBruteForcePackager.this.pack(runnables[0].placements, containers.get(i), parallelPermutationRotationIterator, interrupt);
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
				for(RunnableAdapter runner : runnables) {
					runner.placements = runner.placements.subList(bruteForceResult.getCount(), runner.placements.size());
				}
			} else {
				for(RunnableAdapter runner : runnables) {
					runner.placements = Collections.emptyList();
				}
			}

			return container;
		}

		@Override
		public boolean hasMore(PackResult result) {
			BruteForceResult bruteForceResult = (BruteForceResult) result;
			for(RunnableAdapter runner : runnables) {
				if(runner.placements.size() > bruteForceResult.getCount()) {
					return true;
				}
			}
			
			return false;
		}		
	}
	
	@Override
	protected Adapter adapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach
		return new ParallelAdapter(boxes, containers, interrupt);
	}
	
}
