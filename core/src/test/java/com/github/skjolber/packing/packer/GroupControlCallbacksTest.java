package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.control.manifest.AbstractManifestControlsBuilder;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.point.AbstractPointControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointSource;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * The packagers notify manifest and point controls of each box item group they attempt, in the same way:
 *
 * <pre>
 *   containers 2 x 1 x 1:  [a][b]   [c]
 * </pre>
 */
public class GroupControlCallbacksTest {

	private static class ManifestRecorder implements ManifestControls {

		private final List<String> events;

		ManifestRecorder(List<String> events) {
			this.events = events;
		}

		@Override
		public void attempt(BoxItemGroup group, int offset, int length) {
			events.add("manifest attempt " + group.getId());
		}

		@Override
		public void attemptSuccess(BoxItemGroup group) {
			events.add("manifest success " + group.getId());
		}

		@Override
		public void attemptFailure(BoxItemGroup group) {
			events.add("manifest failure " + group.getId());
		}
	}

	private static class PointRecorder extends DefaultPointControls {

		private final List<String> events;

		PointRecorder(PointSource points, List<String> events) {
			super(points);
			this.events = events;
		}

		@Override
		public void attempt(BoxItemGroup group, int offset, int length) {
			events.add("point attempt " + group.getId());
		}

		@Override
		public void attemptSuccess(BoxItemGroup group) {
			events.add("point success " + group.getId());
		}

		@Override
		public void attemptFailure(BoxItemGroup group) {
			events.add("point failure " + group.getId());
		}
	}

	private static class ManifestRecorderBuilder extends AbstractManifestControlsBuilder<ManifestRecorderBuilder> {

		private final List<String> events;

		ManifestRecorderBuilder(List<String> events) {
			this.events = events;
		}

		@Override
		public ManifestControls build() {
			return new ManifestRecorder(events);
		}
	}

	private static class PointRecorderBuilder extends AbstractPointControlsBuilder<PointRecorderBuilder> {

		private final List<String> events;

		PointRecorderBuilder(List<String> events) {
			this.events = events;
		}

		@Override
		public PointControls build() {
			return new PointRecorder(points, events);
		}
	}

	private static List<String> pack(AbstractPackager<?> packager) {
		List<String> events = new ArrayList<>();
		List<BoxItemGroup> groups = new ArrayList<>();
		for (String id : List.of("a", "b", "c")) {
			groups.add(new BoxItemGroup(id, List.of(new BoxItem(Box.newBuilder().withId(id).withSize(1, 1, 1).withWeight(1).build(), 1))));
		}
		Container container = Container.newBuilder().withId("c").withSize(2, 1, 1).withMaxLoadWeight(100).build();

		packager.newResultBuilder()
				.withContainerItem(b -> b
						.withContainerItem(new ContainerItem(container, 2))
						.withManifestControlsBuilderFactory(() -> new ManifestRecorderBuilder(events))
						.withPointControlsBuilderFactory(() -> new PointRecorderBuilder(events)))
				.withBoxItemGroups(groups)
				.withMaxContainerCount(2)
				.build();
		return events;
	}

	@ParameterizedTest
	@ValueSource(strings = { "laff", "fastLaff" })
	public void notifiesControlsOfGroupsLikePlain(String name) {
		Supplier<AbstractPackager<?>> supplier = name.equals("laff") ? () -> LargestAreaFitFirstPackager.newBuilder().build() : () -> FastLargestAreaFitFirstPackager.newBuilder().build();

		List<String> expected;
		try (PlainPackager plain = PlainPackager.newBuilder().build()) {
			expected = pack(plain);
		}
		assertThat(expected).contains("manifest attempt a", "manifest success a", "point success a");

		try (AbstractPackager<?> packager = supplier.get()) {
			assertThat(pack(packager)).isEqualTo(expected);
		}
	}
}
