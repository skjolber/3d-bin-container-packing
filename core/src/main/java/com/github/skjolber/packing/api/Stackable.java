package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public abstract class Stackable {

	protected static final Comparator<StackValue> AREA = new Comparator<StackValue>() {
		@Override
		public int compare(StackValue o1, StackValue o2) {
			return Long.compare(o1.area, o2.area);
		}
	};
	
	protected static final Comparator<StackValue> VOLUME = new Comparator<StackValue>() {
		@Override
		public int compare(StackValue o1, StackValue o2) {
			return Long.compare(o1.volume, o2.volume);
		}
	};
	
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

	public StackValue largestArea() {
		return best(AREA);
	}

	public StackValue largestVolume() {
		return best(VOLUME);
	}

	public StackValue best(Comparator<StackValue> comparator) {
		StackValue[] stackValues = getStackValues();
		
		StackValue current = stackValues[0];
		for(int i = 1; i < stackValues.length; i++) {
			if(comparator.compare(current, stackValues[i]) < 0) {
				current = stackValues[i];
			}
		}
		
		return current;
	}

	public StackValue best(Predicate<StackValue> predicate) {
		StackValue[] stackValues = getStackValues();
		
		for(StackValue stackValue : stackValues) {
			if(predicate.test(stackValue)) {
				return stackValue;
			}
		}
		
		return null;
	}

	public StackValue[] rank(Comparator<StackValue> comparator) {
		StackValue[] stackValues = getStackValues();

		StackValue[] sorted = new StackValue[stackValues.length];
		System.arraycopy(stackValues, 0, sorted, 0, stackValues.length);
		Arrays.sort(sorted, comparator);
		
		return sorted;
	}

}
