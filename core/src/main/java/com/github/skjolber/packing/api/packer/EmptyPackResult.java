package com.github.skjolber.packing.api.packer;

public class EmptyPackResult implements PackResult {

	@Override
	public boolean packsMoreBoxesThan(PackResult result) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

}
