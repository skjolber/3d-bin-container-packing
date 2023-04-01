package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;

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

	@Override
	public abstract Stackable clone();
	
	public List<StackValue> rotations(Dimension bound) {
		// TODO optimize if max is above min bounds 
		StackValue[] rotations = getStackValues();
		for (int i = 0; i < rotations.length; i++) {
			StackValue stackValue = rotations[i];
			if(stackValue.fitsInside3D(bound)) {
				List<StackValue> fitsInside = new ArrayList<>(rotations.length);
				fitsInside.add(stackValue);
				
				i++;
				while(i < rotations.length) {
					if(rotations[i].fitsInside3D(bound)) {
						fitsInside.add(rotations[i]);
					}
					i++;
				}
				return fitsInside;
			}
		}
		return null;
	}
	
}
