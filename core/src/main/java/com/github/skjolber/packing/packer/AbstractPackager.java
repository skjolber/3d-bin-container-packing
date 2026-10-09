package com.github.skjolber.packing.packer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategy;
import com.github.skjolber.packing.api.packager.strategy.ContainerPackingStrategyFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.strategy.DefaultContainerPackingStrategyFactory;

/**
 * Base class for packagers: fit boxes into one or more containers, i.e. perform bin packing.
 *
 * Thread-safe implementation.
 * 
 *  @param <B> packager
 */

public abstract class AbstractPackager<B extends PackagerResultBuilder> implements Packager<B> {

	public static final int ARGUMENT_1_IS_BETTER = 1;
	public static final int ARGUMENT_2_IS_BETTER = -1;

	protected final IntermediatePackagerResultComparator intermediatePackagerResultComparator;
	/** Whether results with less load volume always compare worse, see {@link IntermediatePackagerResultComparator#prefersHigherLoadVolume()} */
	protected final boolean prefersHigherLoadVolume;
	private volatile ContainerPackingStrategyFactory containerPackingStrategyFactory;
	
	/** The deadline tasks only flag expiry, so one thread is enough. */
	protected final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

	public AbstractPackager(IntermediatePackagerResultComparator comparator) {
		this.scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
		this.intermediatePackagerResultComparator = comparator;
		this.prefersHigherLoadVolume = comparator != null && comparator.prefersHigherLoadVolume();
		this.containerPackingStrategyFactory = new DefaultContainerPackingStrategyFactory();
	}

	/** Used by the builders, before the packager is returned. */
	protected void setContainerPackingStrategyFactory(ContainerPackingStrategyFactory factory) {
		this.containerPackingStrategyFactory = factory;
	}

	/**
	 * Create a session for a packaging operation. Sessions for the same input are independent of one another.
	 *
	 * @param input the boxes and containers, supported by this packager (see {@link #supports(PackagerInput)})
	 * @param interrupt interrupt for the session's packing attempts
	 * @return a session at the start of the packaging operation
	 */
	public PackagerSession createSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		// sessions count down boxes and containers as containers are accepted
		AbstractPackagerSession.initializeGlobalIndexes(input);
		return newSession(input.withCopies(), interrupt);
	}

	/**
	 * @param input the box items and container items, which belong to the new session (the boxes are shared)
	 * @param interrupt interrupt for the session's packing attempts
	 * @return a session at the start of the packaging operation
	 */
	protected abstract PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt);

	/**
	 * @param input the boxes and containers
	 * @return null if this packager supports the input, otherwise the reason why not
	 */
	public String getUnsupportedReason(PackagerInput input) {
		if(input.getOrder() != Order.NONE && hasDecreasingContainerPriorities(input)) {
			// the box items arrive in the given order: they cannot be reordered by container priority
			return "Container priorities must not decrease in the box item order";
		}
		return null;
	}

	/**
	 * @return true if any of the boxes have load limits (max load weight, pressure or box count, identical boxes only)
	 */
	protected static boolean hasLoadLimits(PackagerInput input) {
		if(input.hasBoxItems()) {
			return hasLoadLimits(input.getBoxItems());
		}
		if(input.getBoxItemGroups() != null) {
			for (BoxItemGroup group : input.getBoxItemGroups()) {
				if(hasLoadLimits(group.getItems())) {
					return true;
				}
			}
		}
		return false;
	}

	protected static boolean hasLoadLimits(List<BoxItem> boxItems) {
		for (BoxItem boxItem : boxItems) {
			if(boxItem.isMaxLoad() || boxItem.getBox().isLoadIdenticalBoxOnly()) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasDecreasingContainerPriorities(PackagerInput input) {
		if(input.hasBoxItems()) {
			List<BoxItem> boxItems = input.getBoxItems();
			for (int i = 1; i < boxItems.size(); i++) {
				if(boxItems.get(i).getContainerPriority() < boxItems.get(i - 1).getContainerPriority()) {
					return true;
				}
			}
			return false;
		}
		List<BoxItemGroup> groups = input.getBoxItemGroups();
		for (int i = 1; i < groups.size(); i++) {
			if(groups.get(i).getContainerPriority() < groups.get(i - 1).getContainerPriority()) {
				return true;
			}
		}
		return false;
	}

	public boolean supports(PackagerInput input) {
		return getUnsupportedReason(input) == null;
	}

	/**
	 * Pack the input, using the container packing strategy.
	 *
	 * @param input the boxes and containers, supported by this packager (see {@link #supports(PackagerInput)})
	 * @param deadline deadline in milliseconds, or -1 for none
	 * @param interrupt interrupt, or null for none
	 * @return the result; empty if interrupted or if the boxes could not be packed
	 */
	public PackagerResult pack(PackagerInput input, long deadline, PackagerInterruptSupplier interrupt) {
		long start = System.currentTimeMillis();

		PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.newBuilder();
		if(deadline != -1L) {
			booleanSupplierBuilder.withDeadline(deadline);
		}
		if(interrupt != null) {
			booleanSupplierBuilder.withInterrupt(interrupt);
		}
		booleanSupplierBuilder.withScheduledExecutorService(scheduledThreadPoolExecutor);

		PackagerInterruptSupplier packagerInterrupt = booleanSupplierBuilder.build();
		try {
			PackagerSession session = createSession(input, packagerInterrupt);
			ContainerResult result = packSession(packagerInterrupt, session);

			long duration = System.currentTimeMillis() - start;
			if(result == null) {
				return new PackagerResult(Collections.emptyList(), duration, false, -1);
			}
			return new PackagerResult(result.getPackList(), duration, false, result.getCost(), sequence(input, result.getPackList()));
		} catch (PackagerInterruptedException e) {
			long duration = System.currentTimeMillis() - start;
			return new PackagerResult(Collections.emptyList(), duration, true, -1);
		} finally {
			// the library closes only what it created
			if(packagerInterrupt != interrupt) {
				packagerInterrupt.close();
			}
		}
	}

	/**
	 * Put the containers in insertion order, unless skipped (see {@link InsertionSequencer}).
	 *
	 * @return true if the containers are in insertion order
	 */
	protected static boolean sequence(PackagerInput input, List<Container> containers) {
		if(input.isInsertionOrder()) {
			return InsertionSequencer.sequence(containers, input.getOrder());
		}
		// with a box item order, only insertable boxes are placed
		return input.getOrder() != Order.NONE;
	}

	public ContainerResult packSession(PackagerInterruptSupplier interrupt, PackagerSession session) throws PackagerInterruptedException {
		ContainerPackingStrategy strategy = containerPackingStrategyFactory.create(session.getContainerInventory(), session.getRemainingBoxItems(), session.getRemainingBoxItemGroups(),
				intermediatePackagerResultComparator, this::createEmptyIntermediatePackagerResult);
		return strategy.pack(interrupt, session);
	}

	public void close() {
		scheduledThreadPoolExecutor.shutdownNow();
	}
	
	protected abstract IntermediatePackagerResult createEmptyIntermediatePackagerResult();

}
