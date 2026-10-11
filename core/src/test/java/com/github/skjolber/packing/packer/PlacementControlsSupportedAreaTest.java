package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControlsBuilderFactory;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * A placement comparator which reads the supported area ({@linkplain PlacementComparator#usesSupportedArea()}) has it calculated for
 * each candidate placement by the placement controls, whether or not support is calculated for the packager
 * ({@code withCalculateSupport(true)}): a candidate on the floor is fully supported, one on a box is supported by the area it rests on. A
 * comparator which does not read it is not paid for it.
 * <p>
 * The box "base" (2x2x2) is placed first, in a container of 6x2x3. The box "plank" (4x2x1) then fits beside it, on the floor and fully
 * supported, or on top of it, at the same height as the base, where it overhangs and is supported by half its area. The comparator ranks by
 * support first, and by height (higher is better) only if the support is the same, so the plank lies beside the base if the comparator sees the
 * support, and on top of it if it does not (the support is then zero for both, and the plank is placed higher).
 */
public class PlacementControlsSupportedAreaTest {

	/** Ranks by supported share of the footprint (higher is better), then by z (higher is better), and records what it sees. */
	private static final class SupportThenHigherZ implements PlacementComparator {

		private final boolean usesSupportedArea;

		/** the candidates which the comparator was given: box, z, and the supported area of the area of the footprint */
		private final Set<String> seen = new TreeSet<>();

		private SupportThenHigherZ(boolean usesSupportedArea) {
			this.usesSupportedArea = usesSupportedArea;
		}

		@Override
		public int compare(Placement a, Placement b) {
			see(a);
			see(b);
			int result = Long.compare(ratio(a), ratio(b));
			if(result != 0) {
				return result;
			}
			return Integer.compare(a.getAbsoluteZ(), b.getAbsoluteZ());
		}

		@Override
		public boolean usesSupportedArea() {
			return usesSupportedArea;
		}

		private static long ratio(Placement placement) {
			return placement.getSupportedArea() * 1000 / placement.getStackValue().getArea();
		}

		private void see(Placement placement) {
			seen.add(placement.getBox().getId() + " z=" + placement.getAbsoluteZ() + " supported " + placement.getSupportedArea() + " of " + placement.getStackValue().getArea());
		}
	}

	private static Box box(String id, int dx, int dy, int dz, int weight) {
		return Box.newBuilder().withId(id).withSize(dx, dy, dz).withWeight(weight).withRotation(Rotation.newBuilder().withBottomAtZeroDegrees().build()).build();
	}

	private static PackagerResult pack(PlainPackager.Builder builder) {
		try (PlainPackager packager = builder.build()) {
			return packager.newResultBuilder()
					.withContainerItems(List.of(new ContainerItem(Container.newBuilder().withId("container").withSize(6, 2, 3).withMaxLoadWeight(100).build(), 1)))
					.withBoxItems(List.of(new BoxItem(box("base", 2, 2, 2, 2), 1), new BoxItem(box("plank", 4, 2, 1, 1), 1)))
					.withInterruptDuration(60_000L)
					.build();
		}
	}

	private static String plank(PackagerResult result) {
		assertThat(result.isSuccess()).isTrue();
		for(Placement placement : result.getContainers().get(0).getStack().getPlacements()) {
			if(placement.getBox().getId().equals("plank")) {
				return placement.getAbsoluteX() + "," + placement.getAbsoluteY() + "," + placement.getAbsoluteZ();
			}
		}
		throw new IllegalStateException("No plank");
	}

	@Test
	public void comparatorWhichReadsTheSupportedAreaSeesItWithoutCalculateSupport() {
		SupportThenHigherZ comparator = new SupportThenHigherZ(true);

		// no withCalculateSupport(true)
		PackagerResult result = pack(PlainPackager.newBuilder().withPlacementControlsBuilderFactory(b -> b.withPlacementComparator(comparator)));
		// <figure>
		//   z   /---------------|                                       z
		//      /               /|                                       2 +---------------+
		//   | /               / |                                         |               |
		//   |/               /  |                                         |               |
		// 2 |---------------|   |-------------------------------|   y     |               |
		//   |               |  /                               /|       1 |     base      +-------------------------------+
		//   |               | /                               / | /       |               |                               |
		//   |               |/                               /  |/        |               |             plank             |
		// 1 |     base      |-------------------------------|   | 2       |               |                               |
		//   |               |                               |  /        0 +---------------+-------------------------------+
		//   |               |             plank             | /           0               2                               6   x
		//   |               |                               |/
		// 0 |---------------|-------------------------------|-- x
		//   0               2                               6
		//
		// y                                                         z
		// 2 +---------------+-------------------------------+       2 +---------------+
		//   |               |                               |         |               |
		//   |               |                               |         |     base      |
		//   |               |                               |         |               |
		//   |     base      |             plank             |       1 +---------------+
		//   |               |                               |         |               |
		//   |               |                               |         |     plank     |
		//   |               |                               |         |               |
		// 0 +---------------+-------------------------------+       0 +---------------+
		//   0               2                               6   x     0               2   y
		// </figure>
		figure(result);

		// the floor is fully supported, the overhang by half its area
		assertThat(comparator.seen).containsExactly("plank z=0 supported 8 of 8", "plank z=2 supported 4 of 8");
		// the fully supported candidate is preferred, so the plank lies beside the base
		assertThat(plank(result)).isEqualTo("2,0,0");
	}

	@Test
	public void comparatorWhichDoesNotReadTheSupportedAreaSeesZero() {
		SupportThenHigherZ comparator = new SupportThenHigherZ(false);

		PackagerResult result = pack(PlainPackager.newBuilder().withPlacementControlsBuilderFactory(b -> b.withPlacementComparator(comparator)));
		// <figure>
		//   z   /-------------------------------|       z                                         y                                         z
		//      /                               /|       3 +-------------------------------+       2 +-------------------------------+       3 +---------------+
		//   | /                               / |         |                               |         |                               |         |               |
		//   |/                               /  |         |             plank             |         |                               |         |     plank     |
		// 3 |-------------------------------|   |         |                               |         |                               |         |               |
		//   |                               |  /        2 +---------------+---------------+         |             plank             |       2 +---------------+
		//   |             plank             | /           |               |                         |                               |         |               |
		//   |                               |/            |               |                         |                               |         |               |
		// 2 |---------------|---------------|       y     |               |                         |                               |         |               |
		//   |               |   |                         |     base      |                       0 +-------------------------------+         |     base      |
		//   |               |   |                 /       |               |                         0               2               4   x     |               |
		//   |               |   |                /        |               |                                                                   |               |
		//   |     base      |   |               / 2       |               |                                                                   |               |
		//   |               |  /               /        0 +---------------+                                                                 0 +---------------+
		//   |               | /               /           0               2               4   x                                               0               2   y
		//   |               |/               /
		// 0 |---------------|------------------ x
		//   0               2               4
		// </figure>
		figure(result);

		// not calculated, as before: the plank is placed higher on the tie
		assertThat(comparator.seen).containsExactly("plank z=0 supported 0 of 8", "plank z=2 supported 0 of 8");
		assertThat(plank(result)).isEqualTo("0,0,2");
	}

	@Test
	public void calculateSupportStillCalculatesTheSupportedAreaForAnyComparator() {
		SupportThenHigherZ comparator = new SupportThenHigherZ(false);

		PackagerResult result = pack(PlainPackager.newBuilder().withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(true).withPlacementComparator(comparator)));
		// <figure>
		//   z   /---------------|                                       z
		//      /               /|                                       2 +---------------+
		//   | /               / |                                         |               |
		//   |/               /  |                                         |               |
		// 2 |---------------|   |-------------------------------|   y     |               |
		//   |               |  /                               /|       1 |     base      +-------------------------------+
		//   |               | /                               / | /       |               |                               |
		//   |               |/                               /  |/        |               |             plank             |
		// 1 |     base      |-------------------------------|   | 2       |               |                               |
		//   |               |                               |  /        0 +---------------+-------------------------------+
		//   |               |             plank             | /           0               2                               6   x
		//   |               |                               |/
		// 0 |---------------|-------------------------------|-- x
		//   0               2                               6
		//
		// y                                                         z
		// 2 +---------------+-------------------------------+       2 +---------------+
		//   |               |                               |         |               |
		//   |               |                               |         |     base      |
		//   |               |                               |         |               |
		//   |     base      |             plank             |       1 +---------------+
		//   |               |                               |         |               |
		//   |               |                               |         |     plank     |
		//   |               |                               |         |               |
		// 0 +---------------+-------------------------------+       0 +---------------+
		//   0               2                               6   x     0               2   y
		// </figure>
		figure(result);

		assertThat(comparator.seen).containsExactly("plank z=0 supported 8 of 8", "plank z=2 supported 4 of 8");
		assertThat(plank(result)).isEqualTo("2,0,0");
	}

	// the controls which the factories create: support is calculated for a comparator which reads it, and only then

	private static PlacementControls controls(PlacementControlsBuilderFactory factory) {
		return factory.createPlacementControlsBuilder().build();
	}

	@Test
	public void defaultRankingDoesNotCalculateSupport() {
		PlacementControlsBuilderFactoryBuilder factory = new PlacementControlsBuilderFactoryBuilder();
		assertThat(controls(factory.build())).isInstanceOf(ComparatorPlacementControls.class);
		assertThat(controls(factory.buildFirst())).isInstanceOf(ComparatorPlacementControls.class);

		PlacementControlsBuilderFactoryBuilder ranking = new PlacementControlsBuilderFactoryBuilder().withPlacementComparators(r -> r.lowerZIsBetter().higherVolumeIsBetter());
		assertThat(controls(ranking.build())).isInstanceOf(ComparatorPlacementControls.class);
	}

	@Test
	public void rankingBySupportCalculatesSupportWithoutTheOption() {
		PlacementControlsBuilderFactoryBuilder ranking = new PlacementControlsBuilderFactoryBuilder().withPlacementComparators(r -> r.higherSupportIsBetter().lowerZIsBetter());
		assertThat(controls(ranking.build())).isInstanceOf(SupportPlacementControls.class);
		assertThat(controls(ranking.buildFirst())).isInstanceOf(SupportPlacementControls.class);

		assertThat(controls(new PlacementControlsBuilderFactoryBuilder().withCalculateSupport(true).build())).isInstanceOf(SupportPlacementControls.class);
	}

	@Test
	public void fixedComparatorCalculatesSupportOnlyIfItReadsIt() {
		assertThat(controls(new PlacementControlsBuilderFactoryBuilder().withPlacementComparator(new SupportThenHigherZ(true)).build())).isInstanceOf(SupportPlacementControls.class);
		assertThat(controls(new PlacementControlsBuilderFactoryBuilder().withPlacementComparator(new SupportThenHigherZ(false)).build())).isInstanceOf(ComparatorPlacementControls.class);
	}

	@Test
	public void comparatorControlsBuilderCalculatesSupportOnlyIfTheComparatorReadsIt() {
		VolumeThenWeightBoxItemComparator boxItemComparator = new VolumeThenWeightBoxItemComparator();
		assertThat(controls(new ComparatorPlacementControlsBuilderFactory(new SupportThenHigherZ(true), boxItemComparator))).isInstanceOf(SupportPlacementControls.class);
		assertThat(controls(new ComparatorPlacementControlsBuilderFactory(new SupportThenHigherZ(false), boxItemComparator))).isInstanceOf(ComparatorPlacementControls.class);
	}

	@Test
	public void lambdaComparatorDoesNotDeclareTheSupportedAreaAndGetsTheCheapControls() {
		PlacementComparator lambda = (a, b) -> 0;
		assertThat(lambda.usesSupportedArea()).isFalse();
		assertThat(controls(new PlacementControlsBuilderFactoryBuilder().withPlacementComparator(lambda).build())).isInstanceOf(ComparatorPlacementControls.class);
	}
}
