package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.DefaultPackagerInterrupt;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupComparator;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.MarkResetPointCalculator3D;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.PackagerBoxItems;
import com.github.skjolber.packing.packer.AbstractBoxItemGroupSession;
import com.github.skjolber.packing.packer.AbstractBoxItemSession;
import com.github.skjolber.packing.packer.AbstractControlPackager;
import com.github.skjolber.packing.packer.ExtractionOrderSearch;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerInput;

/**
 * Fills each container level by level: the box with the largest ground area starts a level, then the remaining boxes are stacked within the level.
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */
public abstract class AbstractLargestAreaFitFirstPackager extends AbstractControlPackager<Placement, AbstractLargestAreaFitFirstPackager.LargestAreaFitFirstResultBuilder> {

	protected class LargestAreaFitFirstBoxItemSession extends AbstractBoxItemSession {

		public LargestAreaFitFirstBoxItemSession(List<BoxItem> boxItems, Order order, List<ContainerItem> containers, int containerCount, PackagerInterruptSupplier interrupt) {
			super(boxItems, order, containers, containerCount, interrupt);
		}

		private LargestAreaFitFirstBoxItemSession(LargestAreaFitFirstBoxItemSession source) {
			super(source);
		}

		@Override
		public PackagerSession fork() {
			return new LargestAreaFitFirstBoxItemSession(this);
		}

