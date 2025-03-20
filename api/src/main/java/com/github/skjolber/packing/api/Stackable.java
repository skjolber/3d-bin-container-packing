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
		return rotations(bound.getDx(), bound.getDy(), bound.getDz());
	}

	public List<StackValue> rotations(int dx, int dy, int dz) {
		// TODO optimize if max is above min bounds 
		StackValue[] rotations = getStackValues();
		for (int i = 0; i < rotations.length; i++) {
			StackValue stackValue = rotations[i];
			if(stackValue.fitsInside3D(dx, dy, dz)) {
				List<StackValue> fitsInside = new ArrayList<>(rotations.length);
				fitsInside.add(stackValue);

				i++;
				while (i < rotations.length) {
					if(rotations[i].fitsInside3D(dx, dy, dz)) {
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
	
	public abstract long getMinimumPressure();

	public abstract long getMaximumPressure();

	public static StackValue getMinimumArea(StackValue[] rotations) {
		StackValue minimumArea = null;
		for (StackValue boxStackValue : rotations) {
			if(minimumArea == null || boxStackValue.getArea() < minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}
	
	public static StackValue getMinimumPressure(StackValue[] rotations) {
		StackValue minimumArea = null;
		for (StackValue boxStackValue : rotations) {
			if(minimumArea == null || boxStackValue.getArea() < minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}

	public static StackValue getMaximumArea(StackValue[] rotations) {
		StackValue minimumArea = null;
		for (StackValue boxStackValue : rotations) {
			if(minimumArea == null || boxStackValue.getArea() > minimumArea.getArea()) {
				minimumArea = boxStackValue;
			}
		}
		return minimumArea;
	}

}
