package com.github.skjolber.packing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.old.Box;
import com.github.skjolber.packing.old.Level;
import com.github.skjolber.packing.old.Placement;
import com.github.skjolber.packing.old.Space;


class LevelTest {

	@Test
	void testValidator() {
		Level level = new Level();
		Placement a = new Placement(new Space(1, 1, 1, 0, 0, 0), new Box(1, 1, 1, 0));
		level.add(a);

		Placement b = new Placement(new Space(1, 1, 1, 1, 0, 0), new Box(1, 1, 1, 0));
		level.add(b);

		level.validate();
		
	}
	
	@Test
	void testValidatorThrowsException() {
		Level level = new Level();
		Placement a = new Placement(new Space(1, 1, 1, 0, 0, 0), new Box(2, 1, 1, 0));
		level.add(a);

		Placement b = new Placement(new Space(1, 1, 1, 1, 0, 0), new Box(1, 1, 1, 0));
		level.add(b);

		assertThrows(IllegalArgumentException.class, () -> {
			level.validate();
        });
	}
}
