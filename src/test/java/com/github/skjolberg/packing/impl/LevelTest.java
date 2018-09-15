package com.github.skjolberg.packing.impl;

import com.github.skjolberg.packing.Box;
import com.github.skjolberg.packing.impl.Level;
import com.github.skjolberg.packing.impl.Placement;
import com.github.skjolberg.packing.impl.Space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;


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
