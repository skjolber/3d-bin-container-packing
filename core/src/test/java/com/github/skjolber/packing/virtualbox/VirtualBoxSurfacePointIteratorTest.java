package com.github.skjolber.packing.virtualbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutTest.*;
import static com.github.skjolber.packing.virtualbox.VirtualBoxLayoutPreparationTest.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.MarkResetPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBounds;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsLoadSupport;

class VirtualBoxSurfacePointIteratorTest {
	/*
	 * Geometric pruning leaves just the origin above W:
	 *
	 *       o...............      o = normal point, load rejected
	 *       :       +-------+
	 *       :       |   T   |      Additional candidate over S succeeds.
	 *       +-------+-------+
	 *       |   W   |   S   |      One flat surface, different load capacities.
	 *       +-------+-------+
	 *
	 * Both X and Y versions must retain the strong child's useful origin.
	 */
	@Test
	void reachesStrongChildAlongXWithoutDisablingGeometricPruning() throws PackagerInterruptedException {
		checkStrongChild(false);
	}

	@Test
	void reachesStrongChildAlongYWithoutDisablingGeometricPruning() throws PackagerInterruptedException {
		checkStrongChild(true);
	}

	protected void checkStrongChild(boolean alongY) throws PackagerInterruptedException {
		BoxItem weak = new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.withMaxLoadWeight(0)
				.build());
		BoxItem strong = new BoxItem(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.withMaxLoadWeight(5)
				.build());
		int x = alongY ? 0 : 1;
		int y = alongY ? 1 : 0;
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(x + 1, y + 1, 1), List.of(placement(weak, 0, 0, 0), placement(strong, x, y, 0)));
		List<Placement> physical = layout.createPlacements(true);
		DefaultPointCalculator3D calculator = calculator(x + 1, y + 1, 2);
		calculator.add(0, physical, 1, 1);
		assertThat(calculator.size()).isEqualTo(1);
		assertThat(calculator.findPoint(x, y, 1)).isEqualTo(-1);
		BoxStackValue target = item(1, 1, 1, 1).getBox().getStackValue(0);
		VirtualBoxBoundsLoadSupport loads = new VirtualBoxBoundsLoadSupport(() -> false);
		assertThat(loads.isValidLayout(new Placement[] {physical.get(0), physical.get(1), new Placement(target, calculator.get(0), false)})).isFalse();
		VirtualBoxSurfacePoints surfaces = new VirtualBoxSurfacePoints();
		surfaces.push(layout, physical);
		var iterator = surfaces.newIterator();
		iterator.reset(calculator, target);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.hasNext()).isTrue();
		SimplePoint3D candidate = iterator.next();
		assertThat(candidate.getMinX()).isEqualTo(x);
		assertThat(candidate.getMinY()).isEqualTo(y);
		assertThat(candidate.getMinZ()).isEqualTo(1);
		Placement accepted = new Placement(target, candidate, true);
		int source = iterator.getSourcePointIndex();
		assertThat(calculator.get(source).fits3D(accepted)).isTrue();
		assertThat(loads.isValidLayout(new Placement[] {physical.get(0), physical.get(1), accepted})).isTrue();
		assertThat(iterator.hasNext()).isFalse();
		assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
		assertThat(calculator.size()).isEqualTo(1);
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(physical);
		calculator.add(source, List.of(accepted), 1, 1);
		assertThat(calculator.getPlacements()).hasSize(3);
	}

	/*
	 *       +-----------+       target spans both physical faces
	 *       |     T     |
	 *       +---+-------+---+
	 *       |   A   |   A   |   also try T shifted one unit right
	 *       +-------+-------+
	 *
	 * Different face alignments proposing the same origin must be deduplicated.
	 */
	@Test
	void permitsSpanningFacesAndDeduplicatesOrigins() {
		BoxItem base = item(2, 1, 1, 2);
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(4, 1, 1), List.of(placement(base, 0, 0, 0), placement(base, 2, 0, 0)));
		DefaultPointCalculator3D calculator = calculator(4, 1, 2);
		calculator.add(0, layout.getPlacements(), 1, 1);
		VirtualBoxSurfacePoints surfaces = new VirtualBoxSurfacePoints();
		surfaces.push(layout, layout.getPlacements());
		var iterator = surfaces.newIterator();
		BoxStackValue target = item(3, 1, 1, 1).getBox().getStackValue(0);
		iterator.reset(calculator, target);
		SimplePoint3D candidate = iterator.next();
		// A single two-unit face must not claim all three units of support just
		// because the candidate starts to its left. Full support is a graph query.
		assertThat(candidate.calculateXYSupport(3, 1)).isLessThanOrEqualTo(2);
		iterator.reset(calculator, target);
		assertThat(origins(iterator, calculator, target)).containsExactly("1,0,1");
		iterator.reset(calculator, target);
		assertThat(origins(iterator, calculator, target)).containsExactly("1,0,1");
	}

	/*
	 *       +---+           +---+
	 *       | L | free area | R |    L and R hide the original face corners.
	 *       +---+-----------+---+
	 *       |         A         |    Clamp alignments to the remaining free area.
	 *       +-------------------+
	 */
	@Test
	void coveredFacesOnlyYieldCandidatesInsideCurrentFreeSpace() {
		Placement base = placement(item(5, 1, 1, 1), 0, 0, 0);
		Placement left = placement(item(1, 1, 1, 1), 0, 0, 1);
		Placement right = placement(item(1, 1, 1, 1), 4, 0, 1);
		DefaultPointCalculator3D calculator = calculator(5, 1, 2);
		calculator.add(0, base);
		calculator.add(calculator.findPoint(0, 0, 1), left);
		assertThat(calculator.addObstacle(right)).isTrue();
		VirtualBoxSurfacePoints surfaces = new VirtualBoxSurfacePoints();
		surfaces.push(base);
		surfaces.push(left);
		surfaces.push(right);
		var iterator = surfaces.newIterator();
		BoxStackValue target = item(1, 1, 1, 1).getBox().getStackValue(0);
		iterator.reset(calculator, target);
		assertThat(origins(iterator, calculator, target)).containsExactly("3,0,1");
	}

	/*
	 *       +-------+-------+
	 *       |   A   |   A   |   z = 2, inserted first
	 *       +-------+-------+
	 *       :   T   :   T   :   z = 1, useful supportee-contact candidates
	 *       :.......:.......:
	 *       :               :   z = 0, normal geometric origin
	 *       +---------------+
	 */
	@Test
	void includesBottomFacesAndRollsBackWholeFrames() {
		BoxItem item = item(1, 1, 1, 2);
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(2, 1, 1), List.of(placement(item, 0, 0, 0), placement(item, 1, 0, 0)));
		List<Placement> translated = layout.createPlacements(false);
		layout.translate(0, 0, 2, translated);
		MarkResetPointCalculator3D calculator = new MarkResetPointCalculator3D(true, 8);
		calculator.clearToSize(2, 1, 3);
		calculator.mark();
		calculator.add(0, translated, 1, 1);
		VirtualBoxSurfacePoints surfaces = new VirtualBoxSurfacePoints();
		surfaces.push(layout, translated);
		assertThat(surfaces.getStackSize()).isEqualTo(1);
		assertThat(surfaces.size()).isEqualTo(4);
		var iterator = surfaces.newIterator();
		BoxStackValue target = item.getBox().getStackValue(0);
		iterator.reset(calculator, target);
		assertThat(origins(iterator, calculator, target)).containsExactly("0,0,1", "1,0,1");
		// Another worker never sees these accepted faces or mutable placements.
		var other = new VirtualBoxSurfacePoints().newIterator();
		other.reset(calculator, target);
		assertThat(other.hasNext()).isFalse();
		surfaces.push(placement(item, 0, 0, 0));
		surfaces.setStackSize(1);
		assertThat(surfaces.size()).isEqualTo(4);
		surfaces.pop();
		calculator.reset();
		iterator.reset(calculator, target);
		assertThat(iterator.hasNext()).isFalse();
		assertThat(surfaces.size()).isZero();
		assertThat(calculator.getPlacements()).isEmpty();
		surfaces.push(layout, translated);
		surfaces.clear();
		assertThat(surfaces.getStackSize()).isZero();
		assertThatThrownBy(surfaces::pop).isInstanceOf(IllegalArgumentException.class);
	}

	/* Internal horizontal contacts are excluded, regardless of how many layers the layout has. */
	@Test
	void onlyRegistersExposedFacesOfTallLayout() {
		BoxItem item = item(1, 1, 1, 3);
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(1, 1, 3), List.of(placement(item, 0, 0, 0), placement(item, 0, 0, 1), placement(item, 0, 0, 2)));
		VirtualBoxSurfacePoints surfaces = new VirtualBoxSurfacePoints();
		surfaces.push(layout, layout.getPlacements());
		assertThat(surfaces.size()).isEqualTo(2);
		assertThat(surfaces.faces[0]).isSameAs(layout.getPlacements().get(2));
		assertThat(surfaces.faces[1]).isSameAs(layout.getPlacements().get(0));
	}

	protected static List<String> origins(VirtualBoxSurfacePointIterator iterator, DefaultPointCalculator3D calculator, BoxStackValue target) {
		List<String> result = new ArrayList<>();
		while(iterator.hasNext()) {
			SimplePoint3D point = iterator.next();
			Placement placement = new Placement(target, point, false);
			assertThat(calculator.get(iterator.getSourcePointIndex()).fits3D(placement)).isTrue();
			for(Placement existing : calculator.getPlacements()) {
				assertThat(placement.intersects(existing)).isFalse();
			}
			result.add(point.getMinX() + "," + point.getMinY() + "," + point.getMinZ());
		}
		assertThat(result).doesNotHaveDuplicates();
		return result;
	}

	protected static DefaultPointCalculator3D calculator(int x, int y, int z) {
		DefaultPointCalculator3D calculator = new DefaultPointCalculator3D(true, 16);
		calculator.clearToSize(x, y, z);
		return calculator;
	}

	@Test
	void largeCoordinateKeysDoNotOverflowOrDeduplicateDifferentPositions() {
		BoxItem base = item(1_000_000_000, 1, 1, 2);
		var layout = new VirtualBoxLayout(new VirtualBoxBounds(2_000_000_000, 1, 1),
				List.of(placement(base, 0, 0, 0), placement(base, 1_000_000_000, 0, 0)));
		DefaultPointCalculator3D calculator = calculator(2_000_000_000, 1, 2);
		calculator.add(0, layout.getPlacements(), 1, 1);
		VirtualBoxSurfacePoints surfaces = new VirtualBoxSurfacePoints();
		surfaces.push(layout, layout.getPlacements());
		var iterator = surfaces.newIterator();
		BoxStackValue target = item(1, 1, 1, 1).getBox().getStackValue(0);
		iterator.reset(calculator, target);
		assertThat(origins(iterator, calculator, target)).containsExactly("999999999,0,1", "1000000000,0,1", "1999999999,0,1");
	}
}
