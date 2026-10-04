package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerException;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.ClonablePackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.FilteredReversedBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.ParallelBoxItemGroupPermutationRotationIteratorList;
import com.github.skjolber.packing.iterator.ParallelBoxItemPermutationRotationIteratorList;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePackagerBuilder;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.BruteForcePointIteratorFilter;
import com.github.skjolber.packing.packer.bruteforce.LoadBruteForcePackager.Builder;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * 
 * Note on parallelization: The permutations are split into different tasks. Rotations + point placements is not.
 *
 */

public class ParallelBoxItemBruteForcePackager extends AbstractBruteForcePackager {

	private static final class LocalInterrupt {
		private volatile boolean interrupted;
	}

	public static ParallelBruteForcePackagerBuilder newBuilder() {
		return new ParallelBruteForcePackagerBuilder();
	}

	public static class ParallelBruteForcePackagerBuilder {

		protected int threads = -1;
		protected int parallelizationCount = -1;
		protected ExecutorService executorService;
		protected Comparator<IntermediatePackagerResult> comparator;
		protected BruteForcePointIteratorFilter pointFilter;
		protected boolean filterReversePermutations = false;
		protected ContainerStrategyFactory containerStrategyFactory;

		/**
		 * Set the factory which selects the container strategy: which containers to use, and in which order.
		 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
		 * (in preference order) which holds the boxes.
		 *
		 * @param factory container strategy factory
		 * @return this builder
		 */
		public ParallelBruteForcePackagerBuilder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

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
		

		public ParallelBruteForcePackagerBuilder withPointFilter(BruteForcePointIteratorFilter pointFilter) {
			this.pointFilter = pointFilter;
			return this;
		}

		public ParallelBruteForcePackagerBuilder withSkipReversePermutations(boolean filterReversePermutations) {
			this.filterReversePermutations = filterReversePermutations;
			return this;
		}
		
		public ParallelBoxItemBruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
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
			
