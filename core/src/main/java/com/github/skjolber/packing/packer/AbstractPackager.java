package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.strategy.ContainerStrategy;
import com.github.skjolber.packing.packer.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.packer.strategy.ContainerResult;
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
	private volatile ContainerStrategyFactory containerPackingStrategyFactory;
	
	protected final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(Integer.MAX_VALUE);

	public AbstractPackager(Comparator<IntermediatePackagerResult> comparator) {
		this.intermediatePackagerResultComparator = comparator;
		this.containerPackingStrategyFactory = new DefaultContainerStrategyFactory(comparator,
				this::createEmptyIntermediatePackagerResult);
	}

	/** Configure before using this packager concurrently. */
	public void setContainerPackingStrategyFactory(ContainerStrategyFactory factory) {
		this.containerPackingStrategyFactory = Objects.requireNonNull(factory);
	}

	public ContainerResult packAdapter(PackagerInterruptSupplier interrupt, PackagerAdapter adapter) throws PackagerInterruptedException {
		ContainerStrategy strategy = containerPackingStrategyFactory.create(adapter.getContainerItemsCalculator(), adapter.getRemainingBoxItems(), adapter.getRemainingBoxItemGroups());
		return strategy.pack(interrupt, adapter);
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
