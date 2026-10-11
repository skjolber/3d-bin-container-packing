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
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilder;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.cost.FixedContainerCostCalculator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.NoLightersWithPetrolManifestControls;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.packer.plain.heavy.HeavyItemsOnGroundLevelPointControls;

/**
 * A packager session reuses the result of one container for another container (peek) only if their controls factories are the same
 * instance, or carry the same non-null id ({@code getId()}). Lambdas and other factories without an id are only reused as the same instance.
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

	/** A manifest controls factory (not a lambda) which carries an id. */
	private static ManifestControlsBuilderFactory manifestFactory(String id) {
		return new ManifestControlsBuilderFactory() {
			@Override
			public ManifestControlsBuilder createManifestControlsBuilder() {
				return NoLightersWithPetrolManifestControls.newBuilder();
			}

			@Override
			public String getId() {
				return id;
			}
		};
	}

	/** A point controls factory (not a lambda) which carries an id. */
	private static PointControlsBuilderFactory pointFactory(String id) {
		return new PointControlsBuilderFactory() {
			@Override
			public PointControlsBuilder createPointControlsBuilder() {
				return new DefaultPointControlsBuilder();
			}

			@Override
			public String getId() {
				return id;
			}
		};
	}

	private static Consumer<ContainerItem> manifest(ManifestControlsBuilderFactory factory) {
		return containerItem -> containerItem.setManifestControlsBuilderFactory(factory);
	}

	private static Consumer<ContainerItem> point(PointControlsBuilderFactory factory) {
		return containerItem -> containerItem.setPointControlsBuilderFactory(factory);
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
	public void reusesTheResultBetweenContainersWithTheSameInstance(String name) throws Exception {
		// the same instances, without ids
		ManifestControlsBuilderFactory manifest = NoLightersWithPetrolManifestControls.newFactory();
		PointControlsBuilderFactory point = DefaultPointControlsBuilder::new;

		assertThat(manifest.getId()).isNull();
		assertThat(point.getId()).isNull();

		assertThat(peek(name, manifest(manifest), manifest(manifest))).isNotNull();
		assertThat(peek(name, point(point), point(point))).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void reusesTheResultBetweenContainersWithTheSameId(String name) throws Exception {
		// distinct instances, same id
		assertThat(peek(name, manifest(manifestFactory("x")), manifest(manifestFactory("x")))).isNotNull();
		assertThat(peek(name, point(pointFactory("x")), point(pointFactory("x")))).isNotNull();
		assertThat(peek(name, point(new DefaultPointControlsBuilderFactory()), point(new DefaultPointControlsBuilderFactory()))).isNotNull();

		// both kinds of controls
		assertThat(peek(name, manifest(manifestFactory("x")).andThen(point(pointFactory("y"))), manifest(manifestFactory("x")).andThen(point(pointFactory("y"))))).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void doesNotReuseTheResultBetweenContainersWithDifferentIds(String name) throws Exception {
		assertThat(peek(name, manifest(manifestFactory("x")), manifest(manifestFactory("y")))).isNull();
		assertThat(peek(name, point(pointFactory("x")), point(pointFactory("y")))).isNull();

		// one kind of controls is the same, the other differs
		assertThat(peek(name, manifest(manifestFactory("x")).andThen(point(pointFactory("x"))), manifest(manifestFactory("x")).andThen(point(pointFactory("y"))))).isNull();
		assertThat(peek(name, manifest(manifestFactory("x")).andThen(point(pointFactory("x"))), manifest(manifestFactory("y")).andThen(point(pointFactory("x"))))).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void doesNotReuseTheResultBetweenContainersWithDistinctInstancesWithoutId(String name) throws Exception {
		// lambdas have no id, so distinct instances are not reused even if they do the same
		ManifestControlsBuilderFactory manifestA = () -> NoLightersWithPetrolManifestControls.newBuilder();
		ManifestControlsBuilderFactory manifestB = () -> NoLightersWithPetrolManifestControls.newBuilder();
		PointControlsBuilderFactory pointA = DefaultPointControlsBuilder::new;
		PointControlsBuilderFactory pointB = DefaultPointControlsBuilder::new;

		assertThat(peek(name, manifest(manifestA), manifest(manifestB))).isNull();
		assertThat(peek(name, point(pointA), point(pointB))).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void doesNotReuseTheResultBetweenAFactoryWithAnIdAndOneWithout(String name) throws Exception {
		ManifestControlsBuilderFactory withId = manifestFactory("x");
		ManifestControlsBuilderFactory withoutId = () -> NoLightersWithPetrolManifestControls.newBuilder();

		assertThat(peek(name, manifest(withId), manifest(withoutId))).isNull();
		assertThat(peek(name, manifest(withoutId), manifest(withId))).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void reusesTheResultBetweenContainersWithTheSameConfigurationInTheId(String name) throws Exception {
		// the configuration is part of the id, so a different configuration is a different id
		assertThat(HeavyItemsOnGroundLevelPointControls.newFactory(10).getId()).isNotNull();

		assertThat(peek(name, point(HeavyItemsOnGroundLevelPointControls.newFactory(10)), point(HeavyItemsOnGroundLevelPointControls.newFactory(10)))).isNotNull();
		assertThat(peek(name, point(HeavyItemsOnGroundLevelPointControls.newFactory(10)), point(HeavyItemsOnGroundLevelPointControls.newFactory(20)))).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff" })
	public void reusesTheResultBetweenLambdasWrappedWithAnId(String name) throws Exception {
		// of(id, lambda) lets lambdas of different expressions participate in reuse
		assertThat(peek(name, point(PointControlsBuilderFactory.of("x", DefaultPointControlsBuilder::new)),
				point(PointControlsBuilderFactory.of("x", () -> new DefaultPointControlsBuilder())))).isNotNull();
		assertThat(peek(name, point(PointControlsBuilderFactory.of("x", DefaultPointControlsBuilder::new)), point(PointControlsBuilderFactory.of("y", DefaultPointControlsBuilder::new)))).isNull();

		assertThat(peek(name, manifest(ManifestControlsBuilderFactory.of("x", () -> NoLightersWithPetrolManifestControls.newBuilder())),
				manifest(ManifestControlsBuilderFactory.of("x", NoLightersWithPetrolManifestControls::newBuilder)))).isNotNull();
		assertThat(peek(name, manifest(ManifestControlsBuilderFactory.of("x", () -> NoLightersWithPetrolManifestControls.newBuilder())),
				manifest(ManifestControlsBuilderFactory.of("y", () -> NoLightersWithPetrolManifestControls.newBuilder())))).isNull();
	}

	/**
	 * The cost calculators are not compared when a result is reused, as the cost is not a property of the result: it is calculated
	 * from the cost calculator of the container which the result is accepted for, i.e. the container which it was reused for.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "plain", "laff", "fastLaff", "bruteForce", "fastBruteForce" })
	public void aReusedResultIsPricedByTheContainerItWasReusedFor(String name) throws Exception {
		try (AbstractPackager<?> packager = packager(name).get()) {
			ContainerItem a = new ContainerItem(Container.newBuilder().withId("a").withSize(2, 1, 1).withMaxLoadWeight(100).build(), 1, new FixedContainerCostCalculator(7, 2, "a", 0));
			ContainerItem b = new ContainerItem(Container.newBuilder().withId("b").withSize(2, 1, 1).withMaxLoadWeight(100).build(), 1, new FixedContainerCostCalculator(13, 2, "b", 0));

			List<BoxItem> boxItems = List.of(new BoxItem(Box.newBuilder().withId("box").withSize(1, 1, 1).withWeight(1).build(), 1));
			PackagerSession session = packager.createSession(new PackagerInput(boxItems, null, List.of(a, b), 2, Order.NONE), () -> false);

			IntermediatePackagerResult result = session.attempt(0, null, false);
			assertThat(result).isNotNull();

			IntermediatePackagerResult reused = session.peek(1, result);
			assertThat(reused).isNotNull();
			assertThat(reused.getContainerItem().getContainer().getId()).isEqualTo("b");

			Container container = session.accept(reused);
			assertThat(container.getId()).isEqualTo("b");
			assertThat(session.getContainerInventory().getCost()).isEqualTo(13);
		}
	}
}