			ParallelBoxItemBruteForcePackager packager = new ParallelBoxItemBruteForcePackager(executorService, parallelizationCount, comparator, pointFilter, filterReversePermutations);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			return packager;
		}
	}

	private final ExecutorCompletionService<BruteForceIntermediatePackagerResult> executorCompletionService;
	private final int parallelizationCount;
	private final ExecutorService executorService;
	protected final BruteForcePointIteratorFilter pointFilter;
	protected final boolean filterReversePermutations;

	public ParallelBoxItemBruteForcePackager(ExecutorService executorService, int parallelizationCount,
			Comparator<IntermediatePackagerResult> comparator, BruteForcePointIteratorFilter pointFilter) {
		this(executorService, parallelizationCount, comparator, pointFilter, false);
	}

	public ParallelBoxItemBruteForcePackager(ExecutorService executorService, int parallelizationCount, 
			Comparator<IntermediatePackagerResult> comparator, BruteForcePointIteratorFilter pointFilter, boolean filterReversePermutations) {
		super(comparator);

		this.parallelizationCount = parallelizationCount;
		this.executorService = executorService;
		this.executorCompletionService = new ExecutorCompletionService<BruteForceIntermediatePackagerResult>(executorService);
		this.pointFilter = pointFilter;
		this.filterReversePermutations = filterReversePermutations;
	}

	private BoxItemPermutationRotationIterator filterReversePermutations(BoxItemPermutationRotationIterator iterator, boolean abortOnAnyBoxTooBig) {
		if(!filterReversePermutations || !abortOnAnyBoxTooBig) {
			return iterator;
		}

		// A parallel work unit can start in the filtered half of a reverse pair.
		// Move it to its first canonical permutation before the pack loop processes
		// the iterator's current state.
		while(!FilteredReversedBoxItemPermutationRotationIterator.isCanonical(iterator.getPermutations())) {
			if(iterator.nextPermutation() == -1) {
				return null;
			}
		}
		return new FilteredReversedBoxItemPermutationRotationIterator(iterator);
	}

	private PackagerInterruptSupplier[] forkInterrupts(PackagerInterruptSupplier[] source) {
		PackagerInterruptSupplier[] copy = source.clone();
		for(int i = 0; i < copy.length; i++) {
			if(copy[i] instanceof ClonablePackagerInterruptSupplier clonable) {
				copy[i] = (PackagerInterruptSupplier) clonable.clone();
			}
		}
		return copy;
	}

	private class BruteForceWorker implements Callable<BruteForceIntermediatePackagerResult> {

		private ContainerItem containerItem;
		private BoxItemPermutationRotationIterator iterator;
		private final Placement[] placements;
		private int placementCount;
		private PointCalculator3DStack pointCalculator;
		private PackagerInterruptSupplier interrupt;
		private int containerIndex;

		public BruteForceWorker(int placementsCount, int maxIteratorLength, long minStackableItemVolume, long minStackableArea) {
			this.placements = getPlacements(placementsCount, supportsLoad());
			this.placementCount = placementsCount;

			this.pointCalculator = new PointCalculator3DStack(maxIteratorLength + 1);
			this.pointCalculator.reset(1, 1, 1);
		}

		public BruteForceWorker fork(int maxIteratorLength) {
			return new BruteForceWorker(placementCount, maxIteratorLength, 0L, 0L);
		}

		public void removeFirstPlacements(int size) {
			placementCount = BruteForcePackager.removeFirstPlacements(placements, size, placementCount);
		}

		public void clearPlacements() {
			placementCount = 0;
		}

		public void setContainerItem(ContainerItem containerItem) {
			this.containerItem = containerItem;
		}
		
		public ContainerItem getContainerItem() {
			return containerItem;
		}

		public void setIterator(BoxItemPermutationRotationIterator iterator) {
			this.iterator = iterator;
		}

		public void setInterrupt(PackagerInterruptSupplier interrupt) {
			this.interrupt = interrupt;
		}

		public void setContainerIndex(int containerIndex) {
			this.containerIndex = containerIndex;
		}

		@Override
		public BruteForceIntermediatePackagerResult call() throws PackagerInterruptedException {
			return ParallelBoxItemBruteForcePackager.this.pack(pointCalculator, placements, placementCount, containerItem, containerIndex, iterator, interrupt, pointFilter);
		}
	}

	private class ParallelSession extends AbstractBruteForceBoxItemSession {

		private BruteForceWorker[] runnables; // per thread
		private ParallelBoxItemPermutationRotationIteratorList[] parallelIterators; // per container
		private DefaultBoxItemPermutationRotationIterator[] iterators; // per container
		private PackagerInterruptSupplier[] interrupts;
		private final PackagerInterruptSupplier sourceInterrupt;

		protected ParallelSession(List<BoxItem> boxItems, List<ContainerItem> containers, int containerCount,
				BruteForceWorker[] runnables, DefaultBoxItemPermutationRotationIterator[] iterators,
				ParallelBoxItemPermutationRotationIteratorList[] parallelIterators, PackagerInterruptSupplier[] interrupts,
				PackagerInterruptSupplier sourceInterrupt) {
			super(boxItems, containers, containerCount);

			this.runnables = runnables;
			this.parallelIterators = parallelIterators;
			this.iterators = iterators;
			this.interrupts = interrupts;
			this.sourceInterrupt = sourceInterrupt;
		}

		private ParallelSession(ParallelSession source) {
			super(source);
			this.sourceInterrupt = source.sourceInterrupt;
			this.iterators = new DefaultBoxItemPermutationRotationIterator[source.iterators.length];
			this.parallelIterators = new ParallelBoxItemPermutationRotationIteratorList[source.parallelIterators.length];
			int maxIteratorLength = 0;
			for(int i = 0; i < iterators.length; i++) {
				iterators[i] = source.iterators[i].fork();
				parallelIterators[i] = source.parallelIterators[i].fork();
				maxIteratorLength = Math.max(maxIteratorLength, iterators[i].length());
			}
			this.runnables = new BruteForceWorker[source.runnables.length];
			for(int i = 0; i < runnables.length; i++) {
				runnables[i] = source.runnables[i].fork(maxIteratorLength);
			}
			this.interrupts = forkInterrupts(source.interrupts);
		}

		@Override
		protected ParallelSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemSession(copyInitialBoxItems(), containers, containerCount, sourceInterrupt);
		}

		@Override
		public ParallelSession fork() {
			return new ParallelSession(this);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult currentBest, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			// is there enough work to do parallelization?
			// run on single thread for a small amount of combinations
			// the algorithm only splits on permutations
			boolean multithreaded;
			long permutationCount = filterReversePermutations && abortOnAnyBoxTooBig
					? new FilteredReversedBoxItemPermutationRotationIterator(iterators[i]).countPermutations()
					: iterators[i].countPermutations();
			if(permutationCount > parallelizationCount * 2) {
				multithreaded = true;
			} else {
				multithreaded = false;
			}
			
			if(multithreaded) {
				LocalInterrupt localInterrupt = new LocalInterrupt();

				List<Future<BruteForceIntermediatePackagerResult>> futures = new ArrayList<>(runnables.length);
				for (int j = 0; j < runnables.length; j++) {
					BruteForceWorker worker = runnables[j];
					
					ContainerItem containerItem = getContainerItem(i);
					
					worker.setContainerItem(containerItem);
					worker.setContainerIndex(i);
					BoxItemPermutationRotationIterator iterator = filterReversePermutations(parallelIterators[i].getIterator(j), abortOnAnyBoxTooBig);
					if(iterator == null) {
						continue;
					}
					worker.setIterator(iterator);

					PackagerInterruptSupplier interruptBooleanSupplier = interrupts[i];

					PackagerInterruptSupplier booleanSupplier = () -> localInterrupt.interrupted || interruptBooleanSupplier.getAsBoolean();

					worker.setInterrupt(booleanSupplier);

					futures.add(executorCompletionService.submit(worker));
				}

				try {
					BruteForceIntermediatePackagerResult best = null;
					for (int j = 0; j < futures.size(); j++) {
						try {
							try {
								Future<BruteForceIntermediatePackagerResult> future = executorCompletionService.take();
								BruteForceIntermediatePackagerResult result = future.get();
								if(result != null) {
									if(best == null || intermediatePackagerResultComparator.compare(best, result) < 0) {
										best = result;
										
										if(best.containsLastStackable()) { // will not match any better than this
											// cancel others
											localInterrupt.interrupted = true;
											// don't break, so we're waiting for all the remaining threads to finish
										}
									}
								}
							} catch (ExecutionException e1) {
								Throwable cause = e1.getCause();
								if(cause instanceof PackagerInterruptedException) {
									if(localInterrupt.interrupted) {
										continue;
									}
									throw (PackagerInterruptedException)cause;
								}
								throw e1.getCause();
							}
						} catch (InterruptedException e1) {
							// ignore
							localInterrupt.interrupted = true;
							return null;
						} catch (PackagerInterruptedException e) {
							localInterrupt.interrupted = true;
							throw e;
						} catch (Throwable e) {
							localInterrupt.interrupted = true;
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
			BoxItemPermutationRotationIterator iterator = filterReversePermutations(iterators[i], abortOnAnyBoxTooBig);
			return ParallelBoxItemBruteForcePackager.this.pack(runnables[0].pointCalculator, runnables[0].placements, runnables[0].placementCount, containerItem, i, iterator,
					interrupts[i], pointFilter);
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			if(result instanceof BruteForceIntermediatePackagerResult bruteForceResult) {
				
				bruteForceResult.markDirty();
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
					
					// remove session inventory
					removeInventory(p);
	
					for (BruteForceWorker runner : runnables) {
						runner.removeFirstPlacements(size);
					}
				} else {
					for (BruteForceWorker runner : runnables) {
						runner.clearPlacements();
					}
					for(int i = 0; i < boxesRemaining.length; i++) {
						boxesRemaining[i] = 0;
					}
				}
				return container;
			} else {
				Stack stack = result.getStack();
				Container container = packagerContainerItems.toContainer(resolveContainerItem(result), stack);
				List<Integer> permutations = getLocalIndexes(stack);

				for (ParallelBoxItemPermutationRotationIteratorList iterator : parallelIterators) {
					iterator.removePermutations(permutations);
				}
				for (DefaultBoxItemPermutationRotationIterator iterator : iterators) {
					iterator.removePermutations(permutations);
				}
				removeInventory(permutations);
				for (BruteForceWorker runner : runnables) {
					runner.removeFirstPlacements(permutations.size());
				}
				return container;
			}
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

	private class ParallelGroupSession extends AbstractBruteForceBoxItemGroupSession {

		private BruteForceWorker[] runnables; // per thread
		private ParallelBoxItemGroupPermutationRotationIteratorList[] parallelIterators; // per container
		private DefaultBoxItemGroupPermutationRotationIterator[] iterators; // per container
		private PackagerInterruptSupplier[] interrupts;
		private final PackagerInterruptSupplier sourceInterrupt;

		protected ParallelGroupSession(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, 
				List<ContainerItem> containers, int containerCount, BruteForceWorker[] runnables,
				DefaultBoxItemGroupPermutationRotationIterator[] iterators,
				ParallelBoxItemGroupPermutationRotationIteratorList[] parallelIterators,
				PackagerInterruptSupplier[] interrupts, PackagerInterruptSupplier sourceInterrupt) {
			super(boxItems, containers, containerCount, boxItemGroups);
			this.runnables = runnables;
			this.parallelIterators = parallelIterators;
			this.iterators = iterators;
			this.interrupts = interrupts;
			this.sourceInterrupt = sourceInterrupt;
		}

		private ParallelGroupSession(ParallelGroupSession source) {
			super(source);
			this.sourceInterrupt = source.sourceInterrupt;
			this.iterators = new DefaultBoxItemGroupPermutationRotationIterator[source.iterators.length];
			this.parallelIterators = new ParallelBoxItemGroupPermutationRotationIteratorList[source.parallelIterators.length];
			int maxIteratorLength = 0;
			for(int i = 0; i < iterators.length; i++) {
				iterators[i] = source.iterators[i].fork();
				parallelIterators[i] = source.parallelIterators[i].fork();
				maxIteratorLength = Math.max(maxIteratorLength, iterators[i].length());
			}
			this.runnables = new BruteForceWorker[source.runnables.length];
			for(int i = 0; i < runnables.length; i++) {
				runnables[i] = source.runnables[i].fork(maxIteratorLength);
			}
			this.interrupts = forkInterrupts(source.interrupts);
		}

		@Override
		protected ParallelGroupSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemGroupSession(copyBoxItemGroups(initialBoxItemGroups), containers, containerCount, sourceInterrupt);
		}

		@Override
		public ParallelGroupSession fork() {
			return new ParallelGroupSession(this);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult currentBest, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			// is there enough work to do parallelization?
			// run on single thread for a small amount of combinations
			// the algorithm only splits on permutations
			boolean multithreaded;
			long permutationCount = filterReversePermutations && abortOnAnyBoxTooBig
					? new FilteredReversedBoxItemPermutationRotationIterator(iterators[i]).countPermutations()
					: iterators[i].countPermutations();
			if(permutationCount > parallelizationCount * 2) {
				multithreaded = true;
			} else {
				multithreaded = false;
			}
			
			if(multithreaded) {
				LocalInterrupt localInterrupt = new LocalInterrupt();

				List<Future<BruteForceIntermediatePackagerResult>> futures = new ArrayList<>(runnables.length);
				for (int j = 0; j < runnables.length; j++) {
					BruteForceWorker worker = runnables[j];
					
					ContainerItem containerItem = getContainerItem(i);
					
					worker.setContainerItem(containerItem);
					worker.setContainerIndex(i);
					BoxItemPermutationRotationIterator iterator = filterReversePermutations(parallelIterators[i].getIterator(j), abortOnAnyBoxTooBig);
					if(iterator == null) {
						continue;
					}
					worker.setIterator(iterator);

					PackagerInterruptSupplier interruptBooleanSupplier = interrupts[i];

					PackagerInterruptSupplier booleanSupplier = () -> localInterrupt.interrupted || interruptBooleanSupplier.getAsBoolean();

					worker.setInterrupt(booleanSupplier);

					futures.add(executorCompletionService.submit(worker));
				}

				try {
					BruteForceIntermediatePackagerResult best = null;
					for (int j = 0; j < futures.size(); j++) {
						try {
							try {
								Future<BruteForceIntermediatePackagerResult> future = executorCompletionService.take();
								
								// TODO can truncate be moved to thread?
								BruteForceIntermediatePackagerResult result = truncateToGroup(future.get());
								if(result != null) {
									if(best == null || intermediatePackagerResultComparator.compare(best, result) < 0) {
										best = result;
										
										if(best.containsLastStackable()) { // will not match any better than this
											// cancel others
											localInterrupt.interrupted = true;
											// don't break, so we're waiting for all the remaining threads to finish
										}
									}
								}
							} catch (ExecutionException e1) {
								Throwable cause = e1.getCause();
								if(cause instanceof PackagerInterruptedException) {
									if(localInterrupt.interrupted) {
										continue;
									}
									throw (PackagerInterruptedException)cause;
								}
								throw e1.getCause();
							}
						} catch (InterruptedException e1) {
							// ignore
							localInterrupt.interrupted = true;
							return null;
						} catch (PackagerInterruptedException e) {
							localInterrupt.interrupted = true;
							throw e;
						} catch (Throwable e) {
							localInterrupt.interrupted = true;
							throw new PackagerException(e);
						}
					}
					// was the search interrupted?
					if(interrupts[i].getAsBoolean()) {
						return null;
					}
					// throw away boxes from incomplete groups
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
			BoxItemPermutationRotationIterator iterator = filterReversePermutations(iterators[i], abortOnAnyBoxTooBig);
			return truncateToGroup(ParallelBoxItemBruteForcePackager.this.pack(
					runnables[0].pointCalculator,
					runnables[0].placements,
					runnables[0].placementCount,
					containerItem,
					i,
					iterator,
					interrupts[i],
					pointFilter
			));
		}

		@Override
		public Container accept(IntermediatePackagerResult result) {
			if(result instanceof BruteForceIntermediatePackagerResult bruteForceResult) {
				
				bruteForceResult.markDirty();
				Stack stack = bruteForceResult.getStack();
				
				Container container = packagerContainerItems.toContainer(bruteForceResult.getContainerItem(), stack);
	
				if(!bruteForceResult.containsLastStackable()) {
					// this result does not consume all placements
					// remove consumed items from the iterators
	
					int size = container.getStack().size();
	
					PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();
	
					// this result does not consume all placements
					// remove consumed items from the iterators
	
					// TODO only handles groups in order.
					
					List<Integer> removedGroups = new ArrayList<>();
					int wholeGroupBoxCount = 0;
					for(int i = 0; i < boxItemGroups.size(); i++) {
						BoxItemGroup boxItemGroup = boxItemGroups.get(i);
						
						int groupBoxCount = boxItemGroup.getBoxCount();
						if(size < wholeGroupBoxCount + groupBoxCount) {
							// the last group was not successful
							throw new IllegalStateException("Expected to consume whole groups, but group " + i + " was not fully consumed");
						}
						
						removedGroups.add(i);
						
						wholeGroupBoxCount += groupBoxCount;
						
						if(wholeGroupBoxCount == size) {
							break;
						}
					}
					
					int[] permutations = state.getPermutations();
					
					List<Integer> p = new ArrayList<>();
					for(Integer removedGroup: removedGroups) {
						BoxItemGroup boxItemGroup = boxItemGroups.get(removedGroup);
	
						for (BoxItem boxItem : boxItemGroup.getItems()) {
							for (int i = 0; i < boxItem.getCount(); i++) {
								p.add(permutations[p.size()]);
							}
						}
					}						
	
					// remove stacked items which did not make it
					stack.setSize(p.size());
	
					for (ParallelBoxItemGroupPermutationRotationIteratorList it : parallelIterators) {
						it.removeGroups(removedGroups);
					}
	
					for (DefaultBoxItemGroupPermutationRotationIterator it : iterators) {
						it.removeGroups(removedGroups);
					}
					
					boxItemGroups = boxItemGroups.subList(removedGroups.size(), this.boxItemGroups.size());
					
					// remove session inventory
					removeInventory(p);
	
					for (BruteForceWorker runner : runnables) {
						runner.removeFirstPlacements(p.size());
					}
				} else {
					for (BruteForceWorker runner : runnables) {
						runner.clearPlacements();
					}
					for(int i = 0; i < boxesRemaining.length; i++) {
						boxesRemaining[i] = 0;
					}
				}
				return container;
			} else {
				Stack stack = result.getStack();
				AcceptedGroups accepted = getAcceptedGroups(stack);
				Container container = packagerContainerItems.toContainer(resolveContainerItem(result), stack);

				for (ParallelBoxItemGroupPermutationRotationIteratorList iterator : parallelIterators) {
					iterator.removeGroups(accepted.groupIndexes());
				}
				for (DefaultBoxItemGroupPermutationRotationIterator iterator : iterators) {
					iterator.removeGroups(accepted.groupIndexes());
				}
				boxItemGroups = boxItemGroups.subList(accepted.groupIndexes().size(), boxItemGroups.size());
				removeInventory(accepted.localIndexes());
				for (BruteForceWorker runner : runnables) {
					runner.removeFirstPlacements(accepted.localIndexes().size());
				}
				return container;
			}
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
	protected ParallelSession createBoxItemSession(List<BoxItem> items, List<ContainerItem> containerItems,
			int containerCount, PackagerInterruptSupplier interrupt) {
		
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
		for (BoxItem stackable : items) {
			count += stackable.getCount();
		}

		long minStackableItemVolume = getMinBoxItemVolume(items);
		long minStackableArea = getMinBoxItemArea(items);

		BruteForceWorker[] runnables = new BruteForceWorker[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {
			runnables[i] = new BruteForceWorker(count, maxIteratorLength, minStackableItemVolume, minStackableArea);
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

		return new ParallelSession(items, containerItems, containerCount, runnables, iterators, parallelIterators, interrupts, interrupt);
	}

	@Override
	protected ParallelGroupSession createBoxItemGroupSession(List<BoxItemGroup> itemGroups,
			List<ContainerItem> containerItems, int containerCount, PackagerInterruptSupplier interrupt) {
		
		ParallelBoxItemGroupPermutationRotationIteratorList[] parallelIterators = new ParallelBoxItemGroupPermutationRotationIteratorList[containerItems.size()];
		DefaultBoxItemGroupPermutationRotationIterator[] iterators = new DefaultBoxItemGroupPermutationRotationIterator[containerItems.size()];
		for (int i = 0; i < containerItems.size(); i++) {
			Container container = containerItems.get(i).getContainer();

			parallelIterators[i] = ParallelBoxItemGroupPermutationRotationIteratorList.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItemGroups(itemGroups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.withParallelizationCount(parallelizationCount)
					.build();

			iterators[i] = DefaultBoxItemGroupPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItemGroups(itemGroups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}

		int maxIteratorLength = 0;
		for (DefaultBoxItemGroupPermutationRotationIterator iterator : iterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		
		List<BoxItem> items = new ArrayList<BoxItem>();
		for (BoxItemGroup boxItemGroup : itemGroups) {
			items.addAll(boxItemGroup.getItems());
		}
		
		int count = 0;
		for (BoxItem boxItem : items) {
			count += boxItem.getCount();
		}

		long minStackableItemVolume = getMinBoxItemVolume(items);
		long minStackableArea = getMinBoxItemArea(items);

		BruteForceWorker[] runnables = new BruteForceWorker[parallelizationCount];
		for (int i = 0; i < parallelizationCount; i++) {
			runnables[i] = new BruteForceWorker(count, maxIteratorLength, minStackableItemVolume, minStackableArea);
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

		return new ParallelGroupSession(items, itemGroups, containerItems, containerCount, runnables, iterators, parallelIterators, interrupts, interrupt);
	}

	@Override
	protected LoadPlacementUtility createLoadPlacementUtility(BoxItemPermutationRotationIterator iterator, Stack stack) {
		return null;
	}

}
