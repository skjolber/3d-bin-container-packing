package com.github.skjolber.packing.test;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class BouwkampCodeDirectoryTest {

	@Test
	public void testParser() throws IOException {
		BouwkampCodeDirectory parser = BouwkampCodeDirectory.getInstance();
		
		assertThat(parser.getSimpleImperfectSquaredRectangles().size()).isEqualTo(3);
		assertThat(parser.getSimpleImperfectSquaredSquares().size()).isEqualTo(3);
		assertThat(parser.getSimplePerfectSquaredRectangles().size()).isEqualTo(5);
		
		assertThat(parser.codesForCount(13).size()).isEqualTo(3);
	}
}
