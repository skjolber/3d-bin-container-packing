package com.github.skjolber.packing.virtualbox.bounds;

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
import com.github.skjolber.packing.virtualbox.VirtualBoxLayout;
import com.github.skjolber.packing.virtualbox.bounds.BruteForceVirtualBoxBoundsGenerator;
import com.github.skjolber.packing.virtualbox.bounds.BruteForceVirtualBoxBoundsResultBuilder;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsResult;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsResult.Termination;

class VirtualBoxBoundsResultTest {

	@Test
	void retainsConstructorCollectionsAndSharedLayoutsWithoutCopies() {
		Stack stack = new Stack();
		VirtualBoxBounds bounds = new VirtualBoxBounds(1, 1, 1);
		VirtualBoxLayout layout = new VirtualBoxLayout(bounds, stack.getPlacements());
		Map<String, VirtualBoxLayout> winners = new LinkedHashMap<>();
		winners.put("width", layout);
		winners.put("height", layout);
		Set<String> goals = new LinkedHashSet<>();
		goals.add("height");
		VirtualBoxBoundsResult result = new VirtualBoxBoundsResult(winners, goals, Termination.INTERRUPTED, 12);

		assertThat(result.getObjectiveResults()).isSameAs(winners);
		assertThat(result.getReachedGoals()).isSameAs(goals);
		assertThat(result.getResults()).isSameAs(winners.values()).containsExactly(layout, layout);
		assertThat(layout.getPlacements()).isSameAs(stack.getPlacements());
		assertThat(layout.getBoundingBox()).isSameAs(bounds);
		assertThat(result.isSuccess()).isTrue();
		assertThat(result.getDuration()).isEqualTo(12);
		assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
	}

	@Test
	void emptyResultsNeedNoSyntheticStackOrLayout() {
		VirtualBoxBoundsResult result = new VirtualBoxBoundsResult(Map.of(), Set.of(), Termination.EXHAUSTED, 0);
		assertThat(result.isSuccess()).isFalse();
		assertThat(result.getResults()).isEmpty();
		assertThat(result.getObjectiveResults()).isEmpty();
		assertThat(result.getReachedGoals()).isEmpty();
	}

	@Test
	void defaultBuilderBorrowsInventoryAndDoesNotAllocateAnObjectiveCollection() {
		try(BruteForceVirtualBoxBoundsGenerator search = new BruteForceVirtualBoxBoundsGenerator()) {
			List<BoxItem> inventory = new ArrayList<>();
			BruteForceVirtualBoxBoundsResultBuilder builder = search.newResultBuilder().withBoxItems(inventory);
			assertThat(builder.items).isSameAs(inventory);
			assertThat(builder.objectives).isNull();
		}
	}
}
