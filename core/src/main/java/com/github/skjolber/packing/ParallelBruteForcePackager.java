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
import com.github.skjolber.packing.impl.deadline.ClonableBooleanSupplier;

public class ParallelBruteForcePackager extends BruteForcePackager {

	private final ExecutorCompletionService<PackResult> executorCompletionService;
	private final int threads;
	private final ExecutorService executorService;
	
	public ParallelBruteForcePackager(List<Container> containers, int threads, int checkpointsPerDeadlineCheck) {
		this(containers, Executors.newFixedThreadPool(threads), threads, true, true, checkpointsPerDeadlineCheck);
	}
	
	public ParallelBruteForcePackager(List<Container> containers, int threads, boolean rotate3D, boolean binarySearch, int checkpointsPerDeadlineCheck) {
		this(containers, Executors.newFixedThreadPool(threads), threads, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
	}
	
	public ParallelBruteForcePackager(List<Container> containers, ExecutorService executorService, int threads, boolean rotate3D, boolean binarySearch, int checkpointsPerDeadlineCheck) {
		super(containers, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
		
		this.threads = threads;
		this.executorService = executorService;
		this.executorCompletionService = new ExecutorCompletionService<PackResult>(executorService);
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
		private BooleanSupplier[] interrupts;

		protected ParallelAdapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			this.interrupts = new BooleanSupplier[threads];

			// clone nth interrupts so that everything is not slowed down by sharing a single counter
			if(interrupt instanceof ClonableBooleanSupplier) {
				ClonableBooleanSupplier c = (ClonableBooleanSupplier)interrupt;
				for(int i = 0; i < threads; i++) {
					this.interrupts[i] = (BooleanSupplier) c.clone();
				}
			} else {
				for(int i = 0; i < threads; i++) {
					this.interrupts[i] = interrupt;
				}
			}

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

				for (int j = 0; j < runnables.length; j++) {
					RunnableAdapter runnableAdapter = runnables[j];
					runnableAdapter.setContainer(containers.get(i));
					runnableAdapter.setIterator(new ParallelPermutationRotationIteratorAdapter(iterators[i], j));
					
					BooleanSupplier interruptBooleanSupplier = interrupts[i];
					BooleanSupplier booleanSupplier = () -> localInterrupt.get() || interruptBooleanSupplier.getAsBoolean();
					
					runnableAdapter.setInterrupt(booleanSupplier);
					
					executorCompletionService.submit(runnableAdapter);
				}
	
				PackResult best = null;
				for (int j = 0; j < runnables.length; j++) {
					try {
						Future<PackResult> future = executorCompletionService.take();
						PackResult result = future.get();
						if(result != null) {
							if (best == null || result.packsMoreBoxesThan(best)) {
								best = result;
			
								if (!hasMore(best)) { // will not match any better than this
									// cancel others
									localInterrupt.set(true);
									// don't break, so we're waiting for all the remaining threads to finish
								}
							}
						}
					} catch (InterruptedException e1) {
						// ignore
						return null;
				    } catch (Exception e) {
				    	throw new PackagerException(e);
				    }
				}
				
				if(interrupts[i].getAsBoolean()) {
					return null;
				}
				return best;
			}
			// run with linear approach
			return ParallelBruteForcePackager.this.pack(runnables[0].placements, containers.get(i), parallelPermutationRotationIterator, interrupts[i]);
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
	
	public void shutdown() {
		executorService.shutdown();
	}
	
	public ExecutorService getExecutorService() {
		return executorService;
	}
	
	@Override
	protected Adapter adapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach
		return new ParallelAdapter(boxes, containers, interrupt);
	}
	
}
