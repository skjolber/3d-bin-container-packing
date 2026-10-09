package com.github.skjolber.packing.api.packager.control.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Packagers reuse the result of a container for another container only if their manifest controls factories carry the same id
 * (or are the same instance); a lambda has no id unless wrapped with {@link ManifestControlsBuilderFactory#of(String, ManifestControlsBuilderFactory)}.
 */
class ManifestControlsBuilderFactoryTest {

	@Test
	void lambdaHasNoId() {
		ManifestControlsBuilderFactory lambda = () -> null;

		assertNull(lambda.getId());
	}

	@Test
	void ofCarriesTheIdAndDelegates() {
		AtomicInteger calls = new AtomicInteger();
		ManifestControlsBuilderFactory factory = ManifestControlsBuilderFactory.of("id", () -> {
			calls.incrementAndGet();
			return null;
		});

		assertEquals("id", factory.getId());
		assertNull(factory.createManifestControlsBuilder());
		assertEquals(1, calls.get());
	}

	@Test
	void ofRejectsNullArguments() {
		assertThrows(IllegalArgumentException.class, () -> ManifestControlsBuilderFactory.of(null, () -> null));
		assertThrows(IllegalArgumentException.class, () -> ManifestControlsBuilderFactory.of("id", null));
	}
}
