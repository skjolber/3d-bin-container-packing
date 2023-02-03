package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.packer.DefaultPackResult;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractLargestAreaFitFirstPackager<P extends Point2D<StackPlacement>> extends AbstractPackager<DefaultPackResult, LargestAreaFitFirstPackagerResultBuilder> {

	protected LargestAreaFitFirstPackagerConfigurationBuilderFactory<P, ?> factory;

	public AbstractLargestAreaFitFirstPackager(List<Container> containers, int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator,
			LargestAreaFitFirstPackagerConfigurationBuilderFactory<P, ?> factory) {
		super(containers, checkpointsPerDeadlineCheck, packResultComparator);

		this.factory = factory;
	}

	public DefaultPackResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public DefaultPackResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck, BooleanSupplier interrupt) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public abstract DefaultPackResult pack(List<Stackable> stackables, Container targetContainer, BooleanSupplier interrupt);

	protected class LAFFAdapter implements Adapter<DefaultPackResult> {

		private List<Stackable> boxes;
		private List<Container> containers;
		private final BooleanSupplier interrupt;

		public LAFFAdapter(List<StackableItem> boxItems, List<Container> container, BooleanSupplier interrupt) {
			this.containers = container;

			List<Stackable> boxClones = new ArrayList<>(boxItems.size() * 2);

			for (StackableItem item : boxItems) {
				Stackable box = item.getStackable();
				boxClones.add(box);
				for (int i = 1; i < item.getCount(); i++) {
					boxClones.add(box.clone());
				}
			}

			this.boxes = boxClones;
			this.interrupt = interrupt;
		}

		@Override
		public DefaultPackResult attempt(int index, DefaultPackResult best) {
			return AbstractLargestAreaFitFirstPackager.this.pack(boxes, containers.get(index), interrupt);
		}

		@Override
		public Container accept(DefaultPackResult result) {
			Container container = result.getContainer();
			Stack stack = container.getStack();

			List<Stackable> placed = stack.getPlacements().stream().map(p -> p.getStackable()).collect(Collectors.toList());

			boxes.removeAll(placed);

			return container;
		}

	}

	@Override
	protected LAFFAdapter adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new LAFFAdapter(boxes, containers, interrupt);
	}

}
