package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

public class LevelStack extends Stack {

	protected int weight = 0;
	protected int dz = 0;
	
	protected List<Stack> levels = new ArrayList<>();
	
	public List<StackEntry> getEntries() {
		List<StackEntry> entries = new ArrayList<>();
		
		for(Stack level : levels) {
			entries.addAll(level.getEntries());
		}
		
		return entries;
	}

	public void add(StackEntry e) {
		levels.get(levels.size() - 1).add(e);
	}
	
	public boolean add(Stack element) {
		if(!levels.isEmpty()) {
			dz += currentLevelStackDz();
			weight += currentLevelStackWeight();
		}

		return levels.add(element);
	}
	
	public long getFreeVolumeLoad() {
		long volume = containerStackValue.getMaxLoadVolume();
		
		for (StackEntry stackEntry : getEntries()) {
			volume -= stackEntry.getStackable().getVolume();
		}
		
		return volume;
	}

	public int getFreeWeightLoad() {
		return containerStackValue.getMaxLoadWeight() - getWeight();
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
		int weight = 0;
		for (StackEntry stackEntry : getEntries()) {
			weight += stackEntry.getStackable().getWeight();
		}
		return weight;
	}

	@Override
	public int getDz() {
		return dz + currentLevelStackDz();
	}

	@Override
	public long getVolume() {
		long volume = 0;
		for (StackEntry stackEntry : getEntries()) {
			volume += stackEntry.getStackable().getVolume();
		}
		return volume;
	}
	
	public ContainerStackValue getFreeContainerStackValue() {
		if(levels.isEmpty()) {
			return containerStackValue;
		}
		int remainder = containerStackValue.getLoadDz() - getDz();
		if(remainder < 0) {
			throw new IllegalArgumentException("Remaining free space is negative at " + remainder + " for " + this);
		}
		
		int dz = getDz();
		
		return new ContainerStackValue(
						containerStackValue.getDx(), containerStackValue.getDy(), containerStackValue.getDz() - dz,
						0, getFreeWeight(), getSupportedCount(), containerStackValue.getPressureReference(),
						containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz() - dz,   
						containerStackValue.getDirection()
						);
	}

	public int getSupportedCount() {
		return levels.size(); // XXX not ideal
	}

	public int getFreeWeight() {
		int remainder = weight - getWeight();
		if(remainder < 0) {
			throw new IllegalArgumentException("Remaining weight is negative at " + remainder);
		}
		return remainder;
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
		while(limit > count && i < levels.size()) {
			count += levels.get(i).getEntries().size();
			
			i++;
		}
		
		i--;
		if(count == limit) {
			// see if we can keep the last level
			// if so there must be no free space in it
			Stack level = levels.get(i);
			
			long volume = containerStackValue.getMaxLoadVolume();
			
			long v = (volume / containerStackValue.getLoadDz()) * level.getDz();
			
			for (StackEntry stackEntry : level.getEntries()) {
				v -= stackEntry.getStackable().getVolume();
			}
			
			if(v == 0) {
				// keep last level
				i++;
			} else {
				// discard also the last level
				count -= levels.get(i).getEntries().size();
			}
		} else {
			// discard also the last level
			count -= levels.get(i).getEntries().size();
		}
		
		while(i < levels.size()) {
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
	
}
