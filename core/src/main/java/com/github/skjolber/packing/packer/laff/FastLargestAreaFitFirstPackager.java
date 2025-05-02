package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderFactory;
import com.github.skjolber.packing.api.packager.BoxItemGroupListener;
import com.github.skjolber.packing.api.packager.BoxItemListener;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemComparator;
import com.github.skjolber.packing.comparator.LargestAreaBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.LargestAreaIntermediatePlacementResultComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemGroupComparator;
import com.github.skjolber.packing.comparator.VolumeWeightAreaPointIntermediatePlacementResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;
import com.github.skjolber.packing.ep.points2d.MarkResetExtremePoints2D;
import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementResultBuilderFactory;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.DefaultPackResult;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager.Builder;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. Only places boxes along the floor of each level.
 * <br>
 * <br>
 * Thread-safe implementation. The input boxes must however only be used in a single thread at a time.
 */

public class FastLargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPackagerBuilder<FastLargestAreaFitFirstPackager, Builder> {

		protected Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator;
		protected IntermediatePackagerResultComparator intermediatePackagerResultComparator;
		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected Comparator<BoxItem> boxItemComparator;
		
		protected Comparator<IntermediatePlacementResult> firstIntermediatePlacementResultComparator;
		protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
		protected Comparator<BoxItem> firstBoxItemComparator;

