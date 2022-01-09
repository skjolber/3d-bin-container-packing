package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

public class DefaultStack extends Stack {

	protected final List<StackPlacement> entries = new ArrayList<>();
	
	public DefaultStack() {
	}
	
	public DefaultStack(ContainerStackValue containerStackValue) {
		super(containerStackValue);
	}

	public List<StackPlacement> getPlacements() {
		return entries;
	}

	public void add(StackPlacement e) {
		entries.add(e);
	}
	
	@Override
	public void remove(StackPlacement e) {
		entries.remove(e);
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
			dz = Math.max(dz, stackEntry.getAbsoluteEndZ());
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
	
	@Override
	public int getSize() {
		return entries.size();
	}
	
}
