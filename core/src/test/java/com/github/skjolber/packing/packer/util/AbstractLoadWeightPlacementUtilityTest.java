package com.github.skjolber.packing.packer.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

class AbstractLoadWeightPlacementUtilityTest {

	@Test
	void indexesSupportersByTopZ() {
		Stack stack = new Stack();
		Placement first = placement("first", 0, 0, 0, 2, 2, 1, 0);
		Placement second = placement("second", 3, 0, 0, 2, 2, 1, 1);
		Placement otherLevel = placement("other", 0, 0, 2, 2, 2, 1, 2);
		stack.add(first);
		stack.add(second);
		stack.add(otherLevel);
		addFillers(stack, 3, 29);

		TestUtility utility = new TestUtility(stack);
		utility.initialize(32);
		utility.populatePointSupporters(point(0, 0, 1, 5, 1, 2));

		assertThat(utility.pointSupporters()).containsExactly(first, second);
	}

	@Test
	void preservesStackOrderAcrossSupporteeZRange() {
		Stack stack = new Stack();
		Placement high = placement("high", 0, 0, 3, 2, 2, 1, 0);
		Placement low = placement("low", 0, 0, 1, 2, 2, 1, 1);
		stack.add(high);
		stack.add(low);
		addFillers(stack, 2, 30);

		TestUtility utility = new TestUtility(stack);
		utility.initialize(32);
		utility.populatePointSupportees(point(0, 0, 0, 1, 1, 4), 1, 3);

		assertThat(utility.pointSupportees()).containsExactly(high, low);
	}

	@Test
	void reindexesPlacementReusedAtAnotherZ() {
		Stack stack = new Stack();
		addFillers(stack, 0, 31);
		Placement reused = placement("reused", 0, 0, 0, 2, 2, 1, 31);
		stack.add(reused);

		TestUtility utility = new TestUtility(stack);
		utility.initialize(32);
		utility.populatePointSupporters(point(0, 0, 1, 1, 1, 1));
		assertThat(utility.pointSupporters()).containsExactly(reused);

		stack.remove(stack.size() - 1);
		reused.setPoint(0, 0, 0, 4);
		stack.add(reused);

		utility.populatePointSupporters(point(0, 0, 1, 1, 1, 1));
		assertThat(utility.pointSupporters()).isEmpty();
		utility.populatePointSupporters(point(0, 0, 5, 1, 1, 5));
		assertThat(utility.pointSupporters()).containsExactly(reused);
	}

	@Test
	void acceptedPlacementUsesIndexedSupporters() {
		Stack stack = new Stack();
		Placement floor = placement("floor", 0, 0, 0, 2, 2, 1, 0);
		Placement top = placement("top", 0, 0, 1, 2, 2, 1, 32);
		stack.add(floor);
		addFillers(stack, 1, 31);
		stack.add(top);

		TestUtility utility = new TestUtility(stack);
		utility.initialize(33);
		utility.accepted(top);

		assertThat(top.getSupportedArea()).isEqualTo(4L);
		assertThat(top.getSupporters()).hasSize(1);
		assertThat(top.getSupporters().get(0).getPlacement()).isSameAs(floor);
		assertThat(floor.getSupportees()).hasSize(1);
		assertThat(floor.getSupportees().get(0).getPlacement()).isSameAs(top);
	}

	private static Point point(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return new DefaultPoint3D(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static Placement placement(String id, int x, int y, int z, int dx, int dy, int dz, int index) {
		Box box = Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(1).build();
		Placement placement = new Placement(box.getStackValue(0), 0, x, y, z);
		placement.setIndex(index);
		return placement;
	}

	private static void addFillers(Stack stack, int startIndex, int count) {
		for (int i = 0; i < count; i++) {
			stack.add(placement("filler-" + i, 100, 100, 7, 1, 1, 1, startIndex + i));
		}
	}

	private static class TestUtility extends WeightLoadAwarePlacementUtility {

		TestUtility(Stack stack) {
			super(stack);
		}

		List<Placement> pointSupporters() {
			return copy(pointSupporters);
		}

		List<Placement> pointSupportees() {
			return copy(pointSupportees);
		}

		private List<Placement> copy(PlacementList placements) {
			List<Placement> result = new ArrayList<>(placements.size());
			for (int i = 0; i < placements.size(); i++) {
				result.add(placements.get(i));
			}
			return result;
		}
	}
}
