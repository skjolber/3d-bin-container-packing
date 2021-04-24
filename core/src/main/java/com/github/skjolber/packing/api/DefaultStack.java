package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

public class DefaultStack extends Stack {

	protected final List<StackPlacement> entries = new ArrayList<>();

	public List<StackPlacement> getPlacements() {
		return entries;
	}

	public void add(StackPlacement e) {
		entries.add(e);
	}

	public void clear() {
		entries.clear();
	}

	@Override
	public int getWeight() {
		int weight = 0;
		
		for (StackPlacement stackEntry : entries) {
			weight += stackEntry.getStackable().getWeight();
		}
		
		return weight;
	}

	@Override
	public int getDz() {
		int dz = 0;
		
		for (StackPlacement stackEntry : entries) {
			StackValue stackValue = stackEntry.getStackValue();
			StackSpace space = stackEntry.getSpace();
			dz += space.getZ() + stackValue.getDz();
		}
		
		return dz;
	}

	@Override
	public long getVolume() {
		int volume = 0;
		
		for (StackPlacement stackEntry : entries) {
			volume += stackEntry.getStackable().getVolume();
		}
		
		return volume;
	}
	
	@Override
	public boolean isEmpty() {
		return entries.isEmpty();
	}
	
}
