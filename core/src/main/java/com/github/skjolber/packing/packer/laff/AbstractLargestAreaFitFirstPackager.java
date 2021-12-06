package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.packer.EmptyPackResult;
import com.github.skjolber.packing.packer.PackResult;
import com.github.skjolber.packing.points2d.Point2D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractLargestAreaFitFirstPackager<P extends Point2D> extends AbstractPackager<LargestAreaFitFirstPackagerResult, LargestAreaFitFirstPackagerResultBuilder> {

	protected LargestAreaFitFirstPackagerConfigurationBuilderFactory<P, ?> factory;

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param configurationBuilder 
	 * @param checkpointsPerDeadlineCheck 
	 */
	public AbstractLargestAreaFitFirstPackager(List<Container> containers, LargestAreaFitFirstPackagerConfigurationBuilderFactory<P, ?> factory) {
		this(containers, 1, factory);
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param footprintFirst start with box which has the largest footprint. If not, the highest box is first.
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a container that can hold the boxes, given time, it also tries to find a better match.
	 */

	public AbstractLargestAreaFitFirstPackager(List<Container> containers, int checkpointsPerDeadlineCheck, LargestAreaFitFirstPackagerConfigurationBuilderFactory<P, ?> factory) {
		super(containers, checkpointsPerDeadlineCheck);
		
		this.factory = factory;
	}
	
	/**
	 *
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param containerProducts list of boxes to fit in a container.
	 * @param targetContainer the container to fit within
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return null if no match, or deadline reached
	 */

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck, BooleanSupplier interrupt) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public abstract LargestAreaFitFirstPackagerResult pack(List<Stackable> stackables, Container targetContainer,  BooleanSupplier interrupt);

	protected class LAFFAdapter implements Adapter<LargestAreaFitFirstPackagerResult> {

		private List<Stackable> boxes;
		private List<Container> containers;
		private final BooleanSupplier interrupt;

		public LAFFAdapter(List<StackableItem> boxItems, List<Container> container, BooleanSupplier interrupt) {
			this.containers = container;

			List<Stackable> boxClones = new ArrayList<>(boxItems.size() * 2);

			for(StackableItem item : boxItems) {
				Stackable box = item.getStackable();
				boxClones.add(box);
				for(int i = 1; i < item.getCount(); i++) {
					boxClones.add(box.clone());
				}
			}

			this.boxes = boxClones;
			this.interrupt = interrupt;
		}

		@Override
		public LargestAreaFitFirstPackagerResult attempt(int index, LargestAreaFitFirstPackagerResult best) {
			return AbstractLargestAreaFitFirstPackager.this.pack(boxes, containers.get(index), interrupt);
		}

		@Override
		public Container accept(LargestAreaFitFirstPackagerResult result) {
			return result.getContainer();
		}

	}

	@Override
	protected LAFFAdapter adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new LAFFAdapter(boxes, containers, interrupt);
	}

}
