package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.packager.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.ControlledContainerItem;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePlacement;
import com.github.skjolber.packing.api.packager.ManifestControls;
import com.github.skjolber.packing.api.packager.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.PlacementControls;
import com.github.skjolber.packing.api.packager.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.PointControls;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.iterator.AnyOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.FixedOrderBoxItemGroupIterator;
import com.github.skjolber.packing.iterator.PackagerBoxItems;
import com.github.skjolber.packing.packer.AbstractBoxItemAdapter;
import com.github.skjolber.packing.packer.AbstractBoxItemGroupAdapter;
import com.github.skjolber.packing.packer.AbstractControlPackager;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementControls;
import com.github.skjolber.packing.packer.ComparatorIntermediatePlacementControlsBuilder;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyIntermediatePackagerResult;
import com.github.skjolber.packing.packer.EmptyPackagerResultAdapter;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.laff.AbstractLargestAreaFitFirstPackager.LargestAreaFitFirstResultBuilder;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractLargestAreaFitFirstPackager extends AbstractControlPackager<IntermediatePlacement, IntermediatePackagerResult, AbstractLargestAreaFitFirstPackager.LargestAreaFitFirstResultBuilder> {

	protected class PlainBoxItemAdapter extends AbstractBoxItemAdapter<IntermediatePackagerResult> {

		public PlainBoxItemAdapter(List<BoxItem> boxItems, BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems,
				PackagerInterruptSupplier interrupt) {
			super(boxItems, priority, packagerContainerItems, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ControlledContainerItem containerItem,
				PackagerInterruptSupplier interrupt, BoxPriority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return AbstractLargestAreaFitFirstPackager.this.pack(remainingBoxItems, containerItem, interrupt, priority, abortOnAnyBoxTooBig);
		}

	}
	
	protected class PlainBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter<IntermediatePackagerResult> {

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups,
				BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems, 
				PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, packagerContainerItems, priority, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, BoxPriority priority,
				ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
			return AbstractLargestAreaFitFirstPackager.this.packGroup(remainingBoxItemGroups, priority, containerItem, interrupt, abortOnAnyBoxTooBig);
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
					adapter = new PlainBoxItemAdapter(items, priority, new ContainerItemsCalculator(containers), interrupt);
				} else {
					adapter = new PlainBoxItemGroupAdapter(itemGroups, priority, new ContainerItemsCalculator(containers), interrupt);
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
	protected PlacementControlsBuilderFactory<IntermediatePlacement, ComparatorIntermediatePlacementControlsBuilder> placementControlsBuilderFactory;
	
	protected Comparator<IntermediatePlacement> intermediatePlacementResultComparator;
	protected Comparator<BoxItemGroup> boxItemGroupComparator;
	protected Comparator<BoxItem> boxItemComparator;
	
	protected Comparator<IntermediatePlacement> firstIntermediatePlacementResultComparator;
	protected Comparator<BoxItemGroup> firstBoxItemGroupComparator;
	protected Comparator<BoxItem> firstBoxItemComparator;
	
	public AbstractLargestAreaFitFirstPackager(Comparator<IntermediatePackagerResult> comparator, Comparator<IntermediatePlacement> intermediatePlacementResultComparator, Comparator<BoxItem> boxItemComparator, Comparator<BoxItemGroup> boxItemGroupComparator, Comparator<BoxItemGroup> firstBoxItemGroupComparator, Comparator<BoxItem> firstBoxItemComparator, Comparator<IntermediatePlacement> firstIntermediatePlacementResultComparator, PlacementControlsBuilderFactory<IntermediatePlacement, ComparatorIntermediatePlacementControlsBuilder> placementControlsBuilderFactory) {
		super(comparator);

		this.placementControlsBuilderFactory = placementControlsBuilderFactory;
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		this.boxItemComparator = boxItemComparator;
		this.boxItemGroupComparator = boxItemGroupComparator;
		
		this.firstBoxItemGroupComparator = firstBoxItemGroupComparator;
		this.firstBoxItemComparator = firstBoxItemComparator;
		this.firstIntermediatePlacementResultComparator = firstIntermediatePlacementResultComparator;
	}

	public IntermediatePackagerResult pack(List<BoxItem> boxItems, ControlledContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, BoxPriority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		ContainerItem containerItem = compositeContainerItem;
		Container container = containerItem.getContainer();

		Stack stack = new Stack();

		ExtremePoints extremePoints = createExtremePoints();
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

		DefaultBoxItemSource filteredBoxItems = new DefaultBoxItemSource(boxItems);
		ManifestControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints, null);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);
		// remove boxes which do not fit due to volume, weight or dimensions
		List<BoxItem> removed = new ArrayList<>();
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			BoxItem boxItem = filteredBoxItems.get(i);
			if(!container.fitsInside(boxItem.getBox())) {

				if(abortOnAnyBoxTooBig) {
					return EmptyPackagerResultAdapter.EMPTY;
				}
				
				if(priority != BoxPriority.CRONOLOGICAL) {
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
		
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		int levelOffset = 0;
		boolean newLevel = true;

		PlacementControls<IntermediatePlacement> placementControls = createControls(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);
		PlacementControls<IntermediatePlacement> firstPlacementControls = createFirstControls(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);

		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			IntermediatePlacement result;
			if(newLevel) {
				// get first box in new level
				result = firstPlacementControls.getPlacement(0, filteredBoxItems.size());
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
				result = placementControls.getPlacement(0, filteredBoxItems.size());
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
							
							if(priority != BoxPriority.CRONOLOGICAL) {
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
			
			Placement stackPlacement = new Placement(result.getStackValue(), result.getPoint());

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
						
						if(priority != BoxPriority.CRONOLOGICAL) {
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
				extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());				
			}
		}
		
		// ignore decline for the rest
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem, stack);
	}

	protected abstract ExtremePoints createExtremePoints();

	public IntermediatePackagerResult packGroup(List<BoxItemGroup> boxItemGroups, BoxPriority priority, ControlledContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
		ContainerItem containerItem = compositeContainerItem;
		Container container = containerItem.getContainer();
		
		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints = new MarkResetExtremePoints3D(true);
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		
		PackagerBoxItems packagerBoxItems = new PackagerBoxItems(boxItemGroups);

		BoxItemSource filteredBoxItems = packagerBoxItems.getFilteredBoxItems();

		BoxItemGroupSource filteredBoxItemGroups = packagerBoxItems.getFilteredBoxItemGroups();

		ManifestControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints, filteredBoxItemGroups);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);
				
		List<BoxItemGroup> removedBoxItemGroups = new ArrayList<>();

		if(priority != BoxPriority.CRONOLOGICAL) {
	
			// remove boxes which do not fit due to volume, weight or stack value dimensions
			for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
				BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
				if(!container.fitsInside(boxItemGroup)) {
					if(abortOnAnyBoxTooBig) {
						return EmptyPackagerResultAdapter.EMPTY;
					}
					if(priority != BoxPriority.CRONOLOGICAL) {
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
		
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		
		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, priority, container, extremePoints);

		int levelOffset = 0;
		boolean newLevel = true;

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		PlacementControls<IntermediatePlacement> placementControls = createControls(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);
		PlacementControls<IntermediatePlacement> firstPlacementControls = createFirstControls(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);
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
				
				IntermediatePlacement bestPoint;
				if(newLevel) {
					// get first box in new level
					bestPoint = firstPlacementControls.getPlacement(boxItemStartIndex, boxItemGroup.size());
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
					bestPoint = placementControls.getPlacement(boxItemStartIndex, boxItemGroup.size());
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
									
									if(priority != BoxPriority.CRONOLOGICAL) {
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
				
				Placement stackPlacement = new Placement(bestPoint.getStackValue(), bestPoint.getPoint());
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
							
							if(priority != BoxPriority.CRONOLOGICAL) {
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
				if(priority == BoxPriority.CRONOLOGICAL) {
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
			
			if(boxItemControls != null) {
				boxItemControls.attemptSuccess(boxItemGroup);
			}
		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}
	
	protected BoxItemGroupIterator createBoxItemGroupIterator(BoxItemGroupSource filteredBoxItemGroups, BoxPriority itemGroupOrder, Container container, ExtremePoints extremePoints) {
		if(itemGroupOrder == BoxPriority.CRONOLOGICAL || itemGroupOrder == BoxPriority.CRONOLOGICAL_ALLOW_SKIPPING) {
			return new FixedOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints);
		}
		return new AnyOrderBoxItemGroupIterator(filteredBoxItemGroups, container, extremePoints, boxItemGroupComparator);
	}

	public PlacementControls<IntermediatePlacement> createControls(BoxItemSource boxItems, int offset, int length, BoxPriority priority, PointControls pointControls, Container container, ExtremePoints extremePoints, Stack stack) {
		return placementControlsBuilderFactory.createIntermediatePlacementResultBuilder()
			.withContainer(container)
			.withExtremePoints(extremePoints)
			.withPriority(priority)
			.withStack(stack)
			.withBoxItems(boxItems, offset, length)
			.withPointControls(pointControls)
			.withIntermediatePlacementComparator(intermediatePlacementResultComparator)
			.withBoxItemComparator(boxItemComparator)
			.build();
	}
	
	public ComparatorIntermediatePlacementControls createFirstControls(BoxItemSource boxItems, int offset, int length, BoxPriority priority, PointControls pointControls, Container container, ExtremePoints extremePoints, Stack stack) {
		return placementControlsBuilderFactory.createIntermediatePlacementResultBuilder()
			.withContainer(container)
			.withExtremePoints(extremePoints)
			.withPriority(priority)
			.withStack(stack)
			.withBoxItems(boxItems, offset, length)
			.withPointControls(pointControls)
			.withIntermediatePlacementComparator(firstIntermediatePlacementResultComparator)
			.withBoxItemComparator(firstBoxItemComparator)
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
