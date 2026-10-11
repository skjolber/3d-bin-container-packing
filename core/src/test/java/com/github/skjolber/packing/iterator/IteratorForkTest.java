package com.github.skjolber.packing.iterator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

class IteratorForkTest {

	@Test
	void defaultBoxIteratorForkKeepsItsOwnInventoryAndPermutation() {
		DefaultBoxItemPermutationRotationIterator source = DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(4, 1, 1).withMaxLoadWeight(4).withBoxItems(items()).build();
		source.nextPermutation();
		DefaultBoxItemPermutationRotationIterator fork = source.fork();

		assertThat(fork.getPermutations()).containsExactly(source.getPermutations());
		fork.removePermutations(List.of(0));
		assertThat(fork.length()).isEqualTo(3);
		assertThat(source.length()).isEqualTo(4);
	}

	@Test
	void parallelBoxIteratorForkKeepsItsOwnWorkUnits() {
		ParallelBoxItemPermutationRotationIteratorList source = ParallelBoxItemPermutationRotationIteratorList.newBuilder()
				.withLoadSize(4, 1, 1).withMaxLoadWeight(4).withBoxItems(items())
				.withParallelizationCount(2).build();
		ParallelBoxItemPermutationRotationIteratorList fork = source.fork();

		fork.removePermutations(List.of(0));
		assertThat(fork.getIterators()[0].length()).isEqualTo(3);
		assertThat(source.getIterators()[0].length()).isEqualTo(4);
	}

	@Test
	void groupIteratorForksKeepTheirOwnGroups() {
		DefaultBoxItemGroupPermutationRotationIterator source = DefaultBoxItemGroupPermutationRotationIterator.newBuilder()
				.withLoadSize(4, 1, 1).withMaxLoadWeight(4).withBoxItemGroups(groups()).build();
		DefaultBoxItemGroupPermutationRotationIterator fork = source.fork();
		fork.removeGroups(List.of(0));
		assertThat(fork.getBoxItemGroups()[0]).isNull();
		assertThat(source.getBoxItemGroups()[0]).isNotNull();

		ParallelBoxItemGroupPermutationRotationIteratorList parallel = ParallelBoxItemGroupPermutationRotationIteratorList.newBuilder()
				.withLoadSize(4, 1, 1).withMaxLoadWeight(4).withBoxItemGroups(groups())
				.withParallelizationCount(2).build();
		ParallelBoxItemGroupPermutationRotationIteratorList parallelFork = parallel.fork();
		parallelFork.removeGroups(List.of(0));
		assertThat(parallelFork.getBoxItemGroups()[0]).isNull();
		assertThat(parallel.getBoxItemGroups()[0]).isNotNull();
	}

	private List<BoxItem> items() {
		return List.of(item(), item(), item(), item());
	}

	private List<BoxItemGroup> groups() {
		return List.of(new BoxItemGroup("first", List.of(item(), item())),
				new BoxItemGroup("second", List.of(item(), item())));
	}

	private BoxItem item() {
		return new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).build());
	}
}
