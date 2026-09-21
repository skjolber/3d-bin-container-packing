package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.strategy.ContainerResult;

class ItemAwareContainerItemsCalculatorTest {

	@Test
	void adaptersCreateTheMatchingStandaloneCalculator() {
		PlainPackager packager = PlainPackager.newBuilder().build();
		try {
			List<Class<?>> calculatorTypes = new ArrayList<>();
			packager.setContainerPackingStrategyFactory((calculator, boxes, groups) -> {
				calculatorTypes.add(calculator.getClass());
				return (interrupt, adapter) -> {
					IntermediatePackagerResult result = adapter.attempt(0, null, true);
					Container packed = adapter.accept(result);
					return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), List.of(packed));
				};
			});
			Container container = Container.newBuilder().withId("container").withSize(2, 1, 1)
					.withMaxLoadWeight(2).build();
			List<ContainerItem> containers = List.of(new ContainerItem(container, 2));

			PackagerResult boxes = packager.newResultBuilder().withContainerItems(containers)
					.withMaxContainerCount(10).withBoxItems(new BoxItem(box("box", 1, 1))).build();
			PackagerResult groups = packager.newResultBuilder().withContainerItems(containers)
					.withMaxContainerCount(10)
					.withBoxItems(new BoxItemGroup("group", List.of(new BoxItem(box("box", 1, 1)))))
					.build();

