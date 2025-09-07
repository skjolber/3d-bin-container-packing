package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.packager.PackResultComparator;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.ClonablePackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.ParallelBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.ParallelBoxItemPermutationRotationIteratorList;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.PackagerException;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

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
		protected IntermediatePackagerResultComparator comparator;
		
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
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator();
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
			return new ParallelBruteForcePackager(executorService, parallelizationCount, comparator);
		}
	}

	private final ExecutorCompletionService<BruteForceIntermediatePackagerResult> executorCompletionService;
	private final int parallelizationCount;
	private final ExecutorService executorService;

	public ParallelBruteForcePackager(ExecutorService executorService, int parallelizationCount, 
			IntermediatePackagerResultComparator packResultComparator) {
		super(packResultComparator);

		this.parallelizationCount = parallelizationCount;
		this.executorService = executorService;
		this.executorCompletionService = new ExecutorCompletionService<BruteForceIntermediatePackagerResult>(executorService);
	}

	private class RunnableAdapter implements Callable<BruteForceIntermediatePackagerResult> {

		private ContainerItem containerItem;
		private ParallelBoxItemPermutationRotationIterator iterator;
		private List<StackPlacement> placements;
		private ExtremePoints3DStack extremePoints3D;
		private PackagerInterruptSupplier interrupt;
		private int containerIndex;

		public RunnableAdapter(int placementsCount, int maxIteratorLength, long minStackableItemVolume, long minStackableArea) {
			this.placements = getPlacements(placementsCount);

			this.extremePoints3D = new ExtremePoints3DStack(maxIteratorLength + 1);
			this.extremePoints3D.reset(1, 1, 1);
		}

		public void setContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
		}
		
		public ContainerItem getContainerItem() {
			return containerItem;
		}

		public void setIterator(ParallelBoxItemPermutationRotationIterator iterator) {
			this.iterator = iterator;
		}

		public void setInterrupt(PackagerInterruptSupplier interrupt) {
			this.interrupt = interrupt;
		}

		@Override
		public BruteForceIntermediatePackagerResult call() throws PackagerInterruptedException {
			//System.out.println("START " + containerIndex);
			try {
				return ParallelBruteForcePackager.this.pack(extremePoints3D, placements, containerItem, containerIndex, iterator, interrupt);
			} finally {
				//System.out.println("END "+ containerIndex);
			}
		}
	}

	private class ParallelAdapter extends AbstractBruteForceBoxItemPackagerAdapter {

		private final RunnableAdapter[] runnables; // per thread
		private final ParallelBoxItemPermutationRotationIteratorList[] parallelIterators; // per container
		private final DefaultBoxItemPermutationRotationIterator[] iterators; // per container
		private final PackagerInterruptSupplier[] interrupts;

		protected ParallelAdapter(List<BoxItem> boxItems, BoxPriority priority,
				ContainerItemsCalculator<ContainerItem> packagerContainerItems, RunnableAdapter[] runnables, DefaultBoxItemPermutationRotationIterator[] iterators, ParallelBoxItemPermutationRotationIteratorList[] parallelIterators, PackagerInterruptSupplier[] interrupts) {
			super(boxItems, priority, packagerContainerItems);

			this.runnables = runnables;
			this.parallelIterators = parallelIterators;
			this.iterators = iterators;
			this.interrupts = interrupts;
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, BruteForceIntermediatePackagerResult currentBest, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			// is there enough work to do parallelization?
			// run on single thread for a small amount of combinations
			// the algorithm only splits on permutations
			boolean multithreaded;

			if(iterators[i].countPermutations() > parallelizationCount * 2) {
				multithreaded = true;
			} else {
				multithreaded = false;
			}
			
			if(multithreaded) {
				// interrupt needs not be accurate (i.e. atomic boolean)
				Boolean[] localInterrupt = new Boolean[32]; // add padding to avoid false sharing

				List<Future<BruteForceIntermediatePackagerResult>> futures = new ArrayList<>(runnables.length);
				for (int j = 0; j < runnables.length; j++) {
					RunnableAdapter runnableAdapter = runnables[j];
					
					ContainerItem containerItem = getContainerItem(i);
					
					runnableAdapter.setContainerItem(containerItem);
					runnableAdapter.setIterator(parallelIterators[i].getIterator(j));

					PackagerInterruptSupplier interruptBooleanSupplier = interrupts[i];

					PackagerInterruptSupplier booleanSupplier = () -> localInterrupt[15] != null || interruptBooleanSupplier.getAsBoolean();

					runnableAdapter.setInterrupt(booleanSupplier);

					futures.add(executorCompletionService.submit(runnableAdapter));
				}

				try {
					BruteForceIntermediatePackagerResult best = null;
					for (int j = 0; j < runnables.length; j++) {
						try {
							try {
								Future<BruteForceIntermediatePackagerResult> future = executorCompletionService.take();
								BruteForceIntermediatePackagerResult result = future.get();
								if(result != null) {
									result.markDirty();
									
									if(best == null || intermediatePackagerResultComparator.compare(best, result) == PackResultComparator.ARGUMENT_2_IS_BETTER) {
										best = result;
										
										if(best.containsLastStackable()) { // will not match any better than this
											// cancel others
											localInterrupt[15] = Boolean.TRUE;
											// don't break, so we're waiting for all the remaining threads to finish
										}
									}
								}
							} catch (ExecutionException e1) {
								Throwable cause = e1.getCause();
								if(cause instanceof PackagerInterruptedException && localInterrupt[15]) {
									continue;
								}
								throw e1.getCause();
							}								
						} catch (InterruptedException e1) {
							// ignore
							localInterrupt[15] = Boolean.TRUE;
							return null;
						} catch (Throwable e) {
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
					for (Future<BruteForceIntermediatePackagerResult> future : futures) {
						future.cancel(true);
					}
				}
			}
			
			ContainerItem containerItem = getContainerItem(i);
			
			// no need to split this job
			// run with linear approach
			return ParallelBruteForcePackager.this.pack(runnables[0].extremePoints3D, runnables[0].placements, containerItem, i, iterators[i],
					interrupts[i]);
		}

		@Override
		public Container accept(BruteForceIntermediatePackagerResult bruteForceResult) {
			
			//bruteForceResult.markDirty();
			Stack stack = bruteForceResult.getStack();
			
			Container container = packagerContainerItems.toContainer(bruteForceResult.getContainerItem(), stack);
			
			if(!bruteForceResult.containsLastStackable()) {
				// this result does not consume all placements
				// remove consumed items from the iterators

				int size = container.getStack().size();

				PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();

				int[] permutations = state.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}

				for (ParallelBoxItemPermutationRotationIteratorList it : parallelIterators) {
					it.removePermutations(p);
				}

				for (DefaultBoxItemPermutationRotationIterator it : iterators) {
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
				for(int i = 0; i < boxesRemaining.length; i++) {
					boxesRemaining[i] = 0;
				}
			}
			return container;
		}

		@Override
		public int countRemainingBoxes() {
			int count = 0;
			for (int i : boxesRemaining) {
				count += i;
			}
			return count;
		}

	}

	public void shutdown() {
		executorService.shutdownNow();
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	protected long getMinBoxItemVolume(List<BoxItem> stackables) {
		long minVolume = Integer.MAX_VALUE;
		for (BoxItem stackableItem : stackables) {
			Box stackable = stackableItem.getBox();
			if(stackable.getVolume() < minVolume) {
				minVolume = stackable.getVolume();
			}
		}
		return minVolume;
	}

	protected long getMinBoxItemArea(List<BoxItem> stackables) {
		long minArea = Integer.MAX_VALUE;
		for (BoxItem stackableItem : stackables) {
			Box stackable = stackableItem.getBox();
			if(stackable.getMinimumArea() < minArea) {
				minArea = stackable.getMinimumArea();
			}
		}
		return minArea;
	}

	@Override
	protected AbstractBruteForceBoxItemPackagerAdapter createBoxItemAdapter(List<BoxItem> items, BoxPriority priority,
			ContainerItemsCalculator<ContainerItem> defaultContainerItemsCalculator,
			PackagerInterruptSupplier interrupt) {
		
		List<ContainerItem> containerItems = defaultContainerItemsCalculator.getContainerItems();
		
		ParallelBoxItemPermutationRotationIteratorList[] parallelIterators = new ParallelBoxItemPermutationRotationIteratorList[containerItems.size()];
		DefaultBoxItemPermutationRotationIterator[] iterators = new DefaultBoxItemPermutationRotationIterator[containerItems.size()];
		for (int i = 0; i < containerItems.size(); i++) {
			Container container = containerItems.get(i).getContainer();

			parallelIterators[i] = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItems(items)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.withParallelizationCount(parallelizationCount)
					.build();

			iterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItems(items)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}

		int maxIteratorLength = 0;
		for (DefaultBoxItemPermutationRotationIterator iterator : iterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		
		int count = 0;
		for (BoxItem boxItem : items) {
			count += boxItem.getCount();
		}

		long minStackableItemVolume = getMinBoxItemVolume(items);
		long minStackableArea = getMinBoxItemArea(items);

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

		return new ParallelAdapter(items, priority, defaultContainerItemsCalculator, runnables, iterators, parallelIterators, interrupts);
	}

	@Override
	protected AbstractBruteForceBoxItemPackagerAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups,
			BoxPriority priority, ContainerItemsCalculator<ContainerItem> defaultContainerItemsCalculator,
			PackagerInterruptSupplier interrupt) {
		throw new RuntimeException("Not implemented");
	}


}
