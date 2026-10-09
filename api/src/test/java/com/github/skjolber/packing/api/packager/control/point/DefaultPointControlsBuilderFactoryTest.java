package com.github.skjolber.packing.api.packager.control.point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Packagers reuse the result of a container for another container only if their point controls factories are equal: the
 * default factory is stateless, so its instances are equal.
 */
class DefaultPointControlsBuilderFactoryTest {

	@Test
	void instancesAreEqual() {
		DefaultPointControlsBuilderFactory a = new DefaultPointControlsBuilderFactory();
		DefaultPointControlsBuilderFactory b = new DefaultPointControlsBuilderFactory();

		assertEquals(a, b);
		assertEquals(b, a);
		assertEquals(a, a);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void differsFromOtherFactoriesAndSubclasses() {
		DefaultPointControlsBuilderFactory factory = new DefaultPointControlsBuilderFactory();
		PointControlsBuilderFactory lambda = DefaultPointControlsBuilder::new;
		PointControlsBuilderFactory subclass = new DefaultPointControlsBuilderFactory() {
		};

		assertNotEquals(factory, lambda);
		assertNotEquals(lambda, factory);
		assertNotEquals(factory, subclass);
		assertNotEquals(subclass, factory);
		assertNotEquals(factory, null);
		assertNotEquals(factory, "factory");
	}
}
