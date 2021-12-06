package com.github.skjolber.packing.packer;

public class EmptyPackResult implements PackResult {
	
	public static PackResult EMPTY = new EmptyPackResult();

	@Override
	public boolean isBetterThan(PackResult result) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean containsLastStackable() {
		return false;
	}

}
