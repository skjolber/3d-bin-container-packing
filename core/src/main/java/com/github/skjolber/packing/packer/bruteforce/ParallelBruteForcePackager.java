package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.ClonableBooleanSupplier;
import com.github.skjolber.packing.iterator.ParallelPermutationRotationIterator;
import com.github.skjolber.packing.iterator.ParallelPermutationRotationIteratorAdapter;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.packer.PackagerException;

/**
 * 
 * Note on parallelization: The permutations are split into different tasks. Rotations + point placements is not.
 *
 */

public class ParallelBruteForcePackager extends AbstractBruteForcePackager {

	public static ParallelBruteForcePackagerBuilder newBuilder() {
		return new ParallelBruteForcePackagerBuilder();
	}

	public static class ParallelBruteForcePackagerBuilder {

		protected List<Container> containers;
		protected int checkpointsPerDeadlineCheck = 1;
		protected int threads = -1;
		protected int parallelizationCount = -1;
		protected ExecutorService executorService;

		public ParallelBruteForcePackagerBuilder withThreads(int threads) {
			if(threads < 1) {
				throw new IllegalArgumentException("Unexpected thread count " + threads);
			}
			this.threads = threads;
			return this;
		}
		
		/**
		 * 
		 * Number of units to split the work into. This number should by an order of magnitude larger than the threads.
		 * 
		 * @param parallelizationCount
		 * @return this builder
		 */
		
		public ParallelBruteForcePackagerBuilder withParallelizationCount(int parallelizationCount) {
			if(parallelizationCount < 1) {
				throw new IllegalArgumentException("Unexpected parallelization count " + parallelizationCount);
			}
			this.parallelizationCount = parallelizationCount;
			return this;
		}

		public ParallelBruteForcePackagerBuilder withContainers(List<Container> containers) {
			this.containers = containers;
			return this;
		}

		public ParallelBruteForcePackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
			this.checkpointsPerDeadlineCheck = n;
			return this;
		}
		
		public ParallelBruteForcePackagerBuilder withExecutorService(ExecutorService executorService) {
			this.executorService = executorService;
			
			return this;
		}

		public ParallelBruteForcePackagerBuilder withAvailableProcessors(int factor) {
			this.threads = Runtime.getRuntime().availableProcessors() / factor;
			
			return this;
		}

