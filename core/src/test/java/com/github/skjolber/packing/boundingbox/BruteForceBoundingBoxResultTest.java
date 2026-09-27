package com.github.skjolber.packing.boundingbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;

class BruteForceBoundingBoxResultTest {

	@Test
	void retainsConstructorCollectionsAndSharedLayoutsWithoutCopies() {
		Stack stack = new Stack();
		BoundingBox bounds = new BoundingBox(1, 1, 1);
		BoundingBoxLayout layout = new BoundingBoxLayout(bounds, stack);
		Map<String, BoundingBoxLayout> winners = new LinkedHashMap<>();
		winners.put("width", layout);
		winners.put("height", layout);
		Set<String> goals = new LinkedHashSet<>();
		goals.add("height");
		BruteForceBoundingBoxResult result = new BruteForceBoundingBoxResult(winners, goals, Termination.INTERRUPTED, 12);

		assertThat(result.getObjectiveResults()).isSameAs(winners);
		assertThat(result.getReachedGoals()).isSameAs(goals);
		assertThat(result.getResults()).isSameAs(winners.values()).containsExactly(layout, layout);
		assertThat(layout.getStack()).isSameAs(stack);
		assertThat(layout.getBoundingBox()).isSameAs(bounds);
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getDuration()).isEqualTo(12);
		assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
	}

	@Test
	void emptyResultsNeedNoSyntheticStackOrLayout() {
		BruteForceBoundingBoxResult result = new BruteForceBoundingBoxResult(Map.of(), Set.of(), Termination.EXHAUSTED, 0);
		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getResults()).isEmpty();
		assertThat(result.getObjectiveResults()).isEmpty();
		assertThat(result.getReachedGoals()).isEmpty();
	}

	@Test
	void defaultBuilderBorrowsInventoryAndDoesNotAllocateAnObjectiveCollection() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			List<BoxItem> inventory = new ArrayList<>();
			BruteForceBoundingBoxResultBuilder builder = search.newResultBuilder().withBoxItems(inventory);
			assertThat(builder.items).isSameAs(inventory);
			assertThat(builder.objectives).isNull();
		}
	}
}
