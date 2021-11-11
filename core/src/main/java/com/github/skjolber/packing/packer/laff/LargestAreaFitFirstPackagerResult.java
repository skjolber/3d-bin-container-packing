package com.github.skjolber.packing.packer.laff;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.packer.PackResult;

public class LargestAreaFitFirstPackagerResult implements PackResult {

	private List<Stackable> remaining;
	private LevelStack stack;
	private Container container;

	public LargestAreaFitFirstPackagerResult(List<Stackable> remaining, LevelStack stack, Container container) {
		this.remaining = remaining;
		this.stack = stack;
		this.container = container;
	}

	public Container getContainer() {
		return container;
	}

	@Override
	public boolean packsMoreBoxesThan(PackResult result) {
		LargestAreaFitFirstPackagerResult laffResult = (LargestAreaFitFirstPackagerResult)result;
		if(laffResult.remaining.size() > remaining.size()) { // lower is better
			return true;
		} else {
			laffResult.remaining.size();
			remaining.size();
		}
		return false;
	}

	public List<Stackable> getRemainingBoxes() {
		return remaining;
	}

	@Override
	public boolean isEmpty() {
		return stack.getLevels().isEmpty();
	}

	@Override
	public String toString() {
		return "LaffResult [stack=" + stack + "]";
	}


}