		@Override
		protected LargestAreaFitFirstBoxItemSession fresh(List<ContainerItem> containers, int containerCount) {
			return new LargestAreaFitFirstBoxItemSession(copyBoxItems(initialBoxItems), order, containers, containerCount, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig
				) throws PackagerInterruptedException {
			return AbstractLargestAreaFitFirstPackager.this.pack(remainingBoxItems, containerItem, interrupt, order, abortOnAnyBoxTooBig, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		}

		@Override
		protected IntermediatePackagerResult copy(ContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
			return createIntermediatePackagerResult(controlledContainerItem, result.getStack());
		}

	}
	
	protected class LargestAreaFitFirstBoxItemGroupSession extends AbstractBoxItemGroupSession {

		public LargestAreaFitFirstBoxItemGroupSession(List<BoxItemGroup> boxItemGroups,
				Order order,
				List<ContainerItem> containers,
				int containerCount, PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, containers, containerCount, order, interrupt);
		}

		private LargestAreaFitFirstBoxItemGroupSession(LargestAreaFitFirstBoxItemGroupSession source) {
			super(source);
		}

		@Override
		public PackagerSession fork() {
			return new LargestAreaFitFirstBoxItemGroupSession(this);
		}

		@Override
		protected LargestAreaFitFirstBoxItemGroupSession fresh(List<ContainerItem> containers, int containerCount) {
			return new LargestAreaFitFirstBoxItemGroupSession(copyBoxItemGroups(initialBoxItemGroups), order, containers, containerCount, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order,
				ContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return AbstractLargestAreaFitFirstPackager.this.packGroup(remainingBoxItemGroups, order, containerItem, interrupt, abortOnAnyBoxTooBig, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		}

		@Override
		protected IntermediatePackagerResult copy(ContainerItem controlledContainerItem, IntermediatePackagerResult result, int index) {
			return createIntermediatePackagerResult(controlledContainerItem, result.getStack());
		}

	}

	@Override
	protected PackagerSession newSession(PackagerInput input, PackagerInterruptSupplier interrupt) {
		if(input.hasBoxItems()) {
			return new LargestAreaFitFirstBoxItemSession(input.getBoxItems(), input.getOrder(), input.getContainerItems(), input.getMaxContainerCount(), interrupt);
		}
		return new LargestAreaFitFirstBoxItemGroupSession(input.getBoxItemGroups(), input.getOrder(), input.getContainerItems(), input.getMaxContainerCount(), interrupt);
	}

	public class LargestAreaFitFirstResultBuilder extends AbstractPackagerResultBuilder<LargestAreaFitFirstResultBuilder> {

		@Override
		public PackagerResult build() {
			return pack(validate(AbstractLargestAreaFitFirstPackager.this), deadline, interrupt);
		}
	}
	
	// intermediatePlacementResultBuilderFactory = new ComparatorIntermediatePlacementControlsBuilderFactory();
	protected PlacementControlsBuilderFactory placementControlsBuilderFactory;
	protected PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory;
	
	protected BoxItemGroupComparator boxItemGroupComparator;
	
	public AbstractLargestAreaFitFirstPackager(IntermediatePackagerResultComparator comparator, BoxItemGroupComparator boxItemGroupComparator, PlacementControlsBuilderFactory placementControlsBuilderFactory, PlacementControlsBuilderFactory firstPlacementControlsBuilderFactory) {
		super(comparator);

		this.firstPlacementControlsBuilderFactory = firstPlacementControlsBuilderFactory;
		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		
		this.boxItemGroupComparator = boxItemGroupComparator;
	}

	public IntermediatePackagerResult pack(List<BoxItem> boxItems, ContainerItem controlledContainerItem, PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean maxLoadIdenticalBoxCount) throws PackagerInterruptedException {
		ContainerItem containerItem = controlledContainerItem;
		Container container = containerItem.getContainer();

		Stack stack = new Stack();

		// container priorities: the items of one priority at a time, see getContainerPriorityEnd(..)
		boolean containerPriorities = hasContainerPriorities(boxItems);
		int maxContainerPriority = Integer.MAX_VALUE;
		DefaultBoxItemSource filteredBoxItems = new DefaultBoxItemSource(sortByRanks(boxItems, order));
		ExtractionOrderSearch extractionOrderSearch = createExtractionOrderSearch(boxItems, order);

		PointCalculator pointCalculator = createPointCalculator(filteredBoxItems);
		
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(controlledContainerItem.hasInitialPoints()) {
			pointCalculator.setPoints(controlledContainerItem.getInitialPoints());
			pointCalculator.clear();
		}

		ManifestControls manifestControls = createManifestControls(container, stack, filteredBoxItems, pointCalculator, null, controlledContainerItem.getManifestControlsBuilderFactory());

		PointControlsBuilderFactory pointControlsBuilderFactory = controlledContainerItem.getPointControlsBuilderFactory();
		if(pointControlsBuilderFactory == null) {
			pointControlsBuilderFactory = new DefaultPointControlsBuilderFactory();
		}

		PointControls pointControls = createPointControls(container, stack, filteredBoxItems, pointCalculator, pointControlsBuilderFactory, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		// remove boxes which do not fit due to volume, weight or dimensions
		List<BoxItem> removed = new ArrayList<>();
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			BoxItem boxItem = filteredBoxItems.get(i);
			if(!container.canLoad(boxItem.getBox())) {

				if(abortOnAnyBoxTooBig) {
					return EmptyIntermediatePackagerResult.EMPTY;
				}
				
				if(order != Order.CHRONOLOGICAL) {
					removed.add(filteredBoxItems.remove(i));
					i--;
				} else {
					// remove all later then the first removed
					while(i < filteredBoxItems.size()) {
						removed.add(filteredBoxItems.remove(i));
					}
				}
			}
		}
		
		if(!removed.isEmpty()) {
			manifestControls.declined(removed);
			pointControls.declined(removed);
			maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);
			
			removed.clear();
		}
		
		pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		long maxBoxVolume = filteredBoxItems.getMaxVolume();
		int maxBoxWeight = getMaxBoxWeight(filteredBoxItems);

		int levelOffset = 0;
		boolean newLevel = true;

		// the current level: its floor, its first placement, and whether it was raised to the top of the container
		int levelFloor = 0;
		int levelStart = 0;
		boolean levelRaised = false;

		PlacementControls placementControls = createControls(filteredBoxItems, order, pointControls, container, pointCalculator, stack, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		PlacementControls firstPlacementControls = createFirstControls(filteredBoxItems, 0, filteredBoxItems.size(), order, pointControls, container, pointCalculator, stack, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);

		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			Placement result;
			if(newLevel) {
				// get first box in new level
				int end = containerPriorities ? getContainerPriorityEnd(filteredBoxItems, maxContainerPriority) : filteredBoxItems.size();
				if(end == 0) {
					break;
				}
				if(extractionOrderSearch != null) {
					// each level starts with the boxes which are extracted last
					extractionOrderSearch.reset();
					result = extractionOrderSearch.getPlacement(firstPlacementControls, filteredBoxItems, end);
				} else {
					result = firstPlacementControls.getPlacement(0, end);
				}
				if(result == null) {
					// no box fits a new level: raise the level below, so that a box taller than it can stand beside its boxes
					if(!levelRaised && levelStart < stack.size()
							&& setRaisedLevelPoints(pointCalculator, controlledContainerItem, container, levelFloor, stack.getPlacements(), levelStart, filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume())) {
						levelRaised = true;
						newLevel = false;
						continue;
					}
					break;
				}
				
				// best placement may not be at the current level offset
				// keep all points between the level floor and the top of the target placement

				if(!setLevelPoints(pointCalculator, controlledContainerItem, container, levelOffset, result.getAbsoluteEndZ())) {
					// no more points
					break;
				}

				// the points were reset for the level: the first placement's point index refers to the previous points
				int pointIndex = findPointIndex(pointCalculator, result);
				if(pointIndex == -1) {
					break;
				}
				result.setPoint(pointIndex, result.getAbsoluteX(), result.getAbsoluteY(), result.getAbsoluteZ());
				
				levelFloor = levelOffset;
				levelStart = stack.size();
				levelRaised = false;
				levelOffset = result.getAbsoluteEndZ() + 1;

				newLevel = false;
			} else {
				// next
				int end = containerPriorities ? getContainerPriorityEnd(filteredBoxItems, maxContainerPriority) : filteredBoxItems.size();
				if(end == 0) {
					result = null;
				} else {
					result = extractionOrderSearch != null ? extractionOrderSearch.getPlacement(placementControls, filteredBoxItems, end) : placementControls.getPlacement(0, end);
				}
				if(result == null) {
					newLevel = true;

					int remainingDz = container.getLoadDz() - levelOffset;
					if(remainingDz == 0) {
						break;
					}

					// prepare points for a new level
					if(!setLevelPoints(pointCalculator, controlledContainerItem, container, levelOffset, container.getLoadDz() - 1)) {
						// no more points
						break;
					}
					
					// remove boxes which are too big for the max new level (and for the level below, if it can be raised)
					long maxArea = getMaxLevelArea(pointCalculator, container, levelRaised);
					long maxVolume = getMaxLevelVolume(pointCalculator, container, levelRaised, levelFloor);
					
					for(int i = 0; i < filteredBoxItems.size(); i++) {
						BoxItem boxItem = filteredBoxItems.get(i);
						Box box = boxItem.getBox();
						if(box.getVolume() > maxVolume || box.getMinimumArea() > maxArea) {
							if(abortOnAnyBoxTooBig) {
								return EmptyIntermediatePackagerResult.EMPTY;
							}
							
							if(order != Order.CHRONOLOGICAL) {
								removed.add(filteredBoxItems.remove(i));
								i--;
							} else {
								// remove all later then the first removed
								while(i < filteredBoxItems.size()) {
									removed.add(filteredBoxItems.remove(i));
								}					
							}
						}
					}
					
					if(!removed.isEmpty()) {
						manifestControls.declined(removed);
						pointControls.declined(removed);
						maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);
						
						removed.clear();
					}
					
					continue;
				}
			}
			stack.add(result);
			pointCalculator.add(result.getPointIndex(), result);
			// a box in a raised level can be taller than the level
			levelOffset = Math.max(levelOffset, result.getAbsoluteEndZ() + 1);
			
			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
			remainingLoadVolume -= result.getBoxItem().getBox().getVolume();
			
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING && removeSkippedBoxItems(filteredBoxItems, result.getBoxItem(), removed)) {
				manifestControls.declined(removed);
				pointControls.declined(removed);
				maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);

				removed.clear();
			}
			filteredBoxItems.decrement(result.getBoxItem().getLocalIndex(), 1);

