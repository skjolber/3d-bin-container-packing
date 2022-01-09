package com.github.skjolber.packing.test;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class BouwkampCodeParserTest {

	@Test
	public void testParser() throws IOException {
		BouwkampCodeParser parser = new BouwkampCodeParser();
		
		List<BouwkampCode> codes = parser.parse(getClass().getResourceAsStream("/simplePerfectSquaredRectangles/o12spsr.bkp"), StandardCharsets.UTF_8);

		assertThat(codes.size()).isEqualTo(67);
		
		for(BouwkampCode code : codes) {
			assertThat(code.getLines().stream().flatMap( f -> f.getSquares().stream()).collect(Collectors.toList()).size()).isEqualTo(12);
		}
	}
}
