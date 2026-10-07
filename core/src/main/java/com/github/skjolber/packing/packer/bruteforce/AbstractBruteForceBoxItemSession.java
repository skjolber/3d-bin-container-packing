package com.github.skjolber.packing.packer.bruteforce;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackagerSession;
import com.github.skjolber.packing.packer.BoxItemsContainerItemsCalculator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;

public abstract class AbstractBruteForceBoxItemSession extends AbstractPackagerSession {

	// keep inventory over all of the iterators here
	protected Box[] boxes;
	protected int[] boxesRemaining;
	protected RemainingBoxItem[] boxItems;
	/** The box item (the input), initial count, local index and global index of each box item, for fresh sessions. */
	protected final BoxItem[] initialBoxItems;
	protected final int[] initialCounts;
	protected final int[] initialLocalIndexes;
	protected final int[] globalIndexes;

	/** The box item order: with an order, there is only one permutation */
	protected Order order = Order.NONE;
	/** Whether the box items have different container priorities (they are sorted by container priority) */
	protected final boolean containerPriorities;
	/**
	 * Whether a permutation and its reverse can be expected to pack equally well, so that skipping reverse permutations
	 * is an option, see {@link AbstractBruteForcePackager#isReverseSymmetric(com.github.skjolber.packing.packer.PackagerInput)}
	 */
	protected boolean reverseSymmetric = true;

	public AbstractBruteForceBoxItemSession(List<RemainingBoxItem> boxItems, List<ContainerItem> containers,
			int containerCount) {
		this(boxItems, new BoxItemsContainerItemsCalculator(containers, containerCount, boxItems));
	}

	protected AbstractBruteForceBoxItemSession(List<RemainingBoxItem> boxItems,
			ContainerItemsCalculator containerItemsCalculator) {
		super(containerItemsCalculator);
		
		this.boxes = new Box[boxItems.size()];
		this.boxesRemaining = new int[boxItems.size()];
		this.boxItems = new RemainingBoxItem[boxItems.size()];
		this.initialBoxItems = new BoxItem[boxItems.size()];
		this.initialCounts = new int[boxItems.size()];
		this.initialLocalIndexes = new int[boxItems.size()];
		this.globalIndexes = new int[boxItems.size()];
		
		for(int i = 0; i < boxItems.size(); i++) {
			RemainingBoxItem boxItem = boxItems.get(i);
			this.boxItems[i] = boxItem;
			this.boxes[i] = boxItem.getBox();
			this.boxesRemaining[i] = boxItem.getCount();
			this.initialBoxItems[i] = boxItem.getBoxItem();
			this.initialCounts[i] = boxItem.getCount();
			this.initialLocalIndexes[i] = boxItem.getLocalIndex();
			this.globalIndexes[i] = boxItem.getGlobalIndex();
		}
		boolean containerPriorities = false;
		for(int i = 1; i < boxItems.size(); i++) {
			if(boxItems.get(i).getContainerPriority() != boxItems.get(0).getContainerPriority()) {
				containerPriorities = true;
				break;
			}
		}
		this.containerPriorities = containerPriorities;
	} 

	protected AbstractBruteForceBoxItemSession(AbstractBruteForceBoxItemSession source) {
		super(source);
		this.initialBoxItems = source.initialBoxItems;
		this.initialCounts = source.initialCounts;
		this.initialLocalIndexes = source.initialLocalIndexes;
		this.globalIndexes = source.globalIndexes;
		this.order = source.order;
		this.containerPriorities = source.containerPriorities;
		this.reverseSymmetric = source.reverseSymmetric;
		this.boxes = source.boxes.clone();
		this.boxesRemaining = source.boxesRemaining.clone();
		this.boxItems = new RemainingBoxItem[source.boxItems.length];
		for(int i = 0; i < boxItems.length; i++) {
			if(source.boxItems[i] != null) {
				boxItems[i] = source.boxItems[i].copy();
			}
		}
	}

	/**
	 * @param order the box item order; with an order, there is only one permutation
	 */
	public void setOrder(Order order) {
		this.order = order != null ? order : Order.NONE;
	}

	public void setReverseSymmetric(boolean reverseSymmetric) {
		this.reverseSymmetric = reverseSymmetric;
	}

	@Override
	public PackagerSession fresh() {
		PackagerSession fresh = super.fresh();
		((AbstractBruteForceBoxItemSession)fresh).setOrder(order);
		((AbstractBruteForceBoxItemSession)fresh).setReverseSymmetric(reverseSymmetric);
		return fresh;
	}

	/**
	 * @return whether the box items are in a given order, or sorted by container priority: permutations which are the
	 *         reverse of each other do not both respect the order
	 */
	protected boolean isOrdered() {
		return order != Order.NONE || containerPriorities;
	}

