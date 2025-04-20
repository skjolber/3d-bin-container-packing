package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;

public class LevelStack extends Stack {

	protected int weight = 0; // not including current level
	protected int dz = 0; // not including current level

	protected List<Stack> levels = new ArrayList<>();
	
	protected Container container;
	
	public LevelStack(Container container) {
		this.container = container;
	}

	public List<StackPlacement> getPlacements() {
		List<StackPlacement> entries = new ArrayList<>();

		for (Stack level : levels) {
			entries.addAll(level.getPlacements());
		}

		return entries;
	}

	public void add(StackPlacement e) {
		levels.get(levels.size() - 1).add(e);
	}

	@Override
	public void remove(StackPlacement e) {
		levels.get(levels.size() - 1).remove(e);
	}

	public boolean add(Stack element) {
		if(!levels.isEmpty()) {
			dz += currentLevelStackDz();
			weight += currentLevelStackWeight();
		}

		return levels.add(element);
	}

	public List<Stack> getLevels() {
		return levels;
	}

	public long getFreeVolumeLoad() {
		long volume = container.getMaxLoadVolume();

		for (Stack level : levels) {
			volume -= level.getVolume();
		}
		return volume;
	}

	public int getFreeWeightLoad() {
		return container.getMaxLoadWeight() - getWeight();
	}

	public void clear() {
		levels.clear();
	}

	private int currentLevelStackWeight() {
		if(levels.isEmpty()) {
			return 0;
		}
		return levels.get(levels.size() - 1).getWeight();
	}

	private int currentLevelStackDz() {
		if(levels.isEmpty()) {
			return 0;
		}
		return levels.get(levels.size() - 1).getDz();
	}

	@Override
	public int getWeight() {
		return weight + currentLevelStackWeight();
	}

	@Override
	public int getDz() {
		return dz + currentLevelStackDz();
	}

	@Override
	public long getVolume() {
		long volume = 0;
		for (Stack stack : levels) {
			volume += stack.getVolume();
		}
		return volume;
	}

	public Container getFreeContainerStackValue() {
		if(levels.isEmpty()) {
			return container;
		}
		int remainder = container.getLoadDz() - getDz();
		if(remainder < 0) {
			throw new IllegalArgumentException("Remaining free space is negative at " + remainder + " for " + this);
		}

		int dz = getDz();

		int freeWeight = getFreeWeightLoad();

		return new Container(
				container.getId(), container.getDescription(),
				container.getDx(), container.getDy(), container.getDz() - dz,
				container.getEmptyWeight(),
				container.getLoadDx(), container.getLoadDy(), container.getLoadDz() - dz,
				freeWeight,
				new Stack(), container.getBoxItemListenerBuilderSupplier(), container.getFilteredPointsBuilderSupplier());
	}

	public int getFreeLoadDz() {
		return container.getLoadDz() - getDz();
	}

	public Container getContainerStackValue(int dz) {

		int freeWeight = getFreeWeightLoad();

		return new Container(
				container.getId(), container.getDescription(),
				container.getDx(), container.getDy(), dz,
				container.getEmptyWeight(),
				container.getLoadDx(), container.getLoadDy(), dz,
				freeWeight,
				new Stack(),
				container.getBoxItemListenerBuilderSupplier(), container.getFilteredPointsBuilderSupplier());
	}

	public int getSupportedCount() {
		return levels.size(); // XXX not ideal
	}

	/**
	 * Clear levels up to and including a number of boxes
	 * 
	 * @param limit number of boxes to keep
	 * @return number of boxes kept
	 */

	public int clearLevelsForBoxes(int limit) {
		int count = 0;
		int i = 0;
		while (limit > count && i < levels.size()) {
			count += levels.get(i).getPlacements().size();

			i++;
		}

		i--;
		if(count == limit) {
			// see if we can keep the last level
			// if so there must be no free space in it
			Stack level = levels.get(i);

			long volume = container.getMaxLoadVolume();

			long v = (volume / container.getLoadDz()) * level.getDz();

			for (StackPlacement stackEntry : level.getPlacements()) {
				v -= stackEntry.getBox().getVolume();
			}

			if(v == 0) {
				// keep last level
				i++;
			} else {
				// discard also the last level
				count -= levels.get(i).getPlacements().size();
			}
		} else {
			// discard also the last level
			count -= levels.get(i).getPlacements().size();
		}

		while (i < levels.size()) {
			removeLevel(i);
		}

		return count;
	}

	public void removeLevel(int index) {
		Stack level = levels.remove(index);
		if(index != levels.size()) {
			dz -= level.getDz();
			weight -= level.getWeight();
		}
	}

	@Override
	public boolean isEmpty() {
		return levels.isEmpty() || levels.get(0).isEmpty();
	}

	@Override
	public int getSize() {
		int count = 0;
		for (Stack stack : levels) {
			count += stack.getSize();
		}
		return count;
	}

	@Override
	public void setSize(int size) {
		clearLevelsForBoxes(size); // inaccurate
	}

}
