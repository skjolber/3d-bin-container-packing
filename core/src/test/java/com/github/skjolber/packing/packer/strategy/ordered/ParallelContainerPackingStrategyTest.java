package com.github.skjolber.packing.packer.strategy.ordered;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.plain.PlainPackager;

class ParallelContainerPackingStrategyTest {

	@Test
	void acceptsTheBestResultProducedByAnAdapterFork() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			packager.setContainerPackingStrategyFactory((calculator, boxes, groups) ->
					new ParallelContainerPackingStrategy(packager.getScheduledThreadPoolExecutor(),
							new DefaultIntermediatePackagerResultComparator()));

			Container small = Container.newBuilder().withId("small").withSize(1, 1, 1).withMaxLoadWeight(1).build();
			Container large = Container.newBuilder().withId("large").withSize(2, 1, 1).withMaxLoadWeight(2).build();
			Box box = Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build();

			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(small, 2), new ContainerItem(large, 1)))
					.withMaxContainerCount(2)
					.withBoxItems(new BoxItem(box, 2))
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(result.getContainers()).singleElement().extracting(Container::getId).isEqualTo("large");
		} finally {
			packager.close();
		}
	}
}
