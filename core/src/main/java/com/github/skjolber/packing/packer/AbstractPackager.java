package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategy;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.strategy.DefaultContainerStrategyFactory;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 *
 * Thread-safe implementation.
 * 
 *  @param <B> packager
 */

public abstract class AbstractPackager<B extends PackagerResultBuilder> implements Packager<B> {

	public static final int ARGUMENT_1_IS_BETTER = 1;
	public static final int ARGUMENT_2_IS_BETTER = -1;

	protected final Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;
	private volatile ContainerStrategyFactory containerStrategyFactory;
	
	protected final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(Integer.MAX_VALUE);

	public AbstractPackager(Comparator<IntermediatePackagerResult> comparator) {
		this.intermediatePackagerResultComparator = comparator;
		this.containerStrategyFactory = new DefaultContainerStrategyFactory(comparator,
				this::createEmptyIntermediatePackagerResult);
	}

	/** Used by the builders, before the packager is returned. */
	protected void setContainerStrategyFactory(ContainerStrategyFactory factory) {
		this.containerStrategyFactory = factory;
	}

	/**
	 * Create a session for a packaging operation.
	 *
	 * @param input the boxes and containers, supported by this packager (see {@link #supports(PackagerInput)})
	 * @param interrupt interrupt for the session's packing attempts
	 * @return a session at the start of the packaging operation
	 */
	public PackagerSession createSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		return newSession(input, interrupt);
	}

	/**
	 * @param input the boxes and containers
	 * @param interrupt interrupt for the session's packing attempts
	 * @return a session at the start of the packaging operation
	 */
	protected abstract PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt);

	/**
	 * @param input the boxes and containers
	 * @return null if this packager supports the input, otherwise the reason why not
	 */
	public String getUnsupportedReason(PackagerInput input) {
		return null;
	}

	public boolean supports(PackagerInput input) {
		return getUnsupportedReason(input) == null;
	}

	/**
	 * Pack the input, using the container strategy.
	 *
	 * @param input the boxes and containers, supported by this packager (see {@link #supports(PackagerInput)})
	 * @param deadline deadline in milliseconds, or -1 for none
	 * @param interrupt interrupt, or null for none
	 * @return the result; empty if interrupted or if the boxes could not be packed
	 */
	public PackagerResult pack(PackagerInput input, long deadline, PackagerInterruptSupplier interrupt) {
		long start = System.currentTimeMillis();

		PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
		if(deadline != -1L) {
			booleanSupplierBuilder.withDeadline(deadline);
		}
		if(interrupt != null) {
			booleanSupplierBuilder.withInterrupt(interrupt);
		}
		booleanSupplierBuilder.withScheduledThreadPoolExecutor(scheduledThreadPoolExecutor);

		PackagerInterruptSupplier packagerInterrupt = booleanSupplierBuilder.build();
		try {
			PackagerSession session = createSession(input, packagerInterrupt);
			ContainerResult result = packSession(packagerInterrupt, session);

			long duration = System.currentTimeMillis() - start;
			if(result == null) {
				return new PackagerResult(Collections.emptyList(), duration, false, -1);
			}
			return new PackagerResult(result.getPackList(), duration, false, result.getCost());
		} catch (PackagerInterruptedException e) {
			long duration = System.currentTimeMillis() - start;
			return new PackagerResult(Collections.emptyList(), duration, true, -1);
		} finally {
			packagerInterrupt.close();
		}
	}

	public ContainerResult packSession(PackagerInterruptSupplier interrupt, PackagerSession session) throws PackagerInterruptedException {
		ContainerStrategy strategy = containerStrategyFactory.create(session.getContainerInventory(), session.getRemainingBoxItems(), session.getRemainingBoxItemGroups());
		return strategy.pack(interrupt, session);
	}

	public void close() {
		scheduledThreadPoolExecutor.shutdownNow();
	}
	
	protected List<BoxItemGroup> getFitsInside(List<BoxItemGroup> inputs, Container container) {
		List<BoxItemGroup> result = new ArrayList<>(inputs.size());
		for (BoxItemGroup boxItemGroup : inputs) {
			if(container.fitsInside(boxItemGroup)) {
				result.add(boxItemGroup);
			}
		}
		return result;
	}
	
	protected List<BoxItem> getBoxItemsFitsInside(List<BoxItem> inputs, Container container) {
		List<BoxItem> result = new ArrayList<>(inputs.size());
		for (BoxItem boxItem : inputs) {
			if(container.fitsInside(boxItem)) {
				result.add(boxItem);
			}
		}
		return result;
	}
	
	public List<BoxItem> removeEmpty(List<BoxItem> values) {
		List<BoxItem> result = new ArrayList<>(values.size());
		for(int i = 0; i < values.size(); i++) {
			if(!values.get(i).isEmpty()) {
				result.add(values.get(i));
			}
		}
		return result;
	}
	
	public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
		return scheduledThreadPoolExecutor;
	}
	
	protected abstract IntermediatePackagerResult createEmptyIntermediatePackagerResult();

}
