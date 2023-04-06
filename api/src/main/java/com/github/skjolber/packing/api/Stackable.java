package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class Stackable implements Serializable {

	private static final long serialVersionUID = 1L;

	protected final String id;
	protected final String description;

	public Stackable(String id, String description) {
		super();
		this.id = id;
		this.description = description;
	}

	public abstract BigDecimal getVolume();

	public abstract BigDecimal getWeight();

	public abstract StackValue[] getStackValues();

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
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

	public abstract BigDecimal getMinimumArea();

	public abstract BigDecimal getMaximumArea();

	protected static BigDecimal getMinimumArea(StackValue[] rotations) {
		BigDecimal minimumArea = BigDecimal.valueOf(Long.MAX_VALUE);
		for (StackValue boxStackValue : rotations) {
			if(minimumArea.compareTo(boxStackValue.getArea()) > 0) {
				minimumArea = boxStackValue.getArea();
			}
		}
		return minimumArea;
	}

	public static BigDecimal getMaximumArea(StackValue[] rotations) {
		BigDecimal maximumArea = BigDecimal.valueOf(Long.MIN_VALUE);
		for (StackValue boxStackValue : rotations) {
			if(maximumArea.compareTo(boxStackValue.getArea()) < 0) {
				maximumArea = boxStackValue.getArea();
			}
		}
		return maximumArea;
	}

}
