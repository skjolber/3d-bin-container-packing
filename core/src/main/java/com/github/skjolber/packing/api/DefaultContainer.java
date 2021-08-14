package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.AbstractContainerBuilder.Rotation;

public class DefaultContainer extends Container {

	protected final ContainerStackValue[] stackValues;
	protected final Stack stack;
	
	public DefaultContainer(Builder builder) {
		super(builder.name, builder.rotations.get(0).getVolume(), builder.emptyWeight, builder.getMaxLoadVolume(), builder.getMaxLoadWeight());
			
		List<Rotation> rotations = builder.rotations;

		this.stack = builder.stack;

		if(builder.fixed) {
			FixedContainerStackValue[] stackValues = new FixedContainerStackValue[rotations.size()];
			
			int stackWeight = stack.getWeight();

			for (int i = 0; i < rotations.size(); i++) {
				Rotation rotation = rotations.get(i);

				StackConstraint constraint = rotation.stackConstraint != null ? rotation.stackConstraint : builder.defaultConstraint;
				
				stackValues[i] = new FixedContainerStackValue(
						rotation.dx, rotation.dy, rotation.dz, 
						constraint , 
						stackWeight,
						rotation.loadDx, rotation.loadDy, rotation.loadDz, 
						rotation.getMaxLoadWeight(),
						this
						);
			}
			this.stackValues = stackValues;
		} else {
			DefaultContainerStackValue[] stackValues = new DefaultContainerStackValue[rotations.size()];
			
			for (int i = 0; i < rotations.size(); i++) {
				Rotation rotation = rotations.get(i);

				StackConstraint constraint = rotation.stackConstraint != null ? rotation.stackConstraint : builder.defaultConstraint;

				stackValues[i] = new DefaultContainerStackValue(
						rotation.dx, rotation.dy, rotation.dz, 
						constraint,
						rotation.loadDx, rotation.loadDy, rotation.loadDz, 
						rotation.getMaxLoadWeight(),
						this,
						builder.stack
						);
			}
			this.stackValues = stackValues;
		}		
	}

	@Override
	public ContainerStackValue[] getStackValues() {
		return stackValues;
	}

	@Override
	public Stack getStack() {
		return stack;
	}

	@Override
	public DefaultContainer clone() {
		throw new RuntimeException();
	}
	
	@Override
	public boolean canLoad(Stackable stackable) {
		if(stackable.getVolume() > maxLoadVolume) {
			return false;
		}
		if(stackable.getWeight() > maxLoadWeight) {
			return false;
		}
		for(ContainerStackValue stackValue : stackValues) {
			if(stackValue.canLoad(stackable)) {
				return true;
			}
		}
		
		return false;
	}
	
}
