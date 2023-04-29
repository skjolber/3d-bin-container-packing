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

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.ClonablePackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.ParallelPermutationRotationIteratorList;
import com.github.skjolber.packing.iterator.ParallelPermutationRotationIteratorListBuilder;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;
import com.github.skjolber.packing.packer.PackagerAdapter;
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

	public static class ParallelBruteForcePackagerBuilder extends AbstractPackagerBuilder<ParallelBruteForcePackager, ParallelBruteForcePackagerBuilder> {

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
		 * @param parallelizationCount number of pieces to split the workload into
		 * @return this builder
		 */

		public ParallelBruteForcePackagerBuilder withParallelizationCount(int parallelizationCount) {
			if(parallelizationCount < 1) {
				throw new IllegalArgumentException("Unexpected parallelization count " + parallelizationCount);
			}
			this.parallelizationCount = parallelizationCount;
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
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
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
						throw new ParallelBruteForcePackagerException("Expected a parallelization count for custom exectutor service");
					}
				}
			}

			return new ParallelBruteForcePackager(executorService, parallelizationCount, checkpointsPerDeadlineCheck, packResultComparator);
		}
	}

	private final ExecutorCompletionService<BruteForcePackagerResult> executorCompletionService;
	private final int parallelizationCount;
	private final ExecutorService executorService;

	public ParallelBruteForcePackager(ExecutorService executorService, int parallelizationCount, int checkpointsPerDeadlineCheck,
			PackResultComparator packResultComparator) {
		super(checkpointsPerDeadlineCheck, packResultComparator);

		this.parallelizationCount = parallelizationCount;
		this.executorService = executorService;
		this.executorCompletionService = new ExecutorCompletionService<BruteForcePackagerResult>(executorService);
	}

	private class RunnableAdapter implements Callable<BruteForcePackagerResult> {

		private Container container;
		private ContainerStackValue containerStackValue;
		private PermutationRotationIterator iterator;
		private List<StackPlacement> placements;
		private ExtremePoints3DStack extremePoints3D;
		private PackagerInterruptSupplier interrupt;
		private int containerIndex;

		public RunnableAdapter(int placementsCount, int maxIteratorLength, long minStackableItemVolume, long minStackableArea) {
			this.placements = getPlacements(placementsCount);

			this.extremePoints3D = new ExtremePoints3DStack(1, 1, 1, maxIteratorLength + 1);
		}

		public void setContainer(Container container) {
			this.container = container;
		}

		public void setIterator(PermutationRotationIterator iterator) {
			this.iterator = iterator;
		}

		public void setContainerStackValue(ContainerStackValue containerStackValue) {
			this.containerStackValue = containerStackValue;
		}

		public void setInterrupt(PackagerInterruptSupplier interrupt) {
			this.interrupt = interrupt;
		}

		@Override
		public BruteForcePackagerResult call() {
			return ParallelBruteForcePackager.this.pack(extremePoints3D, placements, container, containerIndex, containerStackValue, iterator, interrupt);
		}
	}

	private class ParallelAdapter extends AbstractBruteForcePackagerAdapter {

		private final ContainerStackValue[] containerStackValues;
		private final RunnableAdapter[] runnables; // per thread

		private final ParallelPermutationRotationIteratorList[] parallelIterators; // per container
		private final DefaultPermutationRotationIterator[] iterators; // per container
		private final PackagerInterruptSupplier[] interrupts;

		protected ParallelAdapter(List<StackableItem> stackableItems, List<ContainerItem> containerItems, ContainerStackValue[] containerStackValues, RunnableAdapter[] runnables, DefaultPermutationRotationIterator[] iterators, ParallelPermutationRotationIteratorList[] parallelIterators, PackagerInterruptSupplier[] interrupts) {
			super(containerItems, stackableItems);

			this.containerStackValues = containerStackValues;
			this.runnables = runnables;
			this.parallelIterators = parallelIterators;
			this.iterators = iterators;
			this.interrupts = interrupts;
		}

		@Override
		public BruteForcePackagerResult attempt(int i, BruteForcePackagerResult currentBest) {
			// is there enough work to do parallelization?
			// run on single thread for a small amount of combinations

			// the algorithm only splits on permutations
			boolean multithreaded;
			if(iterators[i].countPermutations() > parallelizationCount * 2) {
				multithreaded = iterators[i].countRotations() > 1;
			} else {
				multithreaded = false;
			}
			
			if(multithreaded) {

				// interrupt needs not be accurate (i.e. atomic boolean)
				Boolean[] localInterrupt = new Boolean[32]; // add padding to avoid false sharing

				List<Future<BruteForcePackagerResult>> futures = new ArrayList<>(runnables.length);
				for (int j = 0; j < runnables.length; j++) {
					RunnableAdapter runnableAdapter = runnables[j];
					runnableAdapter.setContainer(containerItems.get(i).getContainer());
					runnableAdapter.setContainerStackValue(containerStackValues[i]);

					runnableAdapter.setIterator(parallelIterators[i].getIterator(j));

					PackagerInterruptSupplier interruptBooleanSupplier = interrupts[i];

					PackagerInterruptSupplier booleanSupplier = () -> localInterrupt[15] != null || interruptBooleanSupplier.getAsBoolean();

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
								if(best == null || packResultComparator.compare(best, result) == PackResultComparator.ARGUMENT_2_IS_BETTER) {
									best = result;

									if(best.containsLastStackable()) { // will not match any better than this
										// cancel others
										localInterrupt[15] = Boolean.TRUE;
										// don't break, so we're waiting for all the remaining threads to finish
									}
								}
							}

						} catch (InterruptedException e1) {
							// ignore
							localInterrupt[15] = Boolean.TRUE;
							return null;
						} catch (Exception e) {
							localInterrupt[15] = Boolean.TRUE;
							throw new PackagerException(e);
						}
					}
					// was the search interrupted?
					if(interrupts[i].getAsBoolean()) {
						return null;
					}
					return best;
				} finally {
					for (Future<BruteForcePackagerResult> future : futures) {
						future.cancel(true);
					}
				}
			}
			// no need to split this job
			// run with linear approach
			return ParallelBruteForcePackager.this.pack(runnables[0].extremePoints3D, runnables[0].placements, containerItems.get(i).getContainer(), i, containerStackValues[i], iterators[i],
					interrupts[i]);
		}

		@Override
		public Container accept(BruteForcePackagerResult bruteForceResult) {
			super.accept(bruteForceResult.getContainerItemIndex());

			Container container = bruteForceResult.getContainer();

			if(!bruteForceResult.containsLastStackable()) {
				// this result does not consume all placements
				// remove consumed items from the iterators

				int size = container.getStack().getSize();

				PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();

				int[] permutations = state.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}

				for (ParallelPermutationRotationIteratorList it : parallelIterators) {
					it.removePermutations(p);
				}

				for (DefaultPermutationRotationIterator it : iterators) {
					it.removePermutations(p);
				}
				
				// remove adapter inventory
				removeInventory(p);

				for (RunnableAdapter runner : runnables) {
					runner.placements = runner.placements.subList(size, runner.placements.size());
				}
			} else {
				for (RunnableAdapter runner : runnables) {
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
	protected PackagerAdapter<BruteForcePackagerResult> adapter(List<StackableItem> stackableItems, List<ContainerItem> containerItems, PackagerInterruptSupplier interrupt) {
		ContainerStackValue[] containerStackValues = new ContainerStackValue[containerItems.size()];

		ParallelPermutationRotationIteratorList[] parallelIterators = new ParallelPermutationRotationIteratorList[containerItems.size()];
		DefaultPermutationRotationIterator[] iterators = new DefaultPermutationRotationIterator[containerItems.size()];
		for (int i = 0; i < containerItems.size(); i++) {
			Container container = containerItems.get(i).getContainer();
			ContainerStackValue stackValue = container.getStackValues()[0];

			containerStackValues[i] = stackValue;

			StackConstraint constraint = stackValue.getConstraint();

			Dimension dimension = new Dimension(stackValue.getLoadDx(), stackValue.getLoadDy(), stackValue.getLoadDz());

			parallelIterators[i] = new ParallelPermutationRotationIteratorListBuilder()
					.withLoadSize(dimension)
					.withStackableItems(stackableItems)
					.withMaxLoadWeight(stackValue.getMaxLoadWeight())
					.withFilter(stackable -> constraint == null || constraint.canAccept(stackable))
					.withParallelizationCount(parallelizationCount)
					.build();

			iterators[i] = DefaultPermutationRotationIterator
					.newBuilder()
					.withLoadSize(dimension)
					.withStackableItems(stackableItems)
					.withMaxLoadWeight(stackValue.getMaxLoadWeight())
					.withFilter(stackable -> constraint == null || constraint.canAccept(stackable))
					.build();
		}

		// check that all boxes fit in one or more container(s)
		// otherwise do not attempt packaging
		if(!AbstractBruteForcePackagerAdapter.hasAtLeastOneContainerForEveryStackable(iterators, stackableItems.size())) {
			return null;
		}
		
		int maxIteratorLength = 0;
		for (DefaultPermutationRotationIterator iterator : iterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		
		int count = 0;
		for (StackableItem stackable : stackableItems) {
			count += stackable.getCount();
		}

		long minStackableItemVolume = getMinStackableItemVolume(stackableItems);
		long minStackableArea = getMinStackableItemArea(stackableItems);

		RunnableAdapter[] runnables = new RunnableAdapter[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {
			runnables[i] = new RunnableAdapter(count, maxIteratorLength, minStackableItemVolume, minStackableArea);
		}
		
		PackagerInterruptSupplier[] interrupts = new PackagerInterruptSupplier[parallelizationCount];

		// clone nth interrupts so that everything is not slowed down by sharing a single counter
		if(interrupt instanceof ClonablePackagerInterruptSupplier) {
			ClonablePackagerInterruptSupplier c = (ClonablePackagerInterruptSupplier)interrupt;
			for (int i = 0; i < parallelizationCount; i++) {
				interrupts[i] = (PackagerInterruptSupplier)c.clone();
			}
		} else {
			for (int i = 0; i < parallelizationCount; i++) {
				interrupts[i] = interrupt;
			}
		}
		
		return new ParallelAdapter(stackableItems, containerItems, containerStackValues, runnables, iterators, parallelIterators, interrupts);
	}

}
