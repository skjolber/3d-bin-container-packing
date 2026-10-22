package com.github.skjolber.packing.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;

class SupportGraphTest {

	@Test
	void isolatedPlacementKeepsMutableIndependentAdjacencyLists() {
		// Both boxes are on the floor, with disjoint footprints:
		// x: [ A A ][ B B ]
		Box box = Box.newBuilder()
				.withSize(2, 1, 1)
				.withWeight(1)
				.build();
		Placement first = new Placement(box.getStackValue(0), 0, 0, 0, 0);
		Placement second = new Placement(box.getStackValue(0), 0, 2, 0, 0);
		SupportGraph graph = new SupportGraph(List.of(first, second));

		List<PlacementLoad> supporters = graph.getSupporters(first);
		List<PlacementLoad> supportees = graph.getSupportees(first);
		assertThat(supporters).isEmpty();
		assertThat(supportees).isEmpty();

		PlacementLoad supporter = new PlacementLoad(second, 1);
		PlacementLoad supportee = new PlacementLoad(second, 2);
		supporters.add(supporter);
		supportees.add(supportee);

		assertThat(graph.getSupporters(first)).isSameAs(supporters).containsExactly(supporter);
		assertThat(graph.getSupportees(first)).isSameAs(supportees).containsExactly(supportee);
		assertThat(graph.getSupporters(second)).isEmpty();
		assertThat(graph.getSupportees(second)).isEmpty();
	}
}
