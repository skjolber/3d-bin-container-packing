package com.github.skjolber.packing.iterator;

import com.github.skjolber.packing.iterator.DefaultStackableItemPermutationRotationIterator.Builder;

class StackableItemPermutationRotationIteratorTest extends AbstractStackableItemPermutationRotationIteratorTest<DefaultStackableItemPermutationRotationIterator.Builder> {

	@Override
	public Builder newBuilder() {
		return DefaultStackableItemPermutationRotationIterator.newBuilder();
	}

}
