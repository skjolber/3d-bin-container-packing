package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultContainerItemsCalculator;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * This implementation tries all permutations, rotations and points.
 * <br>
 * <br>
 * Note: The brute force algorithm uses a recursive algorithm. It is not intended for more than 10 boxes.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class BruteForcePackager extends AbstractBruteForcePackager {

	// implementation notes:
	//  * new BoxItemControlsBuilder / BoxItemGroupControls per permutation 
	//
	// placement results:
	//  * comparing box items is not necessary
	//  * comparing groups is not necessary
	//  * finding best point is not necessary
	//
	// filtering:
	// boxes: (resets for each permutation)
	//  * box items
	//  * box item groups 
	//
	// points: (resets for each box)
	//  * finding best point is not necessary
	//  * filtering available points per box item is necessary
	//
	// strategy:
	//  * find first result (current implementation)
	//  * find best result according to comparator (future implementation)
	//
	
	public static BruteForcePackagerBuilder newBuilder() {
		return new BruteForcePackagerBuilder();
	}

	public static class BruteForcePackagerBuilder extends AbstractPackagerBuilder<BruteForcePackager, BruteForcePackagerBuilder> {

		protected IntermediatePackagerResultComparator comparator;
		
		public BruteForcePackager build() {
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator();
			}
			return new BruteForcePackager(comparator);
		}
	}
	
	private class BruteForceAdapter extends AbstractSingleThreadedBruteForceBoxItemPackagerAdapter {

		public BruteForceAdapter(List<BoxItem> boxItems, BoxPriority priority,
				DefaultContainerItemsCalculator packagerContainerItems,
				BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, priority, packagerContainerItems, containerIterators, interrupt);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, BruteForceIntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return BruteForcePackager.this.pack(extremePoints, stackPlacements, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt);
		}
		
	}
	
	private class BruteForceGroupAdapter extends AbstractSingleThreadedBruteForceBoxItemGroupPackagerAdapter {

		public BruteForceGroupAdapter(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, BoxPriority priority,
				DefaultContainerItemsCalculator packagerContainerItems,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, priority, packagerContainerItems, containerIterators, interrupt);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, BruteForceIntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return BruteForcePackager.this.pack(extremePoints, stackPlacements, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt);
		}
		
	}

	public BruteForcePackager(IntermediatePackagerResultComparator packResultComparator) {
		super(packResultComparator);
	}

	@Override
	protected boolean acceptAsFull(BruteForceIntermediatePackagerResult result, Container holder) {
		return result.getLoadVolume() == holder.getMaxLoadVolume();
	}

	@Override
	protected BruteForceGroupAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups,
			BoxPriority priority, DefaultContainerItemsCalculator defaultContainerItemsCalculator,
			PackagerInterruptSupplier interrupt) {
		DefaultBoxItemGroupPermutationRotationIterator[] containerIterators = new DefaultBoxItemGroupPermutationRotationIterator[defaultContainerItemsCalculator.getContainerItemCount()];

		for (int i = 0; i < defaultContainerItemsCalculator.getContainerItemCount(); i++) {
			ContainerItem containerItem = defaultContainerItemsCalculator.getContainerItem(i);
			Container container = containerItem.getContainer();

			Dimension dimension = new Dimension(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

			containerIterators[i] = DefaultBoxItemGroupPermutationRotationIterator
					.newBuilder()
					.withLoadSize(dimension)
					.withBoxItemGroups(itemGroups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		
		
		List<BoxItem> boxItems = new ArrayList<>();
		for (BoxItemGroup boxItemGroup : itemGroups) {
			boxItems.addAll(boxItemGroup.getItems());
		}
		return new BruteForceGroupAdapter(boxItems, itemGroups, priority, defaultContainerItemsCalculator, containerIterators, interrupt);
	}

	@Override
	protected BruteForceAdapter createBoxItemAdapter(List<BoxItem> boxItems, BoxPriority priority,
			DefaultContainerItemsCalculator defaultContainerItemsCalculator, PackagerInterruptSupplier interrupt) {
		BoxItemPermutationRotationIterator[] containerIterators = new DefaultBoxItemPermutationRotationIterator[defaultContainerItemsCalculator.getContainerItemCount()];

		for (int i = 0; i < defaultContainerItemsCalculator.getContainerItemCount(); i++) {
			ContainerItem containerItem = defaultContainerItemsCalculator.getContainerItem(i);
			Container container = containerItem.getContainer();

			Dimension dimension = new Dimension(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

			containerIterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(dimension)
					.withBoxItems(boxItems)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		return new BruteForceAdapter(boxItems, priority, defaultContainerItemsCalculator, containerIterators, interrupt);
	}

}
