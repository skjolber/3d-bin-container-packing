package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractPlainPackager<P extends Point2D<StackPlacement>> extends AbstractPackager<PlainPackagerResult, PlainPackagerResultBuilder> {

	public AbstractPlainPackager(List<Container> containers) {
		this(containers, 1);
	}

	public AbstractPlainPackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}
	
	public PlainPackagerResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public PlainPackagerResult pack(List<Stackable> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck, BooleanSupplier interrupt) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public abstract PlainPackagerResult pack(List<Stackable> stackables, Container targetContainer,  BooleanSupplier interrupt);

	protected class PlainAdapter implements Adapter<PlainPackagerResult> {

		private List<Stackable> boxes;
		private List<Container> containers;
		private final BooleanSupplier interrupt;

		public PlainAdapter(List<StackableItem> boxItems, List<Container> container, BooleanSupplier interrupt) {
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
		public PlainPackagerResult attempt(int index, PlainPackagerResult best) {
			return AbstractPlainPackager.this.pack(boxes, containers.get(index), interrupt);
		}

		@Override
		public Container accept(PlainPackagerResult result) {
			return result.getContainer();
		}

	}

	@Override
	protected PlainAdapter adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new PlainAdapter(boxes, containers, interrupt);
	}

}