			manifestControls.accepted(result.getBoxItem());
			pointControls.accepted(result.getBoxItem());
			
			placementControls.accepted(result);
			
			if(!filteredBoxItems.isEmpty()) {
				// remove items are too big according to total volume / weight
				// (the items only shrink, so nothing can be too big while the remaining capacity holds the largest initial item)
				if(remainingLoadVolume < maxBoxVolume || remainingLoadWeight < maxBoxWeight) {
					for(int i = 0; i < filteredBoxItems.size(); i++) {
						BoxItem boxItem = filteredBoxItems.get(i);
						Box box = boxItem.getBox();
						if(box.getVolume() > remainingLoadVolume || box.getWeight() > remainingLoadWeight) {
							
							if(abortOnAnyBoxTooBig) {
								return EmptyIntermediatePackagerResult.EMPTY;
							}
							
							if(order != Order.CHRONOLOGICAL) {
								removed.add(filteredBoxItems.remove(i));
								i--;
							} else {
								// remove all later then the first removed
								while(i < filteredBoxItems.size()) {
									removed.add(filteredBoxItems.remove(i));
								}					
							}
						}
					}
					
					if(!removed.isEmpty()) {
						manifestControls.declined(removed);
						pointControls.declined(removed);
						maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);
						
						removed.clear();
					}
				}
				
				
				// remove small points
				pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());				
			}
		}
		
		// ignore decline for the rest
		
		return new DefaultIntermediatePackagerResult(controlledContainerItem, stack);
	}

	protected abstract PointCalculator createPointCalculator(BoxItemSource source);

	public IntermediatePackagerResult packGroup(List<BoxItemGroup> boxItemGroups, Order order, ContainerItem controlledContainerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean maxLoadIdenticalBoxCount) throws PackagerInterruptedException {
		ContainerItem containerItem = controlledContainerItem;
		Container container = containerItem.getContainer();
		
		Stack stack = new Stack();

		// container priorities: the groups of one priority at a time
		boolean containerPriorities = hasGroupContainerPriorities(boxItemGroups);
		int maxContainerPriority = Integer.MAX_VALUE;
		PackagerBoxItems packagerBoxItems = new PackagerBoxItems(boxItemGroups);
		BoxItemSource filteredBoxItems = packagerBoxItems.getFilteredBoxItems();

		MarkResetPointCalculator3D pointCalculator = new MarkResetPointCalculator3D(true, filteredBoxItems);
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(controlledContainerItem.hasInitialPoints()) {
			pointCalculator.setPoints(controlledContainerItem.getInitialPoints());
			pointCalculator.clear();
		}

		BoxItemGroupSource filteredBoxItemGroups = packagerBoxItems.getFilteredBoxItemGroups();

		ManifestControls manifestControls = createManifestControls(container, stack, filteredBoxItems, pointCalculator, filteredBoxItemGroups, controlledContainerItem.getManifestControlsBuilderFactory());

		PointControlsBuilderFactory pointControlsBuilderFactory = controlledContainerItem.getPointControlsBuilderFactory();
		if(pointControlsBuilderFactory == null) {
			pointControlsBuilderFactory = new DefaultPointControlsBuilderFactory();
		}

		PointControls pointControls = createPointControls(container, stack, filteredBoxItems, pointCalculator, pointControlsBuilderFactory, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
				
		List<BoxItemGroup> removedBoxItemGroups = new ArrayList<>();

		if(order != Order.CHRONOLOGICAL) {
	
			// remove boxes which do not fit due to volume, weight or stack value dimensions
			for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
				BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
				if(!container.canLoadAtLeastOneBox(boxItemGroup)) {
					if(abortOnAnyBoxTooBig) {
						return EmptyIntermediatePackagerResult.EMPTY;
					}
					if(order != Order.CHRONOLOGICAL) {
						filteredBoxItemGroups.remove(i);
						i--;
						
						removedBoxItemGroups.add(boxItemGroup);
					} else {
						// remove all later groups than the first removed
						while(i < filteredBoxItemGroups.size()) {
							removedBoxItemGroups.add(filteredBoxItemGroups.remove(i));
						}
					}		
				}
			}
			
			if(!removedBoxItemGroups.isEmpty()) {
				manifestControls.filteredGroups(removedBoxItemGroups);
				pointControls.filteredGroups(removedBoxItemGroups);
				maxContainerPriority = getMaxGroupContainerPriority(maxContainerPriority, removedBoxItemGroups);
				removedBoxItemGroups.clear();
			}
		}
		
		pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		
		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, order, container, pointCalculator);

		int levelOffset = 0;
		boolean newLevel = true;

		// the current level: its floor, its first placement, and whether it was raised to the top of the container
		int levelFloor = 0;
		int levelStart = 0;
		boolean levelRaised = false;

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		long maxGroupVolume = getMaxGroupVolume(filteredBoxItemGroups);
		long maxGroupWeight = getMaxGroupWeight(filteredBoxItemGroups);

		PlacementControls placementControls = createControls(filteredBoxItems, order, pointControls, container, pointCalculator, stack, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		PlacementControls firstPlacementControls = createFirstControls(filteredBoxItems, 0, filteredBoxItems.size(), order, pointControls, container, pointCalculator, stack, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
		groups:
		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !pointCalculator.isEmpty() && boxItemGroupIterator.hasNext() && !filteredBoxItemGroups.isEmpty()) {
			int groupIndex = boxItemGroupIterator.next();
			
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(groupIndex);
			if(containerPriorities && boxItemGroup.getContainerPriority() > Math.min(maxContainerPriority, getMinGroupContainerPriority(filteredBoxItemGroups))) {
				// a group of a lower container priority is not placed in this container
				break groups;
			}
			boxItemGroup.mark();
			
			pointCalculator.mark();
			int markStackSize = stack.size();
			
			int markLevelOffset = levelOffset;
			boolean markNewLevel = newLevel;
			int markLevelFloor = levelFloor;
			int markLevelStart = levelStart;
			boolean markLevelRaised = levelRaised;

			manifestControls.attempt(boxItemGroup, packagerBoxItems.getFirstBoxItemIndex(boxItemGroup), boxItemGroup.size());
			
			while(!boxItemGroup.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				// groups before this one may have been removed
				int boxItemStartIndex = packagerBoxItems.getFirstBoxItemIndex(boxItemGroup);
				if(boxItemStartIndex == -1) {
					break;
				}
				
				Placement bestPoint;
				if(newLevel) {
					// get first box in new level
					bestPoint = firstPlacementControls.getPlacement(boxItemStartIndex, boxItemGroup.size());
					if(bestPoint == null) {
						// no box fits a new level: raise the level below, so that a box taller than it can stand beside its boxes
						if(!levelRaised && levelStart < stack.size()
								&& setRaisedLevelPoints(pointCalculator, controlledContainerItem, container, levelFloor, stack.getPlacements(), levelStart, filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume())) {
							levelRaised = true;
							newLevel = false;
							continue;
						}
						break;
					}
					
					// best placement may not be at the current level offset (obstacles etc)
					// keep all points between the level floor and the top of the target placement
					if(!setLevelPoints(pointCalculator, controlledContainerItem, container, levelOffset, bestPoint.getAbsoluteEndZ())) {
						break;
					}

					// the points were reset for the level: the first placement's point index refers to the previous points
					int pointIndex = findPointIndex(pointCalculator, bestPoint);
					if(pointIndex == -1) {
						break;
					}
					bestPoint.setPoint(pointIndex, bestPoint.getAbsoluteX(), bestPoint.getAbsoluteY(), bestPoint.getAbsoluteZ());

					levelFloor = levelOffset;
					levelStart = stack.size();
					levelRaised = false;
					levelOffset = bestPoint.getAbsoluteEndZ() + 1;

					newLevel = false;
				} else {
					// next
					bestPoint = placementControls.getPlacement(boxItemStartIndex, boxItemGroup.size());
					if(bestPoint == null) {
						newLevel = true;

						int remainingDz = container.getLoadDz() - levelOffset;
						if(remainingDz == 0) {
							break;
						}

						// prepare points for a new level
						if(!setLevelPoints(pointCalculator, controlledContainerItem, container, levelOffset, container.getLoadDz() - 1)) {
							// no more points
							break;
						}
						
						// remove groups which have boxes which are too big for the max level size (and for the level below, if it can be raised)
						long maxArea = getMaxLevelArea(pointCalculator, container, levelRaised);
						long maxVolume = getMaxLevelVolume(pointCalculator, container, levelRaised, levelFloor);
						
						for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
							BoxItemGroup g = filteredBoxItemGroups.get(i);

							for(int k = 0; k < g.size(); k++) {
								BoxItem boxItem = g.get(k);
								Box box = boxItem.getBox();
								if(box.getVolume() > maxVolume || box.getMinimumArea() > maxArea) {

									if(abortOnAnyBoxTooBig) {
										return EmptyIntermediatePackagerResult.EMPTY;
									}
									
									if(order != Order.CHRONOLOGICAL) {
										filteredBoxItemGroups.remove(i);
										i--;
										
										removedBoxItemGroups.add(g);
									} else {
										// remove all later groups than the first removed
										while(i < filteredBoxItemGroups.size()) {
											removedBoxItemGroups.add(filteredBoxItemGroups.remove(i));
										}
									}
									break;
								}
							}				
						}
						
						if(!removedBoxItemGroups.isEmpty()) {
							manifestControls.filteredGroups(removedBoxItemGroups);
							pointControls.filteredGroups(removedBoxItemGroups);
							maxContainerPriority = getMaxGroupContainerPriority(maxContainerPriority, removedBoxItemGroups);
							removedBoxItemGroups.clear();
						}
						
						continue;
					}
				}
				
				stack.add(bestPoint);
				pointCalculator.add(bestPoint.getPointIndex(), bestPoint);
				// a box in a raised level can be taller than the level
				levelOffset = Math.max(levelOffset, bestPoint.getAbsoluteEndZ() + 1);
				
				remainingLoadWeight -= bestPoint.getBoxItem().getBox().getWeight();
				remainingLoadVolume -= bestPoint.getBoxItem().getBox().getVolume();
				
				// decrement box item without deleting the whole group
				packagerBoxItems.decrement(bestPoint.getBoxItem().getLocalIndex());

				manifestControls.accepted(bestPoint.getBoxItem());
				pointControls.accepted(bestPoint.getBoxItem());
				
				placementControls.accepted(bestPoint);

				if(!filteredBoxItems.isEmpty()) {
					// remove groups are too big according to total volume / weight
					// (the groups only shrink, so nothing can be too big while the remaining capacity holds the largest initial group)
					if(remainingLoadVolume < maxGroupVolume || remainingLoadWeight < maxGroupWeight) {
						for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
							BoxItemGroup g = filteredBoxItemGroups.get(i);
							if(g.getVolume() > remainingLoadVolume || g.getWeight() > remainingLoadWeight) {
								
								if(abortOnAnyBoxTooBig) {
									return EmptyIntermediatePackagerResult.EMPTY;
								}
								
								if(order != Order.CHRONOLOGICAL) {
									filteredBoxItemGroups.remove(i);
									i--;
									
									removedBoxItemGroups.add(g);
								} else {
									// remove all later groups than the first removed
									while(i < filteredBoxItemGroups.size()) {
										removedBoxItemGroups.add(filteredBoxItemGroups.remove(i));
									}
								}
							}
						}
						
						if(!removedBoxItemGroups.isEmpty()) {
							manifestControls.filteredGroups(removedBoxItemGroups);
							pointControls.filteredGroups(removedBoxItemGroups);
							maxContainerPriority = getMaxGroupContainerPriority(maxContainerPriority, removedBoxItemGroups);
							removedBoxItemGroups.clear();
						}
					}
					
					// remove small points
					pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());
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
					return EmptyIntermediatePackagerResult.EMPTY;
				}

				List<Placement> removedBoxPlacements = stack.getPlacements().subList(markStackSize, stack.size());
				if(!removedBoxPlacements.isEmpty()) {
					List<BoxItem> removedBoxItems = new ArrayList<>();
					for(Placement p : removedBoxPlacements) {
						removedBoxItems.add(p.getBoxItem());
					}

					manifestControls.undo(removedBoxItems);
					pointControls.undo(removedBoxItems);
					
					placementControls.undo(removedBoxPlacements);
					firstPlacementControls.undo(removedBoxPlacements);
					
					removedBoxItems.clear();
				}
				
				manifestControls.attemptFailure(boxItemGroup);
				pointControls.attemptFailure(boxItemGroup);
				
				stack.setSize(markStackSize);
				// the group waits for the next container, and so do the groups of higher container priorities
				maxContainerPriority = Math.min(maxContainerPriority, boxItemGroup.getContainerPriority());
				
				// unable to stack whole group
				if(order == Order.CHRONOLOGICAL) {
					break groups;
				}
				// try again with another group if possible
				pointCalculator.reset();
				
				levelOffset = markLevelOffset;
				newLevel = markNewLevel;
				levelFloor = markLevelFloor;
				levelStart = markLevelStart;
				levelRaised = markLevelRaised;

				continue groups;
			}
			
			if(container.getMaxLoadWeight() < pointCalculator.calculateUsedWeight()) {
				throw new RuntimeException();
			}
			
			// successfully stacked group
			boxItemGroup.reset();
			
			manifestControls.attemptSuccess(boxItemGroup);
			pointControls.attemptSuccess(boxItemGroup);
		}
		
		return new DefaultIntermediatePackagerResult(controlledContainerItem, stack);
	}
	
	/**
	 * @return the index of the point at the placement's position which can hold it, or -1 if none
	 */
	/**
	 * Set the free points of a level: the space from the level offset up to a height, less the container's obstacles.
	 *
	 * @param maxZ the top of the level
	 * @return false if there is no free space
	 */
	protected static boolean setLevelPoints(PointCalculator pointCalculator, ContainerItem containerItem, Container container, int levelOffset, int maxZ) {
		if(containerItem.hasInitialPoints()) {
			// account for obstacles etc
			if(!pointCalculator.setPoints(containerItem.getInitialPoints(), 0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, maxZ)) {
				return false;
			}
		} else {
			DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, maxZ);
			pointCalculator.setPoints(Arrays.asList(levelFloor));
		}
		pointCalculator.clear();
		return true;
	}

	/**
	 * Raise a level to the top of the container: set the free points of the space from the level's floor up to the top
	 * of the container, with the level's boxes in place. A box which is taller than the level, and which does not fit
	 * a new level on top of it, can then stand beside the level's boxes.
	 *
	 * @param levelFloor the bottom of the level
	 * @param placements the placements of the container
	 * @param levelStart the index of the level's first placement
	 * @param minArea the minimum area of the remaining boxes
	 * @param minVolume the minimum volume of the remaining boxes
	 * @return false if there is no free space
	 */
	protected static boolean setRaisedLevelPoints(PointCalculator pointCalculator, ContainerItem containerItem, Container container, int levelFloor, List<Placement> placements,
			int levelStart, long minArea, long minVolume) {
		// keep the points of the level's boxes while they are added again
		pointCalculator.setMinimumAreaAndVolumeLimit(0, 0);
		if(!setLevelPoints(pointCalculator, containerItem, container, levelFloor, container.getLoadDz() - 1)) {
			return false;
		}
		for (int i = levelStart; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			int pointIndex = findPointIndex(pointCalculator, placement);
			if(pointIndex == -1) {
				return false;
			}
			pointCalculator.add(pointIndex, placement);
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(minArea, minVolume);
		return !pointCalculator.isEmpty();
	}

	/**
	 * @return the largest area of a box which can be placed in a new level, or in the level below raised to the top of
	 *         the container
	 */
	protected static long getMaxLevelArea(PointCalculator pointCalculator, Container container, boolean levelRaised) {
		long maxArea = pointCalculator.getMaxArea();
		if(!levelRaised) {
			maxArea = Math.max(maxArea, (long)container.getLoadDx() * container.getLoadDy());
		}
		return maxArea;
	}

	/**
	 * @return the largest volume of a box which can be placed in a new level, or in the level below raised to the top
	 *         of the container
	 */
	protected static long getMaxLevelVolume(PointCalculator pointCalculator, Container container, boolean levelRaised, int levelFloor) {
		long maxVolume = pointCalculator.getMaxVolume();
		if(!levelRaised) {
			maxVolume = Math.max(maxVolume, (long)container.getLoadDx() * container.getLoadDy() * (container.getLoadDz() - levelFloor));
		}
		return maxVolume;
	}

	protected static int findPointIndex(PointCalculator pointCalculator, Placement placement) {
		int index = placement.getPointIndex();
		if(index >= 0 && index < pointCalculator.size() && isPointOf(pointCalculator.get(index), placement)) {
			return index;
		}
		for (int i = 0; i < pointCalculator.size(); i++) {
			if(isPointOf(pointCalculator.get(i), placement)) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isPointOf(Point point, Placement placement) {
		return point.getMinX() == placement.getAbsoluteX() && point.getMinY() == placement.getAbsoluteY() && point.getMinZ() == placement.getAbsoluteZ()
				&& point.fits3D(placement.getStackValue());
	}

	protected BoxItemGroupIterator createBoxItemGroupIterator(BoxItemGroupSource filteredBoxItemGroups, Order itemGroupOrder, Container container, PointCalculator pointCalculator) {
		if(itemGroupOrder == Order.CHRONOLOGICAL || itemGroupOrder == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
			return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, pointCalculator);
		}
		return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, pointCalculator, boxItemGroupComparator);
	}

	@Override
	protected PlacementControls createControls(BoxItemSource boxItems, Order order, PointControls pointControls,
			Container container, PointCalculator pointCalculator, Stack stack, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean maxLoadIdenticalBoxCount) {
		
		return placementControlsBuilderFactory.createPlacementControlsBuilder()
				.withPointCalculator(pointCalculator)
				.withBoxItems(boxItems)
				.withPointControls(pointControls)
				.withOrder(order)
				.withStack(stack)
				.withContainer(container)
				.withMaxLoad(maxLoadWeight, maxLoadPressure, maxLoadBoxCount)
				.withLoadIdenticalBox(maxLoadIdenticalBoxCount)
				.build();
	}

	public PlacementControls createFirstControls(BoxItemSource boxItems, int offset, int length, Order order, PointControls pointControls, Container container, PointCalculator pointCalculator, Stack stack, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean maxLoadIdenticalBoxCount) {
		return firstPlacementControlsBuilderFactory.createPlacementControlsBuilder()
			.withContainer(container)
			.withPointCalculator(pointCalculator)
			.withOrder(order)
			.withStack(stack)
			.withBoxItems(boxItems)
			.withPointControls(pointControls)
			.withMaxLoad(maxLoadWeight, maxLoadPressure, maxLoadBoxCount)
			.withLoadIdenticalBox(maxLoadIdenticalBoxCount)
			.build();
	}
	
	@Override
	protected IntermediatePackagerResult createIntermediatePackagerResult(ContainerItem containerItem, Stack stack) {
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	@Override
	protected IntermediatePackagerResult createEmptyIntermediatePackagerResult() {
		return EmptyIntermediatePackagerResult.EMPTY;
	}

	@Override
	public LargestAreaFitFirstResultBuilder newResultBuilder() {
		return new LargestAreaFitFirstResultBuilder();
	}

	@Override
	public String getUnsupportedReason(PackagerInput input) {
		String reason = super.getUnsupportedReason(input);
		if(reason != null) {
			return reason;
		}
		if((!placementControlsBuilderFactory.supportsLoad() || !firstPlacementControlsBuilderFactory.supportsLoad()) && hasLoadLimits(input)) {
			return "Load limits not supported by the placement controls";
		}
		return null;
	}
}
