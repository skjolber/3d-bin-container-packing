package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.Dimension;

public abstract class Stackable {

	protected final String name;

	public Stackable(String name) {
		super();
		this.name = name;
	}

	public abstract long getVolume();
	
	public abstract int getWeight();
	
	public abstract StackValue[] getStackValues();
	
	public String getName() {
		return name;
	}

	public List<StackValue> fitsInside(Dimension bound) {
		List<StackValue> list = new ArrayList<>();
		
		for (StackValue stackValue : getStackValues()) {
			if(stackValue.fitsInside3D(bound)) {
				list.add(stackValue);
			}
		}
		
		return list;
	}

	public abstract Stackable rotations(Dimension bound);

	
}
