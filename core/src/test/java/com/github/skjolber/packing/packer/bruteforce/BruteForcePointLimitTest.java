package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.collections.api.iterator.IntIterator;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

class BruteForcePointLimitTest {

	@Test
	void selectsTheConfiguredNumberOfBestFittingPointsForEachStep() {
		DefaultPointCalculator3D points = points(
				new DefaultPoint3D(0, 0, 0, 9, 9, 0),
				new DefaultPoint3D(1, 1, 0, 1, 1, 0),
				new DefaultPoint3D(2, 2, 0, 2, 2, 0));
		BoxStackValue stackValue = BoxStackValue.newBuilder().withDimensions(1, 1, 1).build();
		AtomicInteger invocations = new AtomicInteger();

		BruteForcePackager.MostPromisingPointFilter filter = new BruteForcePackager.MostPromisingPointFilter(
				(calculator, value) -> invocations.getAndIncrement() == 0 ? 1 : 2,
				FastBruteForcePackager.DEFAULT_POINT_COMPARATOR, null);

		assertThat(indexes(filter.getPoints(points, stackValue))).containsExactly(2);
		assertThat(indexes(filter.getPoints(points, stackValue))).containsExactly(2, 1);
		assertThat(invocations).hasValue(2);
	}

	@Test
	void fixedPointLimitMustBePositive() {
		assertThatIllegalArgumentException().isThrownBy(() -> new BruteForcePackager.FixedPointLimit(0));
	}

	@Test
	void ordersPointsByClosestVolumeThenArea() {
		DefaultPointCalculator3D points = points(
				new DefaultPoint3D(0, 0, 0, 3, 3, 0),
				new DefaultPoint3D(1, 1, 0, 2, 3, 0),
				new DefaultPoint3D(3, 3, 0, 4, 4, 2));
		BoxStackValue stackValue = BoxStackValue.newBuilder().withDimensions(2, 2, 1).build();

		IntIterator iterator = new BruteForcePackager.ClosestVolumeAndAreaPointFilter().getPoints(points, stackValue);

		assertThat(indexes(iterator)).containsExactly(1, 2, 0);
	}

	@Test
	void appliesThePointLimitPolicyWhilePackaging() {
		AtomicInteger invocations = new AtomicInteger();
		BruteForcePackager.BruteForcePointIteratorFilter filter = new BruteForcePackager.MostPromisingPointFilter(
				(points, stackValue) -> {
					invocations.incrementAndGet();
					return 1;
				});
		BruteForcePackager packager = BruteForcePackager.newBuilder()
				.withPointFilter(filter)
				.build();
		try {
			Box box = Box.newBuilder().withSize(1, 1, 1).withWeight(1).build();
			Container container = Container.newBuilder().withSize(3, 1, 1).withMaxLoadWeight(3).build();
			PackagerResult result = packager.newResultBuilder()
					.withContainerItem(new ContainerItem(container, 1))
					.withBoxItems(new BoxItem(box, 3))
					.build();

			assertThat(result.isSuccess()).isTrue();
			assertThat(invocations).hasValueGreaterThanOrEqualTo(3);
		} finally {
			packager.close();
		}
	}

	private static DefaultPointCalculator3D points(Point... initialPoints) {
		DefaultPointCalculator3D calculator = new DefaultPointCalculator3D(false, initialPoints.length);
		calculator.setSize(10, 10, 10);
		calculator.setPoints(List.of(initialPoints));
		calculator.clear();
		return calculator;
	}

	private static int[] indexes(IntIterator iterator) {
		int[] indexes = new int[8];
		int count = 0;
		while(iterator.hasNext()) {
			indexes[count++] = iterator.next();
		}
		int[] result = new int[count];
		System.arraycopy(indexes, 0, result, 0, count);
		return result;
	}
}
