package com.github.skjolber.packing;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Dimension;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.Space;

class ContainerTest {

	@Test
	void testEquals() {
		assertEquals(new Container(1, 1, 1, 0), new Container(new Dimension(1, 1, 1), 1));
	}

    @Test
		void testGetUsedSpaceWhenEmpty() {
        assertEquals(new Dimension(0, 0, 0), new Container(0, 0, 0, 0).getUsedSpace());
    }

    @Test
		void testGetUsedSpaceWhenOneBox() {
        final Container container = new Container(10, 10, 10, 0);
        container.addLevel();
        container.add(new Placement(new Space(), new Box(2, 3, 4, 0)));
        assertEquals(new Dimension(2, 3, 4), container.getUsedSpace());
    }

    @Test
		void testGetUsedSpaceWhenTwoBoxesSameLevel() {
        final Container container = new Container(10, 10, 10, 0);
        container.addLevel();
        container.add(new Placement(new Space(10, 10, 10, 0, 0, 0), new Box(2, 3, 7, 0)));
        container.add(new Placement(new Space(10, 10, 10, 2, 3, 0), new Box(1, 2, 7, 0)));
        assertEquals(new Dimension(3, 5, 7), container.getUsedSpace());
    }

    @Test
		void testGetUsedSpaceWhenTwoBoxesTwoLevels() {
        final Container container = new Container(10, 10, 10, 0);
        container.addLevel();
        container.add(new Placement(new Space(10, 10, 4, 0, 0, 0), new Box(2, 3, 4, 0)));
        container.addLevel();
        container.add(new Placement(new Space(10, 10, 6, 0, 0, 4), new Box(1, 2, 2, 0)));
        assertEquals(new Dimension(2, 3, 6), container.getUsedSpace());
    }


}
