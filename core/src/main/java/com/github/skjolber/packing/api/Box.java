package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Dimension;

public class Box extends Stackable {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractStackableBuilder<Builder> {
		
		protected Integer weight;
		
		public Builder withWeight(int weight) {
			this.weight = weight;
			
			return this;
		}

		public Box build() {
			if(rotations.isEmpty()) {
				throw new IllegalStateException("No rotations");
			}
			if(weight == null) {
				throw new IllegalStateException("No weight");
			}
			
			BoxStackValue[] stackValues = new BoxStackValue[rotations.size()];
			
			for (int i = 0; i < rotations.size(); i++) {
				Rotation rotation = rotations.get(i);
				
				stackValues[i] = new BoxStackValue(rotation.dx, rotation.dy, rotation.dz, weight, rotation.maxSupportedWeight, rotation.maxSupportedCount, pressureReference);
			}	
			return new Box(name, stackValues[0].getVolume(), weight, stackValues);
		}
	}
	
	protected final int weight;
	protected final BoxStackValue[] rotations;
	protected final long volume;

	public Box(String name, long volume, int weight, BoxStackValue[] rotations) {
		super(name);
		this.volume = volume;
		this.weight = weight;
		this.rotations = rotations;
	}

	@Override
	public int getWeight() {
		return weight;
	}

	@Override
	public BoxStackValue[] getStackValues() {
		return rotations;
	}

	public long getVolume() {
		return volume;
	}

	@Override
	public Box rotations(Dimension bound) {
		// TODO optimiazate if max is above min bounds 
		for (int i = 0; i < rotations.length; i++) {
			BoxStackValue stackValue = rotations[i];
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
				return new Box(name, volume, weight, fitsInside.toArray(new BoxStackValue[fitsInside.size()]));
			}
		}
		return null;
	}

	@Override
	public Box clone() {
		return new Box(name, volume, weight, rotations);
	}
	
}