	/**
	 * With a box item order, the boxes after a box which does not fit the container cannot be placed in it, and with
	 * container priorities, neither can the boxes of a higher priority than such a box: they would be in an earlier
	 * container.
	 *
	 * @param iterator the container's iterator, by box item index (null if the box item does not fit, or is packed)
	 * @return the number of leading boxes of the iterator's permutations which may be placed
	 */
	protected int getLimit(BoxItemPermutationRotationIterator iterator) {
		if(!isOrdered()) {
			return Integer.MAX_VALUE;
		}
		RemainingBoxItem[] iteratorItems = iterator.getBoxItems();
		int limit = 0;
		int blockedPriority = Integer.MAX_VALUE;
		for(int i = 0; i < boxItems.length; i++) {
			RemainingBoxItem boxItem = boxItems[i];
			if(boxItem == null) {
				// packed
				continue;
			}
			if(boxItem.getContainerPriority() > blockedPriority) {
				break;
			}
			if(iteratorItems[i] == null) {
				// does not fit the container
				if(order != Order.NONE) {
					break;
				}
				blockedPriority = boxItem.getContainerPriority();
				continue;
			}
			limit += iteratorItems[i].getCount();
		}
		return limit;
	}

	/**
	 * Boxes which do not fit the container wait for a later container, and so do the boxes of a higher container
	 * priority (see {@link AbstractBruteForcePackager#packInOrderSkipping}).
	 *
	 * @param iterator the container's iterator, by box item index (null if the box item does not fit, or is packed)
	 * @return the highest container priority which may be placed in the container
	 */
	protected int getMaxContainerPriority(BoxItemPermutationRotationIterator iterator) {
		RemainingBoxItem[] iteratorItems = iterator.getBoxItems();
		int maxContainerPriority = Integer.MAX_VALUE;
		for(int i = 0; i < boxItems.length; i++) {
			if(boxItems[i] != null && iteratorItems[i] == null) {
				maxContainerPriority = Math.min(maxContainerPriority, boxItems[i].getContainerPriority());
			}
		}
		return maxContainerPriority;
	}

	/** @return copies of the box items at the start of the packaging operation */
	protected List<RemainingBoxItem> copyInitialBoxItems() {
		List<RemainingBoxItem> copies = new ArrayList<>(boxes.length);
		for(int i = 0; i < boxes.length; i++) {
			copies.add(new RemainingBoxItem(initialBoxItems[i], initialCounts[i], initialLocalIndexes[i], globalIndexes[i]));
		}
		return copies;
	}

	@Override
	public ContainerItem getContainerItem(int index) {
		return packagerContainerItems.getContainerItem(index);
	}


	protected void removeInventory(List<Integer> p) {
		// remove session inventory
		for (Integer remove : p) {
			boxesRemaining[remove]--;
			boxItems[remove].decrement();
			if(boxItems[remove].isEmpty()) {
				boxItems[remove] = null;
			}
		}
	}

	/** Translate placements from another session to this session's local iterator indexes. */
	protected List<Integer> getLocalIndexes(Stack stack) {
		List<Integer> indexes = new ArrayList<>(stack.size());
		for(Placement placement : stack.getPlacements()) {
			int localIndex = getLocalIndex(getGlobalIndex(placement));
			indexes.add(localIndex);
		}
		return indexes;
	}

	@Override
	protected int getInputIndex(BoxItem boxItem) {
		for(int i = 0; i < initialBoxItems.length; i++) {
			if(initialBoxItems[i] == boxItem) {
				return globalIndexes[i];
			}
		}
		return -1;
	}

	protected int getLocalIndex(int globalIndex) {
		for(int i = 0; i < boxItems.length; i++) {
			RemainingBoxItem boxItem = boxItems[i];
			if(boxItem != null && boxItem.getGlobalIndex() == globalIndex) {
				return i;
			}
		}
		throw new IllegalArgumentException("Result contains unknown box item global index " + globalIndex);
	}

	@Override
	public List<Integer> getContainers() {
		return packagerContainerItems.getContainers(getRemainingBoxItems()).getContainerIndexes();
	}

	@Override
	public List<RemainingBoxItem> getRemainingBoxItems() {
		List<RemainingBoxItem> remainingBoxItems = new ArrayList<>(boxItems.length);
		for(int i = 0; i < boxItems.length; i++) {
			RemainingBoxItem boxItem = boxItems[i];
			if(boxItem != null && !boxItem.isEmpty()) {
				remainingBoxItems.add(boxItem);
			}
		}
		return remainingBoxItems;
	}

	@Override
	public long getRemainingVolume() {
		long volume = 0L;
		for(int i = 0; i < boxes.length; i++) {
			volume = Math.addExact(volume, Math.multiplyExact(boxes[i].getVolume(), boxesRemaining[i]));
		}
		return volume;
	}

	@Override
	public long getRemainingWeight() {
		long weight = 0L;
		for(int i = 0; i < boxes.length; i++) {
			weight = Math.addExact(weight, Math.multiplyExact((long)boxes[i].getWeight(), boxesRemaining[i]));
		}
		return weight;
	}

	protected IntermediatePackagerResult copy(ContainerItem peek, IntermediatePackagerResult result, int index) {
		if(result instanceof BruteForceIntermediatePackagerResult bruteForceResult) {
			// keep the permutation state, so that the stack can be calculated again when accepted
			return bruteForceResult.copyTo(peek, index);
		}
		// a result of another session
		return new DefaultIntermediatePackagerResult(peek, result.getStack());
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return -1;
	}

	@Override
	public List<RemainingBoxItemGroup> getRemainingBoxItemGroups() {
		return null;
	}

}
