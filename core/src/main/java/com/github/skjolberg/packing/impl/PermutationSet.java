package com.github.skjolberg.packing.impl;

import java.util.List;

public interface PermutationSet {
	
	void removePermutations(int count);

	/**
	 * Remove permutations, if present.
	 */

	void removePermutations(List<Integer> removed);

}
