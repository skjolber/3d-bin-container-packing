package com.github.skjolber.packing.impl;

import java.util.List;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;

public class LAFFResult implements PackResult {

	private List<Box> remaining;
	private Container container;

	public LAFFResult(List<Box> remaining, Container container) {
		this.remaining = remaining;
		this.container = container;
	}

	public Container getContainer() {
		return container;
	}

	@Override
	public boolean packsMoreBoxesThan(PackResult result) {
		LAFFResult laffResult = (LAFFResult)result;
		if(laffResult.remaining.size() > remaining.size()) { // lower is better
			return true;
		} else {
			laffResult.remaining.size();
			remaining.size();
		}
		return false;
	}

	public List<Box> getRemainingBoxes() {
		return remaining;
	}

	@Override
	public boolean isEmpty() {
		return container.getLevels().isEmpty() || container.getLevels().get(0).isEmpty();
	}

	@Override
	public String toString() {
		return "LAFFResult [container=" + container + "]";
	}


}
