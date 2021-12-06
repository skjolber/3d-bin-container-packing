package com.github.skjolber.packing.packer;

public interface PackResult {
	/**
	 * Compare two results
	 *
	 * @param result to compare against
	 * @return true if this box is better than the argument
	 */
	boolean isBetterThan(PackResult result);
	
	boolean containsLastStackable();
	
	boolean isEmpty();

}
