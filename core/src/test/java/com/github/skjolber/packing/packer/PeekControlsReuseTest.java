package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.NoLightersWithPetrolManifestControls;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * A packager session reuses the result of one container for another container (peek) only if their controls are equal.
 * A result which was packed without controls must not be reused for a container with controls, as its controls would be ignored.
 *
 * <pre>
 *   a (no controls)   [box][ ]      result packed for a
 *   b (controls)      [   ][ ]      not served the result of a
 * </pre>
 */
public class PeekControlsReuseTest {

	private static Supplier<AbstractPackager<?>> packager(String name) {
		switch (name) {
			case "plain": return () -> PlainPackager.newBuilder().build();
			case "laff": return () -> LargestAreaFitFirstPackager.newBuilder().build();
			case "fastLaff": return () -> FastLargestAreaFitFirstPackager.newBuilder().build();
			case "bruteForce": return () -> BruteForcePackager.newBuilder().build();
			case "fastBruteForce": return () -> FastBruteForcePackager.newBuilder().build();
			default: throw new IllegalArgumentException(name);
		}
	}

	/**
	 * Pack the box in container a, then peek whether the result can be used for container b.
	 *
	 * @param configureA configures the controls of container a
	 * @param configureB configures the controls of container b
	 * @return the result for container b, or null if the result of a was not reused
	 */
	private static IntermediatePackagerResult peek(String name, Consumer<ContainerItem> configureA, Consumer<ContainerItem> configureB) throws Exception {
		try (AbstractPackager<?> packager = packager(name).get()) {
			ContainerItem a = new ContainerItem(Container.newBuilder().withId("a").withSize(2, 1, 1).withMaxLoadWeight(100).build(), 1);
			ContainerItem b = new ContainerItem(Container.newBuilder().withId("b").withSize(2, 1, 1).withMaxLoadWeight(100).build(), 1);
			configureA.accept(a);
			configureB.accept(b);

			List<BoxItem> boxItems = List.of(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1));
			PackagerSession session = packager.createSession(new PackagerInput(boxItems, null, List.of(a, b), 2, Order.NONE), () -> false);

			IntermediatePackagerResult result = session.attempt(0, null, false);
			assertThat(result).isNotNull();
			assertThat(result.isEmpty()).isFalse();

			return session.peek(1, result);
		}
	}

	private static void noControls(ContainerItem containerItem) {
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff", "bruteForce", "fastBruteForce" })
	public void reusesTheResultBetweenContainersWithoutControls(String name) throws Exception {
		IntermediatePackagerResult reused = peek(name, PeekControlsReuseTest::noControls, PeekControlsReuseTest::noControls);

		assertThat(reused).isNotNull();
		assertThat(reused.getContainerItem().getContainer().getId()).isEqualTo("b");
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void doesNotReuseAResultPackedWithoutManifestControlsForAContainerWithManifestControls(String name) throws Exception {
		ManifestControlsBuilderFactory factory = NoLightersWithPetrolManifestControls.newFactory();

		IntermediatePackagerResult reused = peek(name, PeekControlsReuseTest::noControls, b -> b.setManifestControlsBuilderFactory(factory));

		assertThat(reused).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void doesNotReuseAResultPackedWithoutPointControlsForAContainerWithPointControls(String name) throws Exception {
		PointControlsBuilderFactory factory = new DefaultPointControlsBuilderFactory();

		IntermediatePackagerResult reused = peek(name, PeekControlsReuseTest::noControls, b -> b.setPointControlsBuilderFactory(factory));

		assertThat(reused).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void doesNotReuseAResultPackedWithControlsForAContainerWithoutControls(String name) throws Exception {
		ManifestControlsBuilderFactory factory = NoLightersWithPetrolManifestControls.newFactory();

		assertThat(peek(name, a -> a.setManifestControlsBuilderFactory(factory), PeekControlsReuseTest::noControls)).isNull();
		assertThat(peek(name, a -> a.setPointControlsBuilderFactory(new DefaultPointControlsBuilderFactory()), PeekControlsReuseTest::noControls)).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void reusesTheResultBetweenContainersWithEqualControls(String name) throws Exception {
		// the same instance
		ManifestControlsBuilderFactory factory = NoLightersWithPetrolManifestControls.newFactory();
		assertThat(peek(name, a -> a.setManifestControlsBuilderFactory(factory), b -> b.setManifestControlsBuilderFactory(factory))).isNotNull();

		// equal instances
		assertThat(peek(name, a -> a.setPointControlsBuilderFactory(new DefaultPointControlsBuilderFactory()), b -> b.setPointControlsBuilderFactory(new DefaultPointControlsBuilderFactory()))).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void doesNotReuseTheResultBetweenContainersWithDifferentControls(String name) throws Exception {
		// lambdas of different expressions never compare equal
		ManifestControlsBuilderFactory factoryA = () -> NoLightersWithPetrolManifestControls.newBuilder();
		ManifestControlsBuilderFactory factoryB = () -> NoLightersWithPetrolManifestControls.newBuilder();

		assertThat(peek(name, a -> a.setManifestControlsBuilderFactory(factoryA), b -> b.setManifestControlsBuilderFactory(factoryB))).isNull();
	}
}
