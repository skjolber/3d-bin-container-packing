package com.github.skjolber.packing.iterator;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;

public class ParallelBoxItemPermutationRotationIterator extends DefaultBoxItemPermutationRotationIterator {

	private int[] lastPermutation;
	private int lastPermutationMaxIndex;
	private boolean checkLastPermutation = false;

	// parent iterator
	private ParallelBoxItemPermutationRotationIteratorList iterator;

	public ParallelBoxItemPermutationRotationIterator(BoxItem[] boxItems, BoxStackValue[][] stackValues, ParallelBoxItemPermutationRotationIteratorList iterator) {
		super(boxItems, stackValues, Collections.emptyList());

		this.iterator = iterator;
	}

	private ParallelBoxItemPermutationRotationIterator(ParallelBoxItemPermutationRotationIterator source,
			ParallelBoxItemPermutationRotationIteratorList parent) {
		super(source);
		this.iterator = parent;
		this.lastPermutation = source.lastPermutation == null ? null : source.lastPermutation.clone();
		this.firstPermutation = source.firstPermutation == null ? null : source.firstPermutation.clone();
		this.lastPermutationMaxIndex = source.lastPermutationMaxIndex;
		this.checkLastPermutation = source.checkLastPermutation;
	}

	public ParallelBoxItemPermutationRotationIterator fork(ParallelBoxItemPermutationRotationIteratorList parent) {
		return new ParallelBoxItemPermutationRotationIterator(this, parent);
	}

	/** The first permutation of the work unit */
	private int[] firstPermutation;

	public void setPermutations(int[] permutations) {
		this.permutations = permutations;
		this.firstPermutation = permutations.clone();
	}

	@Override
	public void reset() {
		if(firstPermutation == null) {
			// no boxes
			return;
		}
		// back to the first permutation of the work unit
		System.arraycopy(firstPermutation, 0, permutations, 0, permutations.length);
		System.arraycopy(reset, 0, rotations, 0, rotations.length);
		checkLastPermutation = false;
		if(permutations.length > 0) {
			calculateMinStackableVolume(0);
		}
	}

	public int[] getLastPermutation() {
		return lastPermutation;
	}

	public void setLastPermutation(int[] lastPermutation) {
		this.lastPermutation = lastPermutation;

		this.checkLastPermutation = false;

		// find the first item that differs, so that we do not have to
		// compare items for each iteration (to detect whether we have done enough work)
		for (int k = 0; k < lastPermutation.length; k++) {
			if(permutations[k] != lastPermutation[k]) {
				lastPermutationMaxIndex = k;

				break;
			}
		}
	}

	public int getLastPermutationMaxIndex() {
		return lastPermutationMaxIndex;
	}

	public int nextPermutation() {
		int resultIndex = super.nextPermutation();
		
		return returnPermuationWithinRangeOrMinusOne(resultIndex);
	}

	public int nextPermutation(int maxIndex) {
		int resultIndex = super.nextPermutation(maxIndex);
		
		return returnPermuationWithinRangeOrMinusOne(resultIndex);
	}

	private int returnPermuationWithinRangeOrMinusOne(int resultIndex) {
		if(lastPermutation != null) {
			if(resultIndex <= lastPermutationMaxIndex) {
				// TODO initial check for bounds here
				checkLastPermutation = true;
			}

			if(checkLastPermutation) {
				// are we still within our designated range?
				// the next permutation must be lexicographically less than the first permutation
				// in the next block

				int i = 0;
				while (i < lastPermutation.length) {
					int value = permutations[i];
					if(value < lastPermutation[i]) {
						return resultIndex;
					} else if(value > lastPermutation[i]) {
						return -1;
					}
					i++;
				}
				// so all most be equal
				// we are at the exact last permutations
				return -1;
			}
		}

		return resultIndex;
	}

	@Override
	public PermutationRotationState getState() {
		return new PermutationRotationState(getRotations(), getPermutations());
	}

	@Override
	public void removePermutations(List<Integer> removed) {
		iterator.removePermutations(removed);
	}

	public long countPermutations() {
		return iterator.countPermutations();
	}

	public void setReset(int[] reset) {
		this.reset = reset;
	}

	public void setRotations(int[] rotations) {
		this.rotations = rotations;
	}

}
