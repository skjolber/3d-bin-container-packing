package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.MarkResetPointCalculator3D;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.PackagerBoxItems;
import com.github.skjolber.packing.packer.AbstractBoxItemAdapter;
import com.github.skjolber.packing.packer.AbstractBoxItemGroupAdapter;
import com.github.skjolber.packing.packer.AbstractControlPackager;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyPackagerResultAdapter;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractLargestAreaFitFirstPackager extends AbstractControlPackager<Placement, IntermediatePackagerResult, AbstractLargestAreaFitFirstPackager.LargestAreaFitFirstResultBuilder> {

	protected class PlainBoxItemAdapter extends AbstractBoxItemAdapter<IntermediatePackagerResult> {

		public PlainBoxItemAdapter(List<BoxItem> boxItems, Order order,
				ContainerItemsCalculator packagerContainerItems,
				PackagerInterruptSupplier interrupt) {
			super(boxItems, order, packagerContainerItems, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ControlledContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return AbstractLargestAreaFitFirstPackager.this.pack(remainingBoxItems, containerItem, interrupt, order, abortOnAnyBoxTooBig);
		}

	}
	
	protected class PlainBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter<IntermediatePackagerResult> {

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups,
				Order order,
				ContainerItemsCalculator packagerContainerItems, 
				PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, packagerContainerItems, order, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order,
				ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
			return AbstractLargestAreaFitFirstPackager.this.packGroup(remainingBoxItemGroups, order, containerItem, interrupt, abortOnAnyBoxTooBig);
		}

	}

	public class LargestAreaFitFirstResultBuilder extends AbstractPackagerResultBuilder<LargestAreaFitFirstResultBuilder> {

		@Override
		public PackagerResult build() {
			validate();
			
			if( (items == null || items.isEmpty()) && (itemGroups == null || itemGroups.isEmpty())) {
				throw new IllegalStateException();
			}
			long start = System.currentTimeMillis();

			PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
			if(deadline != -1L) {
				booleanSupplierBuilder.withDeadline(deadline);
			}
			if(interrupt != null) {
				booleanSupplierBuilder.withInterrupt(interrupt);
			}

			booleanSupplierBuilder.withScheduledThreadPoolExecutor(getScheduledThreadPoolExecutor());

			PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
			try {
				PackagerAdapter<IntermediatePackagerResult> adapter;
				if(items != null && !items.isEmpty()) {
					adapter = new PlainBoxItemAdapter(items, order, new ContainerItemsCalculator(containers), interrupt);
				} else {
					adapter = new PlainBoxItemGroupAdapter(itemGroups, order, new ContainerItemsCalculator(containers), interrupt);
				}
				List<Container> packList = packAdapter(maxContainerCount, interrupt, adapter);
				
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(packList, duration, false);
			} catch (PackagerInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(Collections.emptyList(), duration, true);
			} finally {
				interrupt.close();
			}
		}
	}
	
	// intermediatePlacementResultBuilderFactory = new ComparatorIntermediatePlacementControlsBuilderFactory();
	protected PlacementControlsBuilderFactory<Placement> placementControlsBuilderFactory;
	protected PlacementControlsBuilderFactory<Placement> firstPlacementControlsBuilderFactory;
	
	protected Comparator<BoxItemGroup> boxItemGroupComparator;
	protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
	
	public AbstractLargestAreaFitFirstPackager(Comparator<IntermediatePackagerResult> comparator, List<Point> points, Comparator<BoxItemGroup> boxItemGroupComparator, Comparator<BoxItemGroup> firstBoxItemGroupComparator, PlacementControlsBuilderFactory<Placement> placementControlsBuilderFactory, PlacementControlsBuilderFactory<Placement> firstPlacementControlsBuilderFactory) {
		super(comparator, points);

		this.firstPlacementControlsBuilderFactory = firstPlacementControlsBuilderFactory;
		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		
		this.boxItemGroupComparator = boxItemGroupComparator;
		this.firstBoxItemGroupComparator = firstBoxItemGroupComparator;
	}

	public IntermediatePackagerResult pack(List<BoxItem> boxItems, ControlledContainerItem controlledContainerItem, PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		ContainerItem containerItem = controlledContainerItem;
		Container container = containerItem.getContainer();

		Stack stack = new Stack();

		PointCalculator pointCalculator = createPointCalculator();
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(controlledContainerItem.hasPoints()) {
			pointCalculator.setPoints(controlledContainerItem.getInitialPoints());
			pointCalculator.clear();
		}

		DefaultBoxItemSource filteredBoxItems = new DefaultBoxItemSource(boxItems);
		ManifestControls boxItemControls = controlledContainerItem.createBoxItemControls(container, stack, filteredBoxItems, pointCalculator, null);

		PointControls pointControls = controlledContainerItem.createPointControls(container, stack, filteredBoxItems, pointCalculator);
		// remove boxes which do not fit due to volume, weight or dimensions
		List<BoxItem> removed = new ArrayList<>();
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			BoxItem boxItem = filteredBoxItems.get(i);
			if(!container.fitsInside(boxItem.getBox())) {

				if(abortOnAnyBoxTooBig) {
					return EmptyPackagerResultAdapter.EMPTY;
				}
				
				if(order != Order.CRONOLOGICAL) {
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
			boxItemControls.declined(removed);
			pointControls.declined(removed);
			
			removed.clear();
		}
		
		pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		int levelOffset = 0;
		boolean newLevel = true;

		PlacementControls<Placement> placementControls = createControls(filteredBoxItems, 0, filteredBoxItems.size(), order, pointControls, container, pointCalculator, stack);
		PlacementControls<Placement> firstPlacementControls = createFirstControls(filteredBoxItems, 0, filteredBoxItems.size(), order, pointControls, container, pointCalculator, stack);

		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			Placement result;
			if(newLevel) {
				// get first box in new level
				result = firstPlacementControls.getPlacement(0, filteredBoxItems.size());
				if(result == null) {
					break;
				}
				
				DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, result.getStackValue().getDz() - 1 + levelOffset);
				
				pointCalculator.setPoints(Arrays.asList(levelFloor));
				pointCalculator.clear();
				
				levelOffset += result.getStackValue().getDz();

				newLevel = false;
			} else {
				// next
				result = placementControls.getPlacement(0, filteredBoxItems.size());
				if(result == null) {
					newLevel = true;

					int remainingDz = container.getLoadDz() - levelOffset;
					if(remainingDz == 0) {
						break;
					}

					// prepare points for a new level						
					DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, container.getLoadDz() - 1);
					pointCalculator.setPoints(Arrays.asList(levelFloor));
					pointCalculator.clear();
					
					// remove boxes which are too big for the max new level
					long maxArea = pointCalculator.getMaxArea();
					long maxVolume = pointCalculator.getMaxVolume();
					
					for(int i = 0; i < filteredBoxItems.size(); i++) {
						BoxItem boxItem = filteredBoxItems.get(i);
						Box box = boxItem.getBox();
						if(box.getVolume() > maxVolume || box.getMinimumArea() > maxArea) {
							if(abortOnAnyBoxTooBig) {
								return EmptyPackagerResultAdapter.EMPTY;
							}
							
							if(order != Order.CRONOLOGICAL) {
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
						boxItemControls.declined(removed);
						pointControls.declined(removed);
						
						removed.clear();
					}
					
					continue;
				}
			}
			
			stack.add(result);
			pointCalculator.add(result.getPoint(), result);
			
			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
			remainingLoadVolume -= result.getBoxItem().getBox().getVolume();
			
			filteredBoxItems.decrement(result.getBoxItem().getIndex(), 1);

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
						
						if(order != Order.CRONOLOGICAL) {
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
					boxItemControls.declined(removed);
					pointControls.declined(removed);
					
					removed.clear();
				}
				
				
				// remove small points
				pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());				
			}
		}
		
		// ignore decline for the rest
		
		return new DefaultIntermediatePackagerResult(controlledContainerItem, stack);
	}

	protected abstract PointCalculator createPointCalculator();

	public IntermediatePackagerResult packGroup(List<BoxItemGroup> boxItemGroups, Order order, ControlledContainerItem controlledContainerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
		ContainerItem containerItem = controlledContainerItem;
		Container container = containerItem.getContainer();
		
		Stack stack = new Stack();

		MarkResetPointCalculator3D pointCalculator = new MarkResetPointCalculator3D(true);
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(controlledContainerItem.hasPoints()) {
			pointCalculator.setPoints(controlledContainerItem.getInitialPoints());
			pointCalculator.clear();
		}
		PackagerBoxItems packagerBoxItems = new PackagerBoxItems(boxItemGroups);

		BoxItemSource filteredBoxItems = packagerBoxItems.getFilteredBoxItems();

		BoxItemGroupSource filteredBoxItemGroups = packagerBoxItems.getFilteredBoxItemGroups();

		ManifestControls boxItemControls = controlledContainerItem.createBoxItemControls(container, stack, filteredBoxItems, pointCalculator, filteredBoxItemGroups);

		PointControls pointControls = controlledContainerItem.createPointControls(container, stack, filteredBoxItems, pointCalculator);
				
		List<BoxItemGroup> removedBoxItemGroups = new ArrayList<>();

		if(order != Order.CRONOLOGICAL) {
	
			// remove boxes which do not fit due to volume, weight or stack value dimensions
			for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
				BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
				if(!container.fitsInside(boxItemGroup)) {
					if(abortOnAnyBoxTooBig) {
						return EmptyPackagerResultAdapter.EMPTY;
					}
					if(order != Order.CRONOLOGICAL) {
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
				if(boxItemControls != null) {
					boxItemControls.filteredGroups(removedBoxItemGroups);
				}
				if(pointControls != null) {
					pointControls.filteredGroups(removedBoxItemGroups);
				}
				removedBoxItemGroups.clear();
			}
		}
		
		pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		
		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, order, container, pointCalculator);

		int levelOffset = 0;
		boolean newLevel = true;

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		PlacementControls<Placement> placementControls = createControls(filteredBoxItems, 0, filteredBoxItems.size(), order, pointControls, container, pointCalculator, stack);
		PlacementControls<Placement> firstPlacementControls = createFirstControls(filteredBoxItems, 0, filteredBoxItems.size(), order, pointControls, container, pointCalculator, stack);
		groups:
		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !pointCalculator.isEmpty() && boxItemGroupIterator.hasNext()) {
			int groupIndex = boxItemGroupIterator.next();
			
			int boxItemStartIndex = packagerBoxItems.getFirstBoxItemIndexForGroup(groupIndex);
			
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(groupIndex);
			boxItemGroup.mark();
			
			pointCalculator.mark();
			int markStackSize = stack.size();
			
			int markLevelOffset = levelOffset;
			boolean markNewLevel = newLevel;
			
			while(!boxItemGroup.isEmpty()) {
				
				Placement bestPoint;
				if(newLevel) {
					// get first box in new level
					bestPoint = firstPlacementControls.getPlacement(boxItemStartIndex, boxItemGroup.size());
					if(bestPoint == null) {
						break;
					}
					
					DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, bestPoint.getStackValue().getDz() - 1 + levelOffset);
					
					pointCalculator.setPoints(Arrays.asList(levelFloor));
					pointCalculator.clear();
					
					levelOffset += bestPoint.getStackValue().getDz();

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
						DefaultPoint3D levelFloor = new DefaultPoint3D(0, 0, levelOffset, container.getLoadDx() - 1, container.getLoadDy() - 1, container.getLoadDz() - 1);
						pointCalculator.setPoints(Arrays.asList(levelFloor));
						pointCalculator.clear();
						
						// remove groups which have boxes which are too big for the max level size
						long maxArea = pointCalculator.getMaxArea();
						long maxVolume = pointCalculator.getMaxVolume();
						
						for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
							BoxItemGroup g = filteredBoxItemGroups.get(i);

							for(int k = 0; k < g.size(); k++) {
								BoxItem boxItem = g.get(k);
								Box box = boxItem.getBox();
								if(box.getVolume() > maxVolume || box.getMinimumArea() > maxArea) {

									if(abortOnAnyBoxTooBig) {
										return EmptyPackagerResultAdapter.EMPTY;
									}
									
									if(order != Order.CRONOLOGICAL) {
										filteredBoxItemGroups.remove(i);
										i--;
										
										removedBoxItemGroups.add(boxItemGroup);
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
							if(boxItemControls != null) {
								boxItemControls.filteredGroups(removedBoxItemGroups);
							}
							if(pointControls != null) {
								pointControls.filteredGroups(removedBoxItemGroups);
							}
							removedBoxItemGroups.clear();
						}
						
						continue;
					}
				}
				
				stack.add(bestPoint);
				pointCalculator.add(bestPoint.getPoint(), bestPoint);
				
				remainingLoadWeight -= bestPoint.getBoxItem().getBox().getWeight();
				remainingLoadVolume -= bestPoint.getBoxItem().getBox().getVolume();
				
				// decrement box item without deleting the whole group
				packagerBoxItems.decrement(bestPoint.getBoxItem().getIndex());

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
							
							if(order != Order.CRONOLOGICAL) {
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
						if(boxItemControls != null) {
							boxItemControls.filteredGroups(removedBoxItemGroups);
						}
						if(pointControls != null) {
							pointControls.filteredGroups(removedBoxItemGroups);
						}
						removedBoxItemGroups.clear();
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
					return EmptyPackagerResultAdapter.EMPTY;
				}

				List<BoxItem> removedBoxItems = new ArrayList<>();
				// undo any work on this group
				for(int i = markStackSize; i < stack.size(); i++) {
					removedBoxItems.add(stack.getPlacements().get(i).getStackValue().getBox().getBoxItem());
				}
				if(!removedBoxItems.isEmpty()) {
					if(boxItemControls != null) {
						boxItemControls.undo(removedBoxItems);
					}
					if(pointControls != null) {
						pointControls.undo(removedBoxItems);
					}
					
					removedBoxItems.clear();
				}
				
				if(boxItemControls != null) {
					boxItemControls.attemptFailure(boxItemGroup);
				}
				if(pointControls != null) {
					pointControls.attemptFailure(boxItemGroup);
				}
				
				stack.setSize(markStackSize);
				
				// unable to stack whole group
				if(order == Order.CRONOLOGICAL) {
					break groups;
				}
				// try again with another group if possible
				pointCalculator.reset();
				
				levelOffset = markLevelOffset;
				newLevel = markNewLevel;

				continue groups;
			}
			
			if(container.getMaxLoadWeight() < pointCalculator.calculateUsedWeight()) {
				throw new RuntimeException();
			}
			
			// successfully stacked group
			boxItemGroup.reset();
			
			if(boxItemControls != null) {
				boxItemControls.attemptSuccess(boxItemGroup);
			}
		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}
	
	protected BoxItemGroupIterator createBoxItemGroupIterator(BoxItemGroupSource filteredBoxItemGroups, Order itemGroupOrder, Container container, PointCalculator pointCalculator) {
		if(itemGroupOrder == Order.CRONOLOGICAL || itemGroupOrder == Order.CRONOLOGICAL_ALLOW_SKIPPING) {
			return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, pointCalculator);
		}
		return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, pointCalculator, boxItemGroupComparator);
	}

	public PlacementControls<Placement> createControls(BoxItemSource boxItems, int offset, int length, Order order, PointControls pointControls, Container container, PointCalculator pointCalculator, Stack stack) {
		return placementControlsBuilderFactory.createPlacementControlsBuilder()
			.withContainer(container)
			.withPointCalculator(pointCalculator)
			.withOrder(order)
			.withStack(stack)
			.withBoxItems(boxItems, offset, length)
			.withPointControls(pointControls)
			.build();
	}
	
	public PlacementControls<Placement> createFirstControls(BoxItemSource boxItems, int offset, int length, Order order, PointControls pointControls, Container container, PointCalculator pointCalculator, Stack stack) {
		return firstPlacementControlsBuilderFactory.createPlacementControlsBuilder()
			.withContainer(container)
			.withPointCalculator(pointCalculator)
			.withOrder(order)
			.withStack(stack)
			.withBoxItems(boxItems, offset, length)
			.withPointControls(pointControls)
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
	
}
