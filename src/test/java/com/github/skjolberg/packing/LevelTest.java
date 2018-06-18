package com.github.skjolberg.packing;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LevelTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void testValidator() {
		Level level = new Level();
		Placement a = new Placement(new Space(1, 1, 1, 0, 0, 0), new Box(1, 1, 1, 0));
		level.add(a);

		Placement b = new Placement(new Space(1, 1, 1, 1, 0, 0), new Box(1, 1, 1, 0));
		level.add(b);

		level.validate();
	}
	
	@Test
	public void testValidatorThrowsException() {
		Level level = new Level();
		Placement a = new Placement(new Space(1, 1, 1, 0, 0, 0), new Box(2, 1, 1, 0));
		level.add(a);

		Placement b = new Placement(new Space(1, 1, 1, 1, 0, 0), new Box(1, 1, 1, 0));
		level.add(b);

		exception.expect(IllegalArgumentException.class);
		level.validate();
	}
}
