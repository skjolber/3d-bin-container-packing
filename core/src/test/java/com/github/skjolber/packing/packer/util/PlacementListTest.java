package com.github.skjolber.packing.packer.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Placement;

class PlacementListTest {

	@Test
	void addsAllPlacementsInOrder() {
		Placement first = new Placement();
		Placement second = new Placement();
		Placement third = new Placement();
		PlacementList destination = new PlacementList(1);
		destination.add(first);
		PlacementList source = new PlacementList(2);
		source.add(second);
		source.add(third);

		destination.addAll(source);

		assertThat(destination.size()).isEqualTo(3);
		assertThat(destination.get(0)).isSameAs(first);
		assertThat(destination.get(1)).isSameAs(second);
		assertThat(destination.get(2)).isSameAs(third);
	}

	@Test
	void canAddItself() {
		Placement first = new Placement();
		Placement second = new Placement();
		PlacementList list = new PlacementList(2);
		list.add(first);
		list.add(second);

		list.addAll(list);

		assertThat(list.size()).isEqualTo(4);
		assertThat(list.get(0)).isSameAs(first);
		assertThat(list.get(1)).isSameAs(second);
		assertThat(list.get(2)).isSameAs(first);
		assertThat(list.get(3)).isSameAs(second);
	}
}
