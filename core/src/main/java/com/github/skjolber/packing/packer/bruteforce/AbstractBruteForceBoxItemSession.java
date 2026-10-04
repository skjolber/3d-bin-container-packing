package com.github.skjolber.packing.packer.bruteforce;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.AbstractPackagerSession;
import com.github.skjolber.packing.packer.BoxItemsContainerItemsCalculator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;

public abstract class AbstractBruteForceBoxItemSession extends AbstractPackagerSession {

	// keep inventory over all of the iterators here
	protected Box[] boxes;
	protected int[] boxesRemaining;
	protected BoxItem[] boxItems;
	/** The initial count, local index and global index of each box item, for fresh sessions. */
	protected final int[] initialCounts;
	protected final int[] initialLocalIndexes;
	protected final int[] globalIndexes;

	public AbstractBruteForceBoxItemSession(List<BoxItem> boxItems, List<ContainerItem> containers,
			int containerCount) {
		this(initializeGlobalIndexes(boxItems), new BoxItemsContainerItemsCalculator(containers, containerCount, boxItems));
	}

	protected AbstractBruteForceBoxItemSession(List<BoxItem> boxItems,
			ContainerItemsCalculator containerItemsCalculator) {
		super(containerItemsCalculator);
		
		this.boxes = new Box[boxItems.size()];
		this.boxesRemaining = new int[boxItems.size()];
		this.boxItems = new BoxItem[boxItems.size()];
		this.initialCounts = new int[boxItems.size()];
		this.initialLocalIndexes = new int[boxItems.size()];
		this.globalIndexes = new int[boxItems.size()];
		
		for(int i = 0; i < boxItems.size(); i++) {
			BoxItem boxItem = boxItems.get(i);
			this.boxItems[i] = boxItem;
			this.boxes[i] = boxItem.getBox();
			this.boxesRemaining[i] = boxItem.getCount();
			this.initialCounts[i] = boxItem.getCount();
			this.initialLocalIndexes[i] = boxItem.getLocalIndex();
			this.globalIndexes[i] = boxItem.getGlobalIndex();
		}
	} 

	protected AbstractBruteForceBoxItemSession(AbstractBruteForceBoxItemSession source) {
		super(source);
		this.initialCounts = source.initialCounts;
		this.initialLocalIndexes = source.initialLocalIndexes;
		this.globalIndexes = source.globalIndexes;
		this.boxes = source.boxes.clone();
		this.boxesRemaining = source.boxesRemaining.clone();
		this.boxItems = new BoxItem[source.boxItems.length];
		for(int i = 0; i < boxItems.length; i++) {
			if(source.boxItems[i] != null) {
				boxItems[i] = source.boxItems[i].copy();
			}
		}
	}

	/** @return copies of the box items at the start of the packaging operation */
	protected List<BoxItem> copyInitialBoxItems() {
		List<BoxItem> copies = new ArrayList<>(boxes.length);
		for(int i = 0; i < boxes.length; i++) {
			copies.add(new BoxItem(boxes[i].copy(), initialCounts[i], initialLocalIndexes[i], globalIndexes[i]));
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
			BoxItem source = (BoxItem) placement.getStackValue().getBox().getBoxItem();
			int globalIndex = source.getGlobalIndex();
			int localIndex = getLocalIndex(globalIndex);
			indexes.add(localIndex);
		}
		return indexes;
	}

	protected int getLocalIndex(int globalIndex) {
		for(int i = 0; i < boxItems.length; i++) {
			BoxItem boxItem = boxItems[i];
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
	public List<BoxItem> getRemainingBoxItems() {
		List<BoxItem> remainingBoxItems = new ArrayList<>(boxItems.length);
		for(int i = 0; i < boxItems.length; i++) {
			BoxItem boxItem = boxItems[i];
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
	public List<BoxItemGroup> getRemainingBoxItemGroups() {
		return null;
	}

}
