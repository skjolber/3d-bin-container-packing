package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.iterator.DefaultStackableItemGroupPermutationRotationIterator.Builder;

public class DefaultStackableItemGroupPermutationRotationIteratorTest extends AbstractBoxItemGroupPermutationRotationIteratorTest<DefaultStackableItemGroupPermutationRotationIterator.Builder> {

	@Override
	public Builder newBuilder() {
		return DefaultStackableItemGroupPermutationRotationIterator.newBuilder();
	}

}
