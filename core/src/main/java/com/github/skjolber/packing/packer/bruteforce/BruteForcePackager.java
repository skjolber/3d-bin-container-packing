package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.PackagerInterruptedException;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;

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

	public static class BruteForcePackagerBuilder {

		protected Comparator<BruteForceIntermediatePackagerResult> comparator;
		
		public BruteForcePackager build() {
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator<>();
			}
			return new BruteForcePackager(comparator);
		}
	}
	
	private class BruteForceAdapter extends AbstractSingleThreadedBruteForceBoxItemPackagerAdapter {

		protected final ExtremePoints3DStack extremePoints;
		
		public BruteForceAdapter(List<BoxItem> boxItems, BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems,
				BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, priority, packagerContainerItems, containerIterators, interrupt);
			
			this.extremePoints =  new ExtremePoints3DStack(getMaxIteratorLength() + 1);
			this.extremePoints.reset(1, 1, 1);
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

		protected final ExtremePoints3DStack extremePoints;

		public BruteForceGroupAdapter(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, priority, packagerContainerItems, containerIterators, interrupt);
			
			this.extremePoints =  new ExtremePoints3DStack(getMaxIteratorLength() + 1);
			this.extremePoints.reset(1, 1, 1);
		}
		
		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, BruteForceIntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return BruteForcePackager.this.pack(extremePoints, stackPlacements, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt);
		}
		
	}

	public BruteForcePackager(Comparator<BruteForceIntermediatePackagerResult> comparator) {
		super(comparator);
	}

	@Override
	protected BruteForceGroupAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups,
			BoxPriority priority, ContainerItemsCalculator containerItemsCalculator,
			PackagerInterruptSupplier interrupt) {
		DefaultBoxItemGroupPermutationRotationIterator[] containerIterators = new DefaultBoxItemGroupPermutationRotationIterator[containerItemsCalculator.getContainerItemCount()];

		for (int i = 0; i < containerItemsCalculator.getContainerItemCount(); i++) {
			ContainerItem containerItem = containerItemsCalculator.getContainerItem(i);
			Container container = containerItem.getContainer();

			containerIterators[i] = DefaultBoxItemGroupPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItemGroups(itemGroups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		List<BoxItem> boxItems = new ArrayList<>();
		for (BoxItemGroup boxItemGroup : itemGroups) {
			boxItems.addAll(boxItemGroup.getItems());
		}
		return new BruteForceGroupAdapter(boxItems, itemGroups, priority, containerItemsCalculator, containerIterators, interrupt);
	}

	@Override
	protected BruteForceAdapter createBoxItemAdapter(List<BoxItem> boxItems, BoxPriority priority,
			ContainerItemsCalculator containerItemsCalculator, PackagerInterruptSupplier interrupt) {
		BoxItemPermutationRotationIterator[] containerIterators = new DefaultBoxItemPermutationRotationIterator[containerItemsCalculator.getContainerItemCount()];

		for (int i = 0; i < containerItemsCalculator.getContainerItemCount(); i++) {
			ContainerItem containerItem = containerItemsCalculator.getContainerItem(i);
			Container container = containerItem.getContainer();

			containerIterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItems(boxItems)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		return new BruteForceAdapter(boxItems, priority, containerItemsCalculator, containerIterators, interrupt);
	}

}
