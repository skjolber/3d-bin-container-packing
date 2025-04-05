package com.github.skjolber.packing.packer.plain;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerAdapter;
import com.github.skjolber.packing.packer.DefaultPackResult;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractPlainPackager extends AbstractPackager<DefaultPackResult, PlainPackagerResultBuilder> {

	public AbstractPlainPackager(PackResultComparator packResultComparator) {
		super(packResultComparator);
	}

	public abstract DefaultPackResult pack(List<Box> stackables, Container targetContainer, int containerIndex, PackagerInterruptSupplier interrupt);

	protected class PlainAdapter extends AbstractPackagerAdapter<DefaultPackResult> {

		private List<Box> boxes;
		private final PackagerInterruptSupplier interrupt;

		public PlainAdapter(List<BoxItem> boxItems, List<ContainerItem> containerItems, PackagerInterruptSupplier interrupt) {
			super(containerItems);

			List<Box> boxClones = new LinkedList<>();

			for (BoxItem item : boxItems) {
				Box box = item.getStackable();
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
			return AbstractPlainPackager.this.pack(boxes, containerItems.get(index).getContainer(), index, interrupt);
		}

		@Override
		public Container accept(DefaultPackResult result) {
			super.accept(result.getContainerItemIndex());

			Container container = result.getContainer();
			Stack stack = container.getStack();

			List<Box> placed = stack.getPlacements().stream().map(p -> p.getStackable()).collect(Collectors.toList());

			boxes.removeAll(placed);

			return container;
		}

		@Override
		public List<Integer> getContainers(int maxCount) {
			return getContainers(boxes, maxCount);
		}

	}

	@Override
	protected PlainAdapter adapter(List<BoxItem> boxes, List<ContainerItem> containers, PackagerInterruptSupplier interrupt) {
		return new PlainAdapter(boxes, containers, interrupt);
	}

	protected ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
		return scheduledThreadPoolExecutor;
	}

}
