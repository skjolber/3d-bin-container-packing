package com.github.skjolber.packing.packer.strategy.ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerException;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategy;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner;

/**
 * Attempts every eligible container-item index concurrently.
 *
 * <p>Each task receives a session fork, so it can freely mutate its own
 * packing state. The selected result is then accepted by the original session.
 * This requires sessions to support accepting results produced by another
 * session in the same packaging operation.</p>
 */
public class ParallelContainerPackingStrategy implements ContainerStrategy {

	private final ExecutorService executorService;
	private final IntermediatePackagerResultComparator comparator;
	private final boolean allocationFeasibilityCheck;

	public ParallelContainerPackingStrategy(ExecutorService executorService,
			IntermediatePackagerResultComparator comparator) {
		this(executorService, comparator, true);
	}

	private ParallelContainerPackingStrategy(ExecutorService executorService,
			IntermediatePackagerResultComparator comparator, boolean allocationFeasibilityCheck) {
		this.executorService = executorService;
		this.comparator = comparator;
		this.allocationFeasibilityCheck = allocationFeasibilityCheck;
	}

	/** Create a variant for callers that have already established allocation feasibility. */
	public ParallelContainerPackingStrategy withoutAllocationFeasibilityCheck() {
		return new ParallelContainerPackingStrategy(executorService, comparator, false);
	}

	@Override
	public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerSession session) throws PackagerInterruptedException {
		int limit = session.getMaxContainerCount();
		List<Container> containers = new ArrayList<>();

		while(containers.size() < limit) {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			if(allocationFeasibilityCheck && !ContainerAllocationPlanner.canAllocate(session, interrupt)) {
				return null;
			}

			List<Integer> containerIndexes = session.getContainers();
			if(containerIndexes.isEmpty()) {
				return null;
			}

			int remainingContainerCount = limit - containers.size();
			IntermediatePackagerResult best = attemptAll(containerIndexes, session, interrupt, remainingContainerCount == 1);
			if(best == null) {
				return null;
			}

			containers.add(session.accept(best));
			if(session.countRemainingBoxes() == 0) {
				return new ContainerResult(session.getContainerInventory().getCost(), containers);
			}
		}
		return null;
	}

	private IntermediatePackagerResult attemptAll(List<Integer> containerIndexes, PackagerSession session,
			PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		List<Future<IntermediatePackagerResult>> futures = new ArrayList<>(containerIndexes.size());
		try {
			for(int containerIndex : containerIndexes) {
				PackagerSession fork = session.fork();
				futures.add(executorService.submit(() -> fork.attempt(containerIndex, null, abortOnAnyBoxTooBig)));
			}

			IntermediatePackagerResult best = null;
			for(Future<IntermediatePackagerResult> future : futures) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				IntermediatePackagerResult result = get(future);
				if(result != null && !result.isEmpty() && (best == null || comparator.compare(best, result) <= 0)) {
					best = result;
				}
			}
			return best;
		} finally {
			for(Future<?> future : futures) {
				if(!future.isDone()) {
					future.cancel(true);
				}
			}
		}
	}

	private static IntermediatePackagerResult get(Future<IntermediatePackagerResult> future) throws PackagerInterruptedException {
		try {
			return future.get();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PackagerInterruptedException();
		} catch(ExecutionException e) {
			Throwable cause = e.getCause();
			if(cause instanceof PackagerInterruptedException interrupted) {
				throw interrupted;
			}
			if(cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new PackagerException(cause);
		}
	}
}
