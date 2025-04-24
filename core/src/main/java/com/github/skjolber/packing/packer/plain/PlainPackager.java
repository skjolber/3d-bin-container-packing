package com.github.skjolber.packing.packer.plain;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderSupplier;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.BoxItemGroupListener;
import com.github.skjolber.packing.api.packager.BoxItemListener;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResultBuilderSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.AbstractSimplePackager;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractSimplePackager {

	protected static final VolumeThenWeightBoxItemGroupComparator BOX_ITEM_GROUP_COMPARATOR = new VolumeThenWeightBoxItemGroupComparator();
	protected static final VolumeThenWeightBoxItemComparator BOX_ITEM_COMPARATOR = new VolumeThenWeightBoxItemComparator();
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPackagerBuilder<PlainPackager, Builder> {

		protected Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator;
		protected IntermediatePackagerResultComparator intermediatePackagerResultComparator;
		
		public Builder withPackResultComparator(Comparator<PlainIntermediatePlacementResult> c) {
			this.intermediatePlacementResultComparator = c;
			return this;
		}

		public Builder withIntermediatePlacementResultComparator(
				Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator) {
			this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
			return this;
		}
		
		public PlainPackager build() {
			if(intermediatePlacementResultComparator == null) {
				intermediatePlacementResultComparator = new PlainPlacementResultComparator();
			}
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			return new PlainPackager(intermediatePackagerResultComparator, intermediatePlacementResultComparator);
		}
	}

	protected PlainIntermediatePlacementResultBuilderSupplier supplier = new PlainIntermediatePlacementResultBuilderSupplier();
	
	protected Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator;
	
	public PlainPackager(IntermediatePackagerResultComparator comparator, Comparator<PlainIntermediatePlacementResult> intermediatePlacementResultComparator) {
		super(comparator);
		
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
	}
	
	@Override
	public DefaultIntermediatePackagerResult pack(List<BoxItem> boxes, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		Stack stack = new Stack();

		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		List<BoxItem> scopedBoxItems = boxes.stream().filter(s -> container.fitsInside(s.getBox())).collect(Collectors.toList());

		DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems(scopedBoxItems);

		ExtremePoints3D extremePoints3D = new ExtremePoints3D(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinBoxItemArea(filteredBoxItems), getMinBoxItemVolume(filteredBoxItems));

		BoxItemListener listener = compositeContainerItem.createBoxItemListener(container, stack, filteredBoxItems, extremePoints3D);

		int remainingLoadWeight = container.getMaxLoadWeight();

		while (!extremePoints3D.isEmpty() && remainingLoadWeight > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			IntermediatePlacementResult result = findBestPoint(filteredBoxItems, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderSupplier(), container, stack);
			if(result == null) {
				break;
			}

			Point point = result.getPoint();
			
			StackPlacement stackPlacement = new StackPlacement(null, result.getBoxItem(), result.getStackValue(), point.getMinX(), point.getMinY(), point.getMinZ());
			stack.add(stackPlacement);
			extremePoints3D.add(result.getPoint(), stackPlacement);

			result.getBoxItem().decrement();

			listener.accepted(result.getBoxItem());
			
			filteredBoxItems.removeEmpty();
	
			if(!filteredBoxItems.isEmpty()) {
				extremePoints3D.updateMinimums(result.getStackValue(), filteredBoxItems);
			}

			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
		}
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem.getContainerItem(), stack);
	}
	
	public DefaultIntermediatePackagerResult pack(List<BoxItemGroup> boxItemGroups, Order itemGroupOrder, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();
		
		List<BoxItemGroup> scopedBoxItemGroups = getFitsInside(boxItemGroups, container);

		DefaultFilteredBoxItemGroups filteredBoxItemGroups = new DefaultFilteredBoxItemGroups(scopedBoxItemGroups);

		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints3D = new MarkResetExtremePoints3D(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinBoxItemGroupArea(scopedBoxItemGroups), getMinBoxItemGroupVolume(scopedBoxItemGroups));

		BoxItemGroupListener listener = compositeContainerItem.createBoxItemGroupListener(container, stack, filteredBoxItemGroups, extremePoints3D);

		groups:
		while (!extremePoints3D.isEmpty() && !filteredBoxItemGroups.isEmpty()) {
			int bestBoxItemGroupIndex = getNextBoxItemGroup(container, scopedBoxItemGroups, itemGroupOrder, extremePoints3D);
			if(bestBoxItemGroupIndex == -1) {
				break;
			}

			BoxItemGroup boxItemGroup = filteredBoxItemGroups.remove(bestBoxItemGroupIndex);

			extremePoints3D.mark();
			
			while(!boxItemGroup.isEmpty()) {
				
				DefaultFilteredBoxItems items = new DefaultFilteredBoxItems(boxItemGroup.getItems()); 
				
				IntermediatePlacementResult bestPoint = findBestPoint(items, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderSupplier(), container, stack);
				if(bestPoint == null) {
					if(itemGroupOrder == Order.FIXED) {
						break groups;
					}
					// discard the whole group
					extremePoints3D.reset();
					
					continue groups;
				}
				
				bestPoint.getBoxItem().decrement();

				StackPlacement stackPlacement = new StackPlacement(boxItemGroup, bestPoint.getBoxItem(), bestPoint.getStackValue(), bestPoint.getPoint().getMinX(), bestPoint.getPoint().getMinY(), bestPoint.getPoint().getMinZ());
				stack.add(stackPlacement);
				extremePoints3D.add(bestPoint.getPoint(), stackPlacement);

				if(!boxItemGroup.isEmpty()) {
					extremePoints3D.updateMinimums(bestPoint.getStackValue(), filteredBoxItemGroups);
				}

			}
			
			if(container.getMaxLoadWeight() < extremePoints3D.getUsedWeight()) {
				throw new RuntimeException();
			}
			
			// successfully stacked group
			listener.accepted(boxItemGroup);

		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	public IntermediatePlacementResult findBestPoint(FilteredBoxItems boxItems, ExtremePoints3D extremePoints3D, FilteredPointsBuilderSupplier filteredPointsBuilderSupplier, Container container, Stack stack) {
		return supplier.getIntermediatePlacementResultBuilder()
			.withContainer(container)
			.withExtremePoints(extremePoints3D)
			.withStack(stack)
			.withFilteredBoxItems(boxItems)
			.withFilteredPointsBuilderSupplier(filteredPointsBuilderSupplier)
			.withIntermediatePlacementResultComparator(intermediatePlacementResultComparator)
			.withBoxItemComparator(BOX_ITEM_COMPARATOR)
			.build();
	}

	public int getNextBoxItemGroup(Container container, List<BoxItemGroup> boxItemGroups, Order itemGroupOrder, ExtremePoints3D extremePoints3D) {
		// can any of the groups be split?

		if(itemGroupOrder == Order.FIXED) {
			return getFirstItemGroup(boxItemGroups, container, extremePoints3D);
		} else {
			return getBestItemGroup(boxItemGroups, container, extremePoints3D);
		}
	}

	protected int getFirstItemGroup(List<BoxItemGroup> boxItemGroups, Container container, ExtremePoints3D extremePoints3D) {
		long maxPointVolume = extremePoints3D.getMaxVolume();
		long maxPointArea = extremePoints3D.getMaxArea();

		long maxTotalPointVolume = container.getMaxLoadVolume() -  extremePoints3D.getUsedVolume();		
		long maxTotalWeight = container.getMaxLoadWeight() -  extremePoints3D.getUsedWeight();

		BoxItemGroup bestBoxItemGroup = boxItemGroups.get(0);

		if(bestBoxItemGroup.getVolume() > maxTotalPointVolume) {
			return -1;
		}
		if(bestBoxItemGroup.getWeight() > maxTotalWeight) {
			return -1;
		}
		for (int i = 0; i < bestBoxItemGroup.size(); i++) {
			BoxItem boxItem = bestBoxItemGroup.get(i);
			
			Box box = boxItem.getBox();
			if(box.getVolume() > maxPointVolume) {
				return -1;
			}

			if(box.getMinimumArea() > maxPointArea) {
				return -1;
			}
		}
		return 0;
	}

	protected int getBestItemGroup(List<BoxItemGroup> boxItemGroups, Container container,
			ExtremePoints3D extremePoints3D) {
		long maxPointVolume = extremePoints3D.getMaxVolume();
		long maxPointArea = extremePoints3D.getMaxArea();

		long maxTotalPointVolume = container.getMaxLoadVolume() -  extremePoints3D.getUsedVolume();		
		long maxTotalWeight = container.getMaxLoadWeight() -  extremePoints3D.getUsedWeight();

		BoxItemGroup bestBoxItemGroup = null;
		int bestIndex = -1;
		
		// find next best group
		bestGroupSearch:
		for (int l = 0; l < boxItemGroups.size(); l++) {
			BoxItemGroup group = boxItemGroups.get(l);
			
			if(group.getVolume() > maxTotalPointVolume) {
				continue;
			}
			if(group.getWeight() > maxTotalWeight) {
				continue;
			}
			for (int i = 0; i < group.size(); i++) {
				BoxItem boxItem = group.get(i);
				
				Box box = boxItem.getBox();
				if(box.getVolume() > maxPointVolume) {
					continue bestGroupSearch;
				}

				if(box.getMinimumArea() > maxPointArea) {
					continue bestGroupSearch;
				}
			}
			
			if(bestBoxItemGroup == null || BOX_ITEM_GROUP_COMPARATOR.compare(bestBoxItemGroup, group) > 0) {
				bestBoxItemGroup = group;
				bestIndex = l;
			}
		}
		return bestIndex;
	}
}
