package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

public class DefaultStack extends Stack {

	protected List<StackEntry> entries = new ArrayList<>();

	public List<StackEntry> getEntries() {
		return entries;
	}

	public void add(StackEntry e) {
		entries.add(e);
	}

	public void clear() {
		entries.clear();
	}

	@Override
	public int getWeight() {
		int weight = 0;
		
		for (StackEntry stackEntry : entries) {
			weight += stackEntry.getStackable().getWeight();
		}
		
		return weight;
	}

	@Override
	public int getDz() {
		int dz = 0;
		
		for (StackEntry stackEntry : entries) {
			StackValue stackValue = stackEntry.getStackValue();
			StackSpace space = stackEntry.getSpace();
			dz += space.getZ() + stackValue.getDz();
		}
		
		return dz;
	}

	@Override
	public long getVolume() {
		int volume = 0;
		
		for (StackEntry stackEntry : entries) {
			volume += stackEntry.getStackable().getVolume();
		}
		
		return volume;
	}
	
}