		public ParallelBruteForcePackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			if(executorService == null) {
				if(threads == -1) {
					threads = Runtime.getRuntime().availableProcessors();
				}
				if(executorService == null) {
					executorService = Executors.newFixedThreadPool(threads);
				}
				if(parallelizationCount == -1) {
					parallelizationCount = 16 * threads;
				}
			} else {
				if(threads != -1) {
					throw new IllegalArgumentException("Not expection both thread count and executor service");
				}
				if(parallelizationCount == -1) {
					// auto detect
					if(executorService instanceof ThreadPoolExecutor) {
						ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)executorService;
						parallelizationCount = 16 * threadPoolExecutor.getMaximumPoolSize();
					} else {
						throw new IllegalArgumentException("Expected a parallelization count for custom exectutor service");
					}
				}
			}
			
			return new ParallelBruteForcePackager(containers, executorService, parallelizationCount, checkpointsPerDeadlineCheck);
		}
	}
	
	private final ExecutorCompletionService<BruteForcePackagerResult> executorCompletionService;
	private final int parallelizationCount;
	private final ExecutorService executorService;

	public ParallelBruteForcePackager(List<Container> containers, ExecutorService executorService, int parallelizationCount, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
		
		this.parallelizationCount = parallelizationCount;
		this.executorService = executorService;
		this.executorCompletionService = new ExecutorCompletionService<BruteForcePackagerResult>(executorService);
	}

	private class RunnableAdapter implements Callable<BruteForcePackagerResult> {

		private Container container;
		private PermutationRotationIterator iterator;
		private List<StackPlacement> placements;
		private ExtremePoints3DStack extremePoints3D;
		private BooleanSupplier interrupt;

		public RunnableAdapter(int placementsCount) {
			this.placements = getPlacements(placementsCount);
			
			extremePoints3D = new ExtremePoints3DStack(1, 1, 1, placementsCount + 1);
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
		public BruteForcePackagerResult call() {
			//System.out.println("Start work " + Thread.currentThread().getName());
			try {
				return ParallelBruteForcePackager.this.pack(extremePoints3D, placements, container, iterator, interrupt);
			} finally {
				//System.out.println("End work " + Thread.currentThread().getName());
			}
		}
	}
	
	private class ParallelAdapter implements Adapter<BruteForcePackagerResult> {
		
		private List<Container> containers;
		private ParallelPermutationRotationIterator[] iterators; // per container
		private RunnableAdapter[] runnables; // per thread
		private BooleanSupplier[] interrupts;

		protected ParallelAdapter(List<StackableItem> stackables, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			this.interrupts = new BooleanSupplier[parallelizationCount];

			// clone nth interrupts so that everything is not slowed down by sharing a single counter
			if(interrupt instanceof ClonableBooleanSupplier) {
				ClonableBooleanSupplier c = (ClonableBooleanSupplier)interrupt;
				for(int i = 0; i < parallelizationCount; i++) {
					this.interrupts[i] = (BooleanSupplier) c.clone();
				}
			} else {
				for(int i = 0; i < parallelizationCount; i++) {
					this.interrupts[i] = interrupt;
				}
			}

			int count = 0;
			for (StackableItem stackable : stackables) {
				count += stackable.getCount();
			}

			runnables = new RunnableAdapter[parallelizationCount];
			for(int i = 0; i < parallelizationCount; i++) {
				runnables[i] = new RunnableAdapter(count);
			}

			iterators = new ParallelPermutationRotationIterator[containers.size()];
			for (int i = 0; i < containers.size(); i++) {
				Container container = containers.get(i);
				ContainerStackValue[] stackValues = container.getStackValues();
				
				iterators[i] = new ParallelPermutationRotationIterator(new Dimension(stackValues[0].getLoadDx(), stackValues[0].getLoadDy(), stackValues[0].getLoadDz()), stackables, parallelizationCount);
			}
		}
		
		@Override
		public BruteForcePackagerResult attempt(int i, BruteForcePackagerResult currentBest) {
			// run on single thread for a small amount of combinations
			ParallelPermutationRotationIterator parallelPermutationRotationIterator = iterators[i];
			if(parallelPermutationRotationIterator.countPermutations() * parallelPermutationRotationIterator.countRotations() > parallelizationCount * 2) { // somewhat conservative, as the number of rotations is unknown 
				AtomicBoolean localInterrupt = new AtomicBoolean();

				List<Future<BruteForcePackagerResult>> futures = new ArrayList<>(runnables.length);
				for (int j = 0; j < runnables.length; j++) {
					RunnableAdapter runnableAdapter = runnables[j];
					runnableAdapter.setContainer(containers.get(i));
					runnableAdapter.setIterator(new ParallelPermutationRotationIteratorAdapter(iterators[i], j));
					
					BooleanSupplier interruptBooleanSupplier = interrupts[i];
					BooleanSupplier booleanSupplier = () -> localInterrupt.get() || interruptBooleanSupplier.getAsBoolean();
					
					runnableAdapter.setInterrupt(booleanSupplier);
					
					futures.add(executorCompletionService.submit(runnableAdapter));
				}
				
				try {
					BruteForcePackagerResult best = null;
					for (int j = 0; j < runnables.length; j++) {
						try {
							Future<BruteForcePackagerResult> future = executorCompletionService.take();
							BruteForcePackagerResult result = future.get();
							if(result != null) {
								if (best == null || result.isBetterThan(best)) {
									best = result;
				
									if (best.containsLastStackable()) { // will not match any better than this
										// cancel others
										localInterrupt.set(true);
										// don't break, so we're waiting for all the remaining threads to finish
									}
								}
							}
							
						} catch (InterruptedException e1) {
							// ignore
							localInterrupt.set(true);
							return null;
					    } catch (Exception e) {
							localInterrupt.set(true);
					    	throw new PackagerException(e);
					    }
					}
					// was the search interrupted?
					if(interrupts[i].getAsBoolean()) {
						return null;
					}
					return best;
				} finally {
					for(Future<BruteForcePackagerResult> future : futures) {
						future.cancel(true);
					}
				}
			}
			// no need to split this job
			// run with linear approach
			return ParallelBruteForcePackager.this.pack(runnables[0].extremePoints3D, runnables[0].placements, containers.get(i), parallelPermutationRotationIterator, interrupts[i]);
		}
		
		@Override
		public Container accept(BruteForcePackagerResult bruteForceResult) {
			Container container = bruteForceResult.getContainer();

			if (!bruteForceResult.containsLastStackable()) {
				// this result does not consume all placements
				// remove consumed items from the iterators
				
				int size = container.getStack().getSize();

				PermutationRotationIterator iterator = bruteForceResult.getPermutationRotationIteratorForState();
				
				int[] permutations = iterator.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}
				
				for (PermutationRotationIterator it : iterators) {
					if (it == bruteForceResult.getPermutationRotationIteratorForState()) {
						it.removePermutations(size);
					} else {
						it.removePermutations(p);
					}
				}
				for(RunnableAdapter runner : runnables) {
					runner.placements = runner.placements.subList(size, runner.placements.size());
				}
			} else {
				for(RunnableAdapter runner : runnables) {
					runner.placements = Collections.emptyList();
				}
			}
			return container;
		}
	
	}
	
	public void shutdown() {
		executorService.shutdownNow();
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}
	
	@Override
	protected Adapter<BruteForcePackagerResult> adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new ParallelAdapter(boxes, containers, interrupt);
	}
	
}
