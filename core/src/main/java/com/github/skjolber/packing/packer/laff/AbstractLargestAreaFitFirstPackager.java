package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableFilter;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.ep.Point2D;
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
public abstract class AbstractLargestAreaFitFirstPackager<P extends Point2D<StackPlacement>> extends AbstractPackager<DefaultPackResult, LargestAreaFitFirstPackagerResultBuilder> {

	public static StackableFilter FIRST_STACKABLE_FILTER = (best, candidate) -> {
		// return true if the candidate might be better than the current best
		return candidate.getMaximumArea() >= best.getMinimumArea();
	};

	public static StackableFilter DEFAULT_STACKABLE_FILTER = (best, candidate) -> {
		// return true if the candidate might be better than the current best
		return candidate.getVolume() >= best.getVolume();
	};

	public static StackValuePointFilter DEFAULT_STACK_VALUE_POINT_FILTER = (stackable1, point1, stackValue1, stackable2, point2, stackValue2) -> {
		if(stackable2.getVolume() == stackable1.getVolume()) {
			if(stackValue1.getArea() == stackValue2.getArea()) {
				// closest distance to a wall is better

				int distance1 = Math.min(point1.getDx() - stackValue1.getDx(), point1.getDy() - stackValue1.getDy());
				int distance2 = Math.min(point2.getDx() - stackValue2.getDx(), point2.getDy() - stackValue2.getDy());

				return distance2 < distance1; // closest is better
			}
			return stackValue2.getArea() < stackValue1.getArea(); // smaller is better
		}
		return stackable2.getVolume() > stackable1.getVolume(); // larger volume is better 
	};

	public static StackValuePointFilter FIRST_STACK_VALUE_POINT_FILTER = (stackable1, point1, stackValue1, stackable2, point2, stackValue2) -> {
		if(stackValue1.getArea() == stackValue2.getArea()) {
			if(stackValue1.getVolume() == stackValue2.getVolume()) {
				// closest distance to a wall is better

				int distance1 = Math.min(point1.getDx() - stackValue1.getDx(), point1.getDy() - stackValue1.getDy());
				int distance2 = Math.min(point2.getDx() - stackValue2.getDx(), point2.getDy() - stackValue2.getDy());

				return distance2 < distance1; // closest is better
			}
			return stackValue1.getVolume() < stackValue2.getVolume(); // larger volume is better 

		}
		return stackValue1.getArea() < stackValue2.getArea(); // larger area is better
	};
	
	
	public AbstractLargestAreaFitFirstPackager(PackResultComparator packResultComparator) {
		super(packResultComparator);
	}

	public abstract DefaultPackResult pack(List<Stackable> stackables, Container targetContainer, int index, PackagerInterruptSupplier interrupt);

	protected class LAFFAdapter extends AbstractPackagerAdapter<DefaultPackResult> {

		private List<Stackable> boxes;
		private final PackagerInterruptSupplier interrupt;

		public LAFFAdapter(List<StackableItem> boxItems, List<ContainerItem> containerItems, PackagerInterruptSupplier interrupt) {
			super(containerItems);

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
			return AbstractLargestAreaFitFirstPackager.this.pack(boxes, containerItems.get(index).getContainer(), index, interrupt);
		}

		@Override
		public Container accept(DefaultPackResult result) {
			super.accept(result.getContainerItemIndex());

			Container container = result.getContainer();
			Stack stack = container.getStack();

			List<Stackable> placed = stack.getPlacements().stream().map(p -> p.getStackable()).collect(Collectors.toList());

			boxes.removeAll(placed);

			return container;
		}

		@Override
		public List<Integer> getContainers(int maxCount) {
			return getContainers(boxes, maxCount);
		}

	}

	@Override
	protected LAFFAdapter adapter(List<StackableItem> boxes, List<ContainerItem> containers, PackagerInterruptSupplier interrupt) {
		return new LAFFAdapter(boxes, containers, interrupt);
	}

	protected ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
		return scheduledThreadPoolExecutor;
	}

}