		public Builder withFirstBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
			this.firstBoxItemGroupComparator = boxItemGroupComparator;
			return this;
		}

		public Builder withFirstBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
			this.firstBoxItemComparator = boxItemComparator;
			return this;
		}

		public Builder withBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
			this.boxItemGroupComparator = boxItemGroupComparator;
			return this;
		}
		
		public Builder withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
			this.boxItemComparator = boxItemComparator;
			return this;
		}
		
		public Builder withFirstIntermediatePlacementResultComparator(Comparator<IntermediatePlacementResult> c) {
			this.firstIntermediatePlacementResultComparator = c;
			return this;
		}
	
		public Builder withIntermediatePlacementResultComparator(
				Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator) {
			this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
			return this;
		}
		
		public FastLargestAreaFitFirstPackager build() {
			if(intermediatePlacementResultComparator == null) {
				intermediatePlacementResultComparator = new VolumeWeightAreaPointIntermediatePlacementResultComparator();
			}
			if(intermediatePackagerResultComparator == null) {
				intermediatePackagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			if(boxItemComparator == null) {
				boxItemComparator = VolumeThenWeightBoxItemComparator.getInstance();
			}
			if(boxItemGroupComparator == null) {
				boxItemGroupComparator = VolumeThenWeightBoxItemGroupComparator.getInstance();
			}
			if(firstBoxItemGroupComparator == null) {
				firstBoxItemGroupComparator = new LargestAreaBoxItemGroupComparator();
			}
			if(firstBoxItemComparator == null) {
				firstBoxItemComparator = new LargestAreaBoxItemComparator();
			}
			if(firstIntermediatePlacementResultComparator == null) {
				firstIntermediatePlacementResultComparator = new LargestAreaIntermediatePlacementResultComparator();
			}
			return new FastLargestAreaFitFirstPackager(intermediatePackagerResultComparator, intermediatePlacementResultComparator, boxItemComparator, boxItemGroupComparator, firstBoxItemGroupComparator, firstBoxItemComparator, firstIntermediatePlacementResultComparator);
		}
	}

	protected ComparatorIntermediatePlacementResultBuilderFactory intermediatePlacementResultBuilderFactory = new ComparatorIntermediatePlacementResultBuilderFactory();
	protected Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;
	
	protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
	protected Comparator<BoxItem> firstBoxItemComparator;
	protected Comparator<IntermediatePlacementResult> firstIntermediatePlacementResultComparator;
	
	public FastLargestAreaFitFirstPackager(IntermediatePackagerResultComparator comparator, Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator, Comparator<BoxItem> boxItemComparator, Comparator<BoxItemGroup> boxItemGroupComparator, Comparator<BoxItemGroup> firstBoxItemGroupComparator, Comparator<BoxItem> firstBoxItemComparator, Comparator<IntermediatePlacementResult> firstIntermediatePlacementResultComparator) {
		super(comparator);
		
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		this.boxItemComparator = boxItemComparator;
		this.boxItemGroupComparator = boxItemGroupComparator;
		
		this.firstBoxItemGroupComparator = firstBoxItemGroupComparator;
		this.firstBoxItemComparator = firstBoxItemComparator;
		this.firstIntermediatePlacementResultComparator = firstIntermediatePlacementResultComparator;
	}

	public DefaultIntermediatePackagerResult pack(List<BoxItem> boxes, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		Stack stack = new Stack();

		List<BoxItem> scopedBoxItems = boxes.stream().filter(s -> container.fitsInside(s.getBox())).collect(Collectors.toList());
		if(scopedBoxItems.isEmpty()) {
			return new DefaultIntermediatePackagerResult(containerItem, stack);
		}
		
		DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems(scopedBoxItems);

		ExtremePoints2D extremePoints2D = new ExtremePoints2D();
		extremePoints2D.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		//extremePoints2D.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		BoxItemListener listener = compositeContainerItem.createBoxItemListener(container, stack, filteredBoxItems, extremePoints2D);

		int remainingLoadWeight = container.getMaxLoadWeight();

		int levelOffset = 0;
		boolean newLevel = true;

		while (remainingLoadWeight > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			IntermediatePlacementResult result;
			if(newLevel) {
				// get first box in new level
				result = findBestFirstPoint(filteredBoxItems, extremePoints2D, compositeContainerItem.getFilteredPointsBuilderFactory(), container, stack);
				if(result == null) {
					break;
				}
				
				DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, result.getStackValue().getDz() - 1 + levelOffset);
				
				extremePoints2D.setInitialPoints(Arrays.asList(levelFloor));
				extremePoints2D.clear();
				
				levelOffset += result.getStackValue().getDz();

				newLevel = false;
			} else {
				// next
				result = findBestPoint(filteredBoxItems, extremePoints2D, compositeContainerItem.getFilteredPointsBuilderFactory(), container, stack);
				if(result == null) {
					newLevel = true;

					int remainingDz = container.getLoadDz() - levelOffset;
					if(remainingDz == 0) {
						break;
					}

					// prepare extreme points for a new level						
					DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, container.getLoadDz() - 1);
					extremePoints2D.setInitialPoints(Arrays.asList(levelFloor));
					extremePoints2D.clear();
					
					continue;
				}
			}
			
			StackPlacement stackPlacement = new StackPlacement(null, 
					result.getBoxItem(), 
					result.getStackValue(), 
					result.getPoint().getMinX(), 
					result.getPoint().getMinY(), 
					result.getPoint().getMinZ()
			);

			stack.add(stackPlacement);
			extremePoints2D.add(result.getPoint(), stackPlacement);
			
			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
			
			result.getBoxItem().decrement();
			
			listener.accepted(result.getBoxItem());
			
			filteredBoxItems.removeEmpty();
			
			/*
			if(!filteredBoxItems.isEmpty()) {
				extremePoints2D.updateMinimums(result.getStackValue(), filteredBoxItems);
			}
			*/
		}
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem.getContainerItem(), stack);
	}

	public DefaultIntermediatePackagerResult pack(List<BoxItemGroup> boxItemGroups, Order itemGroupOrder, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();
		
		List<BoxItemGroup> scopedBoxItemGroups = getFitsInside(boxItemGroups, container);

		DefaultFilteredBoxItemGroups filteredBoxItemGroups = new DefaultFilteredBoxItemGroups(scopedBoxItemGroups);

		Stack stack = new Stack();

		MarkResetExtremePoints2D extremePoints3D = new MarkResetExtremePoints2D();
		extremePoints3D.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		//extremePoints3D.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());

		BoxItemGroupListener listener = compositeContainerItem.createBoxItemGroupListener(container, stack, filteredBoxItemGroups, extremePoints3D);

		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, itemGroupOrder, container, extremePoints3D);

		int levelOffset = 0;
		boolean newLevel = true;

		groups:
		while (!extremePoints3D.isEmpty() && !filteredBoxItemGroups.isEmpty() && boxItemGroupIterator.hasNext()) {
			int bestBoxItemGroupIndex = boxItemGroupIterator.next();

			BoxItemGroup boxItemGroup = filteredBoxItemGroups.remove(bestBoxItemGroupIndex);

			extremePoints3D.mark();
			int markStackSize = stack.size();
			
			int markLevelOffset = levelOffset;
			boolean markNewLevel = newLevel;
			
			DefaultFilteredBoxItems boxItems = new DefaultFilteredBoxItems(new ArrayList<>(boxItemGroup.getItems())); 

			while(!boxItems.isEmpty()) {
				
				IntermediatePlacementResult bestPoint;
				if(newLevel) {
					// get first box in new level
					bestPoint = findBestFirstPoint(boxItems, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderFactory(), container, stack);
					if(bestPoint == null) {
						break;
					}
					
					DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, bestPoint.getStackValue().getDz() - 1 + levelOffset);
					
					extremePoints3D.setInitialPoints(Arrays.asList(levelFloor));
					extremePoints3D.clear();
					
					levelOffset += bestPoint.getStackValue().getDz();

					newLevel = false;
				} else {
					// next
					bestPoint = findBestPoint(boxItems, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderFactory(), container, stack);
					if(bestPoint == null) {
						newLevel = true;

						int remainingDz = container.getLoadDz() - levelOffset;
						if(remainingDz == 0) {
							break;
						}

						// prepare extreme points for a new level						
						DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, container.getLoadDz() - 1);
						extremePoints3D.setInitialPoints(Arrays.asList(levelFloor));
						extremePoints3D.clear();
						
						continue;
					}
				}
				
				bestPoint.getBoxItem().decrement();

				boxItems.removeEmpty();
				
				StackPlacement stackPlacement = new StackPlacement(boxItemGroup, bestPoint.getBoxItem(), bestPoint.getStackValue(), bestPoint.getPoint().getMinX(), bestPoint.getPoint().getMinY(), bestPoint.getPoint().getMinZ());
				stack.add(stackPlacement);
				extremePoints3D.add(bestPoint.getPoint(), stackPlacement);

				/*
				if(!boxItems.isEmpty()) {
					extremePoints3D.updateMinimums(bestPoint.getStackValue(), filteredBoxItemGroups);
				}
				*/

			}
			
			if(!boxItems.isEmpty()) {
				// unable to stack whole group
				if(itemGroupOrder == Order.FIXED) {
					break groups;
				}
				// discard the whole group, try again with another group if possible
				extremePoints3D.reset();
				stack.setSize(markStackSize);
				
				levelOffset = markLevelOffset;
				newLevel = markNewLevel;

				continue groups;
			}
			
			if(container.getMaxLoadWeight() < extremePoints3D.getUsedWeight()) {
				throw new RuntimeException();
			}
			
			// successfully stacked group
			listener.accepted(boxItemGroup);

		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	
	protected BoxItemGroupIterator createBoxItemGroupIterator(FilteredBoxItemGroups filteredBoxItemGroups, Order itemGroupOrder, Container container, ExtremePoints extremePoints) {
		if(itemGroupOrder == Order.FIXED) {
			return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints, boxItemGroupComparator);
		}
		return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints);
	}

	public IntermediatePlacementResult findBestPoint(FilteredBoxItems boxItems, ExtremePoints extremePoints, FilteredPointsBuilderFactory filteredPointsBuilderFactory, Container container, Stack stack) {
		return intermediatePlacementResultBuilderFactory.createIntermediatePlacementResultBuilder()
			.withContainer(container)
			.withExtremePoints(extremePoints)
			.withStack(stack)
			.withFilteredBoxItems(boxItems)
			.withFilteredPointsBuilderFactory(filteredPointsBuilderFactory)
			.withIntermediatePlacementResultComparator(intermediatePlacementResultComparator)
			.withBoxItemComparator(boxItemComparator)
			.build();
	}
	
	public IntermediatePlacementResult findBestFirstPoint(FilteredBoxItems boxItems, ExtremePoints extremePoints, FilteredPointsBuilderFactory filteredPointsBuilderFactory, Container container, Stack stack) {
		return intermediatePlacementResultBuilderFactory.createIntermediatePlacementResultBuilder()
			.withContainer(container)
			.withExtremePoints(extremePoints)
			.withStack(stack)
			.withFilteredBoxItems(boxItems)
			.withFilteredPointsBuilderFactory(filteredPointsBuilderFactory)
			.withIntermediatePlacementResultComparator(firstIntermediatePlacementResultComparator)
			.withBoxItemComparator(firstBoxItemComparator)
			.build();
	}

}
