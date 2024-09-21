package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Stackable implements Serializable {

	private static final long serialVersionUID = 1L;

	public abstract long getVolume();

	public abstract int getWeight();

	public abstract StackValue[] getStackValues();

	public abstract String getDescription();

	public abstract String getId();

	public List<StackValue> fitsInside(Dimension bound) {
		List<StackValue> list = new ArrayList<>();

		for (StackValue stackValue : getStackValues()) {
			if(stackValue.fitsInside3D(bound)) {
				list.add(stackValue);
			}
		}

		return list;
	}
	
	public abstract StackValue getStackValue(int index);

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
				while (i < rotations.length) {
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

	public abstract long getMinimumArea();

	public abstract long getMaximumArea();

	protected static long getMinimumArea(StackValue[] rotations) {
		long minimumArea = Long.MAX_VALUE;
		for (StackValue boxStackValue : rotations) {
			if(minimumArea > boxStackValue.getArea()) {
				minimumArea = boxStackValue.getArea();
			}
		}
		return minimumArea;
	}

	public static long getMaximumArea(StackValue[] rotations) {
		long maximumArea = Long.MIN_VALUE;
		for (StackValue boxStackValue : rotations) {
			if(maximumArea < boxStackValue.getArea()) {
				maximumArea = boxStackValue.getArea();
			}
		}
		return maximumArea;
	}

}