			assertThat(boxes.isSuccess()).isTrue();
			assertThat(groups.isSuccess()).isTrue();
			assertThat(calculatorTypes).containsExactly(BoxItemsContainerItemsCalculator.class,
					BoxItemGroupsContainerItemsCalculator.class);
		} finally {
			packager.close();
		}
	}

	@Test
	void boxItemsCalculatorIndexesFitAndTracksRemainingCapacity() {
		Box small = box("small", 1, 2);
		Box large = box("large", 2, 3);
		BoxItem smallItems = new BoxItem(small, 2);
		BoxItem largeItem = new BoxItem(large, 1);
		BoxItemsContainerItemsCalculator calculator = new BoxItemsContainerItemsCalculator(
				controlledContainers(), 100, List.of(smallItems, largeItem));

		assertThat(calculator.getContainerCount()).isEqualTo(3);
		assertThat(calculator.getRemainingVolume()).isEqualTo(4);
		assertThat(calculator.getRemainingWeight()).isEqualTo(7);
		assertThat(calculator.getFittingContainerItemCount(0)).isEqualTo(2);
		assertThat(calculator.getFittingContainerItemCount(1)).isEqualTo(1);
		assertThat(calculator.canLoad(0, 0)).isTrue();
		assertThat(calculator.canLoad(1, 0)).isFalse();
		assertThat(calculator.canLoad(1, 1)).isTrue();
		smallItems.setIndex(0);
		largeItem.setIndex(1);
		assertThat(calculator.isFeasible(List.of(smallItems, largeItem))).isFalse();
		assertThat(calculator.isFeasible(List.of(smallItems))).isTrue();

		Stack stack = stack(small);
		calculator.toContainer(calculator.getContainerItem(1), stack);

		assertThat(calculator.getContainerCount()).isEqualTo(2);
		assertThat(calculator.getRemainingVolume()).isEqualTo(3);
		assertThat(calculator.getRemainingWeight()).isEqualTo(5);
		assertThat(calculator.hasContainer(0)).isTrue();
		assertThat(calculator.hasContainer(1)).isFalse();
		assertThat(calculator.isFeasible(List.of(smallItems, largeItem))).isFalse();
		assertThat(calculator.isFeasible(List.of(smallItems))).isFalse();

		BoxItemsContainerItemsCalculator clone = calculator.clone();
		clone.toContainer(clone.getContainerItem(0), stack(small));
		assertThat(clone.hasContainer(0)).isFalse();
		assertThat(calculator.hasContainer(0)).isTrue();

		calculator.reset();
		assertThat(calculator.getContainerCount()).isEqualTo(3);
		assertThat(calculator.getRemainingVolume()).isEqualTo(4);
		assertThat(calculator.getRemainingWeight()).isEqualTo(7);
		assertThat(calculator.hasContainer(1)).isTrue();
		assertThat(calculator.isFeasible(List.of(smallItems, largeItem))).isFalse();
	}

	@Test
	void groupCalculatorIndexesWholeGroupFitAndCapsByGroupCount() {
		Box small = box("small", 1, 2);
		BoxItemGroup pair = new BoxItemGroup("pair", List.of(new BoxItem(small, 2)));
		BoxItemGroup single = new BoxItemGroup("single", List.of(new BoxItem(small, 1)));
		BoxItemGroupsContainerItemsCalculator calculator = new BoxItemGroupsContainerItemsCalculator(
				controlledContainers(), 100, List.of(pair, single));

		assertThat(calculator.getContainerCount()).isEqualTo(2);
		assertThat(calculator.getRemainingVolume()).isEqualTo(3);
		assertThat(calculator.getRemainingWeight()).isEqualTo(6);
		assertThat(calculator.getFittingContainerItemCount(0)).isEqualTo(1);
		assertThat(calculator.getFittingContainerItemCount(1)).isEqualTo(2);
		assertThat(calculator.canLoad(0, 0)).isFalse();
		assertThat(calculator.canLoad(0, 1)).isTrue();
		pair.setIndex(0);
		single.setIndex(1);
		assertThat(calculator.isGroupFeasible(List.of(pair, single))).isTrue();

		calculator.toContainer(calculator.getContainerItem(1), stack(small, small));

		assertThat(calculator.getContainerCount()).isEqualTo(1);
		assertThat(calculator.getRemainingVolume()).isEqualTo(1);
		assertThat(calculator.getRemainingWeight()).isEqualTo(2);
		assertThat(calculator.hasContainer(0)).isFalse();
		assertThat(calculator.hasContainer(1)).isTrue();
		assertThat(calculator.isGroupFeasible(List.of(pair, single))).isFalse();
		assertThat(calculator.isGroupFeasible(List.of(single))).isTrue();

		BoxItemGroupsContainerItemsCalculator clone = calculator.clone();
		assertThat(clone.getRemainingVolume()).isEqualTo(1);
		assertThat(clone.hasContainer(0)).isFalse();

		calculator.reset();
		assertThat(calculator.getContainerCount()).isEqualTo(2);
		assertThat(calculator.getRemainingVolume()).isEqualTo(3);
		assertThat(calculator.getRemainingWeight()).isEqualTo(6);
		assertThat(calculator.hasContainer(0)).isTrue();
		assertThat(calculator.isGroupFeasible(List.of(pair, single))).isTrue();
	}

	@Test
	void containerResultRetainsBoxAndGroupFitRecords() {
		Box small = box("small", 1, 2);
		Box large = box("large", 2, 3);
		ContainerItemsCalculator calculator = new ContainerItemsCalculator(controlledContainers(), 2);

		ContainerItemsResult boxes = calculator.getContainers(
				List.of(new BoxItem(small), new BoxItem(large)));

		assertThat(boxes).containsExactly(0, 1);
		assertThat(boxes.getContainerIndexes()).containsExactly(0, 1);
		assertThat(boxes.getItemCount()).isEqualTo(2);
		assertThat(boxes.getContainerItemCount()).isEqualTo(2);
		assertThat(boxes.canLoad(0, 0)).isTrue();
		assertThat(boxes.canLoad(0, 1)).isTrue();
		assertThat(boxes.canLoad(1, 0)).isFalse();
		assertThat(boxes.canLoad(1, 1)).isTrue();
		assertThat(boxes.getFittingContainerItemCount(0)).isEqualTo(2);
		assertThat(boxes.getFittingContainerItemCount(1)).isEqualTo(1);
		assertThat(calculator.isFeasible(List.of(new BoxItem(small), new BoxItem(large)))).isTrue();
		assertThat(calculator.isFeasible(List.of(new BoxItem(small), new BoxItem(large)), 1)).isFalse();
		assertThat(calculator.isFeasible(List.of(new BoxItem(large)), 2, new boolean[] {false, true})).isFalse();

		BoxItemGroup pair = new BoxItemGroup("pair", List.of(new BoxItem(small, 2)));
		ContainerItemsResult groups = calculator.getGroupContainers(List.of(pair));

		assertThat(groups).containsExactly(1);
		assertThat(groups.hasContainer(0)).isTrue();
		assertThat(groups.canLoad(0, 0)).isFalse();
		assertThat(groups.canLoad(0, 1)).isTrue();
		assertThat(calculator.isGroupFeasible(List.of(pair))).isTrue();
		assertThat(calculator.isGroupFeasible(List.of(pair), 2, new boolean[] {false, true})).isFalse();
	}

	private static List<ControlledContainerItem> controlledContainers() {
		Container small = Container.newBuilder().withId("small").withSize(1, 1, 1)
				.withMaxLoadWeight(10).build();
		Container large = Container.newBuilder().withId("large").withSize(2, 1, 1)
				.withMaxLoadWeight(10).build();
		List<ControlledContainerItem> result = new ArrayList<>();
		result.add(new ControlledContainerItem(new ContainerItem(small, 1)));
		result.add(new ControlledContainerItem(new ContainerItem(large, 1)));
		return result;
	}

	private static Box box(String id, int dx, int weight) {
		return Box.newBuilder().withId(id).withSize(dx, 1, 1).withWeight(weight).build();
	}

	private static Stack stack(Box... boxes) {
		Stack stack = new Stack(boxes.length);
		for(Box box : boxes) {
			stack.add(new Placement(box.getStackValue(0), 0, 0, 0, 0));
		}
		return stack;
	}
}
