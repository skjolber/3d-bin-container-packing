package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultContainerStackValue;
import com.github.skjolber.packing.packer.PackResult;

public class LargestAreaFitFirstPackagerResult implements PackResult {
	
	public static LargestAreaFitFirstPackagerResult EMPTY = new LargestAreaFitFirstPackagerResult(new LevelStack(new DefaultContainerStackValue(0, 0, 0, null, 0, 0, 0, 0)), null, false);
	
	private LevelStack stack;
	private Container container;
	private boolean last;

	public LargestAreaFitFirstPackagerResult(LevelStack stack, Container container, boolean last) {
		this.stack = stack;
		this.container = container;
		this.last = last;
	}

	public Container getContainer() {
		return container;
	}

	@Override
	public boolean isBetterThan(PackResult result) {
		LargestAreaFitFirstPackagerResult laffResult = (LargestAreaFitFirstPackagerResult)result;
		if(stack.getSize() >= laffResult.stack.getSize()) {
			return true;
		} else if(stack.getSize() == laffResult.stack.getSize()) {
			return container.getWeight() >= laffResult.container.getWeight();
		}
		
		return false;
	}

	@Override
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	@Override
	public String toString() {
		return "LaffResult [stack=" + stack + "]";
	}

	@Override
	public boolean containsLastStackable() {
		return last;
	}


}
