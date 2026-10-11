package com.github.skjolber.packing.api.interrupt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PackagerInterruptedExceptionTest {

	@Test
	void carriesNoStackTraceOrSuppressedExceptions() {
		PackagerInterruptedException exception = new PackagerInterruptedException();

		assertThat(exception.getStackTrace()).isEmpty();
		assertThat(exception.getMessage()).isNull();
		assertThat(exception.getCause()).isNull();

		exception.addSuppressed(new RuntimeException());
		assertThat(exception.getSuppressed()).isEmpty();
	}
}
