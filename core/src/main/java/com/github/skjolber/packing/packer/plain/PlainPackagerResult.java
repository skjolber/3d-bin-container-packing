package com.github.skjolber.packing.packer.plain;

import java.util.Collections;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultContainerStackValue;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.packer.PackResult;

public class PlainPackagerResult implements PackResult {
	
	public static PlainPackagerResult EMPTY = new PlainPackagerResult(new DefaultStack(new DefaultContainerStackValue(0, 0, 0, null, 0, 0, 0, 0, Collections.emptyList())), null, false);
	
	private Stack stack;
	private Container container;
	private boolean last;

	public PlainPackagerResult(Stack stack, Container container, boolean last) {
		this.stack = stack;
		this.container = container;
		this.last = last;
	}

	public Container getContainer() {
		return container;
	}

	@Override
	public boolean isBetterThan(PackResult result) {
		PlainPackagerResult plainResult = (PlainPackagerResult)result;
		if(stack.getSize() >= plainResult.stack.getSize()) {
			return true;
		} else if(stack.getSize() == plainResult.stack.getSize()) {
			return container.getWeight() >= plainResult.container.getWeight();
		}
		
		return false;
	}

	@Override
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	@Override
	public String toString() {
		return "PlainPackagerResult [stack=" + stack + "]";
	}

	@Override
	public boolean containsLastStackable() {
		return last;
	}


}
