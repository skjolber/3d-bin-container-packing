package com.github.skjolber.packing.packer.laff;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Priority;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.BoxItemGroupControls;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
import com.github.skjolber.packing.api.packager.PackagerBoxItems;
import com.github.skjolber.packing.api.packager.PointControls;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.packer.AbstractDefaultPackager;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementResultBuilderFactory;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyPackagerResultAdapter;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractLargestAreaFitFirstPackager extends AbstractDefaultPackager {

	public AbstractLargestAreaFitFirstPackager(IntermediatePackagerResultComparator packResultComparator) {
		super(packResultComparator);
	}

	public static abstract class LargestAreaFitFirstPackagerBuilder<P extends AbstractLargestAreaFitFirstPackager, B extends AbstractPackagerBuilder<P, B>> extends AbstractPackagerBuilder<P, B> {

		protected Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator;
		protected IntermediatePackagerResultComparator intermediatePackagerResultComparator;
		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected Comparator<BoxItem> boxItemComparator;
		
		protected Comparator<IntermediatePlacementResult> firstIntermediatePlacementResultComparator;
		protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
		protected Comparator<BoxItem> firstBoxItemComparator;

		public B withFirstBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
			this.firstBoxItemGroupComparator = boxItemGroupComparator;
			return (B)this;
		}

		public B withFirstBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
			this.firstBoxItemComparator = boxItemComparator;
			return (B)this;
		}

		public B withBoxItemGroupComparator(Comparator<BoxItemGroup> boxItemGroupComparator) {
			this.boxItemGroupComparator = boxItemGroupComparator;
			return (B)this;
		}
		
		public B withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
			this.boxItemComparator = boxItemComparator;
			return (B)this;
		}
		
		public B withFirstIntermediatePlacementResultComparator(Comparator<IntermediatePlacementResult> c) {
			this.firstIntermediatePlacementResultComparator = c;
			return (B)this;
		}
	
		public B withIntermediatePlacementResultComparator(
				Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator) {
			this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
			return (B)this;
		}
		
	}

	protected ComparatorIntermediatePlacementResultBuilderFactory intermediatePlacementResultBuilderFactory = new ComparatorIntermediatePlacementResultBuilderFactory();
	protected Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;
	
	protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
	protected Comparator<BoxItem> firstBoxItemComparator;
	protected Comparator<IntermediatePlacementResult> firstIntermediatePlacementResultComparator;
	
	public AbstractLargestAreaFitFirstPackager(IntermediatePackagerResultComparator comparator, Comparator<IntermediatePlacementResult> intermediatePlacementResultComparator, Comparator<BoxItem> boxItemComparator, Comparator<BoxItemGroup> boxItemGroupComparator, Comparator<BoxItemGroup> firstBoxItemGroupComparator, Comparator<BoxItem> firstBoxItemComparator, Comparator<IntermediatePlacementResult> firstIntermediatePlacementResultComparator) {
		super(comparator);
		
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		this.boxItemComparator = boxItemComparator;
		this.boxItemGroupComparator = boxItemGroupComparator;
		
		this.firstBoxItemGroupComparator = firstBoxItemGroupComparator;
		this.firstBoxItemComparator = firstBoxItemComparator;
		this.firstIntermediatePlacementResultComparator = firstIntermediatePlacementResultComparator;
	}

	public IntermediatePackagerResult pack(List<BoxItem> boxItems, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, Priority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		Stack stack = new Stack();

		ExtremePoints extremePoints = createExtremePoints();
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

		DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems(boxItems);
		BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);
		
		// remove boxes which do not fit due to volume, weight or stack value dimensions
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			BoxItem boxItem = filteredBoxItems.get(i);
			if(!container.fitsInside(boxItem.getBox())) {
				if(abortOnAnyBoxTooBig) {
					return EmptyPackagerResultAdapter.EMPTY;
				}
				
				if(priority != Priority.NATURAL) {
					filteredBoxItems.remove(i);
					i--;
					
					boxItemControls.declined(boxItem);
					pointControls.declined(boxItem);
				} else {
					// remove all later then the first removed

					while(i < filteredBoxItems.size()) {
						boxItem = filteredBoxItems.get(i);
						filteredBoxItems.remove(i);
						
						boxItemControls.declined(boxItem);
						pointControls.declined(boxItem);
					}
				}
			}
		}
		
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		int levelOffset = 0;
		boolean newLevel = true;

		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			IntermediatePlacementResult result;
			if(newLevel) {
				// get first box in new level
				result = findBestFirstPoint(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);
				if(result == null) {
					break;
				}
				
				DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, result.getStackValue().getDz() - 1 + levelOffset);
				
				extremePoints.setPoints(Arrays.asList(levelFloor));
				extremePoints.clear();
				
				levelOffset += result.getStackValue().getDz();

				newLevel = false;
			} else {
				// next
				result = findBestPoint(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);
				if(result == null) {
					newLevel = true;

					int remainingDz = container.getLoadDz() - levelOffset;
					if(remainingDz == 0) {
						break;
					}

					// prepare extreme points for a new level						
					DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, container.getLoadDz() - 1);
					extremePoints.setPoints(Arrays.asList(levelFloor));
					extremePoints.clear();
					
					// remove boxes which are too big for the max new level
					long maxArea = extremePoints.getMaxArea();
					long maxVolume = extremePoints.getMaxVolume();
					
					for(int i = 0; i < filteredBoxItems.size(); i++) {
						BoxItem boxItem = filteredBoxItems.get(i);
						Box box = boxItem.getBox();
						if(box.getVolume() > maxVolume || box.getMinimumArea() > maxArea) {
							if(abortOnAnyBoxTooBig) {
								return EmptyPackagerResultAdapter.EMPTY;
							}
							
							if(priority != Priority.NATURAL) {
								filteredBoxItems.remove(i);
								i--;
								
								boxItemControls.declined(boxItem);
								pointControls.declined(boxItem);
							} else {
								// remove all later then the first removed

								while(i < filteredBoxItems.size()) {
									boxItem = filteredBoxItems.get(i);
									filteredBoxItems.remove(i);
									
									boxItemControls.declined(boxItem);
									pointControls.declined(boxItem);
								}
							}
						}
					}					
					
					continue;
				}
			}
			
			StackPlacement stackPlacement = new StackPlacement(result.getStackValue(), 
				result.getPoint().getMinX(), 
				result.getPoint().getMinY(), 
				result.getPoint().getMinZ()
			);

			stack.add(stackPlacement);
			extremePoints.add(result.getPoint(), stackPlacement);
			
			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
			remainingLoadVolume -= result.getBoxItem().getBox().getVolume();
			
			filteredBoxItems.decrement(result.getIndex(), 1);

			boxItemControls.accepted(result.getBoxItem());
			pointControls.accepted(result.getBoxItem());
			
			if(!filteredBoxItems.isEmpty()) {
				// remove items are too big according to total volume / weight
				for(int i = 0; i < filteredBoxItems.size(); i++) {
					BoxItem boxItem = filteredBoxItems.get(i);
					Box box = boxItem.getBox();
					if(box.getVolume() > remainingLoadVolume || box.getWeight() > remainingLoadWeight) {
						
						if(abortOnAnyBoxTooBig) {
							return EmptyPackagerResultAdapter.EMPTY;
						}
						
						if(priority != Priority.NATURAL) {
							filteredBoxItems.remove(i);
							i--;
							
							boxItemControls.declined(boxItem);
							pointControls.declined(boxItem);
						} else {
							// remove all later then the first removed

							while(i < filteredBoxItems.size()) {
								boxItem = filteredBoxItems.get(i);
								filteredBoxItems.remove(i);
								
								boxItemControls.declined(boxItem);
								pointControls.declined(boxItem);
							}
						}
					}
				}
				
				// remove small points
				extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());				
			}
		}
		
		// ignore decline for the rest
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem.getContainerItem(), stack);
	}

	protected abstract ExtremePoints createExtremePoints();

	public IntermediatePackagerResult packGroup(List<BoxItemGroup> boxItemGroups, Priority priority, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();
		
		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints = new MarkResetExtremePoints3D(true);
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		
		PackagerBoxItems packagerBoxItems = new PackagerBoxItems(boxItemGroups);

		FilteredBoxItems filteredBoxItems = packagerBoxItems.getFilteredBoxItems();

		BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);

		BoxItemGroupControls boxItemGroupControls = boxItemControls instanceof BoxItemGroupControls ? (BoxItemGroupControls)boxItemControls : null;

		FilteredBoxItemGroups filteredBoxItemGroups = packagerBoxItems.getFilteredBoxItemGroups();
				
		// remove boxes which do not fit due to volume, weight or stack value dimensions
		for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
			if(!container.fitsInside(boxItemGroup)) {
				if(abortOnAnyBoxTooBig) {
					return EmptyPackagerResultAdapter.EMPTY;
				}
				
				filteredBoxItemGroups.remove(i);
				i--;
				
				for(int k = 0; k < boxItemGroup.size(); k++) {
					boxItemControls.declined(boxItemGroup.get(k));
					if(pointControls != null) {
						pointControls.declined(boxItemGroup.get(k));
					}
				}
				
				if(boxItemGroupControls != null) {
					boxItemGroupControls.declined(boxItemGroup);
				}

			}
		}
		
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		
		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, priority, container, extremePoints);

		int levelOffset = 0;
		boolean newLevel = true;

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		groups:
		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !extremePoints.isEmpty() && boxItemGroupIterator.hasNext()) {
			int groupIndex = boxItemGroupIterator.next();
			
			int boxItemStartIndex = packagerBoxItems.getFirstBoxItemIndexForGroup(groupIndex);
			
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(groupIndex);
			boxItemGroup.mark();
			
			extremePoints.mark();
			int markStackSize = stack.size();
			
			int markLevelOffset = levelOffset;
			boolean markNewLevel = newLevel;
			
			while(!boxItemGroup.isEmpty()) {
				
				IntermediatePlacementResult bestPoint;
				if(newLevel) {
					// get first box in new level
					bestPoint = findBestFirstPoint(filteredBoxItems, boxItemStartIndex, boxItemGroup.size(), priority, pointControls, container, extremePoints, stack);
					if(bestPoint == null) {
						break;
					}
					
					DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, bestPoint.getStackValue().getDz() - 1 + levelOffset);
					
					extremePoints.setPoints(Arrays.asList(levelFloor));
					extremePoints.clear();
					
					levelOffset += bestPoint.getStackValue().getDz();

					newLevel = false;
				} else {
					// next
					bestPoint = findBestPoint(filteredBoxItems, boxItemStartIndex, boxItemGroup.size(), priority, pointControls, container, extremePoints, stack);
					if(bestPoint == null) {
						newLevel = true;

						int remainingDz = container.getLoadDz() - levelOffset;
						if(remainingDz == 0) {
							break;
						}

						// prepare extreme points for a new level						
						DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, container.getLoadDz() - 1);
						extremePoints.setPoints(Arrays.asList(levelFloor));
						extremePoints.clear();
						
						// remove groups which have boxes which are too big for the max level size
						long maxArea = extremePoints.getMaxArea();
						long maxVolume = extremePoints.getMaxVolume();
						
						for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
							BoxItemGroup g = filteredBoxItemGroups.get(i);

							for(int k = 0; k < g.size(); k++) {
								BoxItem boxItem = g.get(k);
								Box box = boxItem.getBox();
								if(box.getVolume() > maxVolume || box.getMinimumArea() > maxArea) {

									if(abortOnAnyBoxTooBig) {
										return EmptyPackagerResultAdapter.EMPTY;
									}
									
									filteredBoxItemGroups.remove(i);
									i--;
									
									for(int l = 0; l < boxItemGroup.size(); l++) {
										boxItemControls.declined(boxItemGroup.get(l));
										if(pointControls != null) {
											pointControls.declined(boxItemGroup.get(l));
										}
									}
									
									if(boxItemGroupControls != null) {
										boxItemGroupControls.declined(boxItemGroup);
									}
									break;
								}
							}				
						}
						
						continue;
					}
				}
				
				StackPlacement stackPlacement = new StackPlacement(bestPoint.getStackValue(), bestPoint.getPoint().getMinX(), bestPoint.getPoint().getMinY(), bestPoint.getPoint().getMinZ());
				stack.add(stackPlacement);
				extremePoints.add(bestPoint.getPoint(), stackPlacement);
				
				remainingLoadWeight -= bestPoint.getBoxItem().getBox().getWeight();
				remainingLoadVolume -= bestPoint.getBoxItem().getBox().getVolume();
				
				// decrement box item without deleting the whole group
				packagerBoxItems.decrement(bestPoint.getIndex());
				
				boxItemControls.accepted(bestPoint.getBoxItem());
				if(pointControls != null) {
					pointControls.accepted(bestPoint.getBoxItem());
				}
				
				if(!filteredBoxItems.isEmpty()) {
					// remove groups are too big according to total volume / weight
					
					for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
						BoxItemGroup g = filteredBoxItemGroups.get(i);
						if(g.getVolume() > remainingLoadVolume || g.getWeight() > remainingLoadWeight) {
							
							if(abortOnAnyBoxTooBig) {
								return EmptyPackagerResultAdapter.EMPTY;
							}
							
							filteredBoxItemGroups.remove(i);
							i--;
							
							for(int k = 0; k < boxItemGroup.size(); k++) {
								boxItemControls.declined(boxItemGroup.get(k));
								if(pointControls != null) {
									pointControls.declined(boxItemGroup.get(k));
								}
							}
							
							if(boxItemGroupControls != null) {
								boxItemGroupControls.declined(boxItemGroup);
							}
						}
					}
					
					// remove small points
					extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());
				}
				
				if(!packagerBoxItems.contains(boxItemGroup)) {
					// the current group was removed, assume packaging unsuccessful.
					break;
				}
			}

			boolean removed = !packagerBoxItems.contains(boxItemGroup);
			if(!removed) {
				packagerBoxItems.remove(boxItemGroup);
			}
			
			if(removed || !boxItemGroup.isEmpty()) {				
				boxItemGroup.reset();

				if(abortOnAnyBoxTooBig) {
					return EmptyPackagerResultAdapter.EMPTY;
				}
				
				for(int i = markStackSize; i < stack.size(); i++) {
					boxItemControls.undo(stack.getPlacements().get(i).getStackValue().getBox().getBoxItem());
				}
				
				for(int k = 0; k < boxItemGroup.size(); k++) {
					boxItemControls.declined(boxItemGroup.get(k));
					if(pointControls != null) {
						pointControls.declined(boxItemGroup.get(k));
					}
				}
				
				if(boxItemGroupControls != null) {
					boxItemGroupControls.declined(boxItemGroup);
				}

				stack.setSize(markStackSize);
				
				// unable to stack whole group
				if(priority == Priority.NATURAL) {
					break groups;
				}
				// try again with another group if possible
				extremePoints.reset();
				
				levelOffset = markLevelOffset;
				newLevel = markNewLevel;

				continue groups;
			}
			
			if(container.getMaxLoadWeight() < extremePoints.calculateUsedWeight()) {
				throw new RuntimeException();
			}
			
			// successfully stacked group
			boxItemGroup.reset();
			
			if(boxItemGroupControls != null) {
				boxItemGroupControls.accepted(boxItemGroup);
			}
		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}
	
	protected BoxItemGroupIterator createBoxItemGroupIterator(FilteredBoxItemGroups filteredBoxItemGroups, Priority itemGroupOrder, Container container, ExtremePoints extremePoints) {
		if(itemGroupOrder == Priority.NATURAL || itemGroupOrder == Priority.NATURAL_ALLOW_SKIPPING) {
			return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints);
		}
		return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints, boxItemGroupComparator);
	}

	public IntermediatePlacementResult findBestPoint(FilteredBoxItems boxItems, int offset, int length, Priority priority, PointControls pointControls, Container container, ExtremePoints extremePoints, Stack stack) {
		return intermediatePlacementResultBuilderFactory.createIntermediatePlacementResultBuilder()
			.withContainer(container)
			.withExtremePoints(extremePoints)
			.withPriority(priority)
			.withStack(stack)
			.withBoxItems(boxItems, offset, length)
			.withPointControls(pointControls)
			.withIntermediatePlacementResultComparator(intermediatePlacementResultComparator)
			.withBoxItemComparator(boxItemComparator)
			.build();
	}
	
	public IntermediatePlacementResult findBestFirstPoint(FilteredBoxItems boxItems, int offset, int length, Priority priority, PointControls pointControls, Container container, ExtremePoints extremePoints, Stack stack) {
		return intermediatePlacementResultBuilderFactory.createIntermediatePlacementResultBuilder()
			.withContainer(container)
			.withExtremePoints(extremePoints)
			.withPriority(priority)
			.withStack(stack)
			.withBoxItems(boxItems, offset, length)
			.withPointControls(pointControls)
			.withIntermediatePlacementResultComparator(firstIntermediatePlacementResultComparator)
			.withBoxItemComparator(firstBoxItemComparator)
			.build();
	}
}
