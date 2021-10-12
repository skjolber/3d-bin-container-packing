package com.github.skjolber.packing.packer.laff;

import java.util.List;

import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.packer.PackResult;

public class LaffResult implements PackResult {

	private List<Stackable> remaining;
	private LevelStack stack;

	public LaffResult(List<Stackable> remaining, LevelStack stack) {
		this.remaining = remaining;
		this.stack = stack;
	}

	public LevelStack getContainer() {
		return stack;
	}

	@Override
	public boolean packsMoreBoxesThan(PackResult result) {
		LaffResult laffResult = (LaffResult)result;
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
