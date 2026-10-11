package com.github.skjolber.packing.api.packager.control.point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Packagers reuse the result of a container for another container only if their point controls factories carry the same id
 * (or are the same instance): the default factory is stateless, so all its instances carry the same id.
 */
class DefaultPointControlsBuilderFactoryTest {

	@Test
	void instancesCarryTheSameId() {
		DefaultPointControlsBuilderFactory a = new DefaultPointControlsBuilderFactory();
		DefaultPointControlsBuilderFactory b = new DefaultPointControlsBuilderFactory();

		assertNotNull(a.getId());
		assertEquals(a.getId(), b.getId());
	}

	@Test
	void differsFromFactoriesWithoutOrWithAnotherId() {
		DefaultPointControlsBuilderFactory factory = new DefaultPointControlsBuilderFactory();
		PointControlsBuilderFactory lambda = DefaultPointControlsBuilder::new;
		PointControlsBuilderFactory other = PointControlsBuilderFactory.of("other", DefaultPointControlsBuilder::new);

		assertNull(lambda.getId());
		assertNotEquals(factory.getId(), other.getId());
	}

	@Test
	void ofCarriesTheIdAndDelegates() {
		PointControlsBuilder builder = new DefaultPointControlsBuilder();
		PointControlsBuilderFactory factory = PointControlsBuilderFactory.of("id", () -> builder);

		assertEquals("id", factory.getId());
		assertEquals(builder, factory.createPointControlsBuilder());
	}

	@Test
	void ofRejectsNullArguments() {
		assertThrows(IllegalArgumentException.class, () -> PointControlsBuilderFactory.of(null, DefaultPointControlsBuilder::new));
		assertThrows(IllegalArgumentException.class, () -> PointControlsBuilderFactory.of("id", null));
	}
}
