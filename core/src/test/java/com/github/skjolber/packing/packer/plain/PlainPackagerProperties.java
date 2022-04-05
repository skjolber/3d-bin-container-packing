package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitQuickcheck.class)
public class PlainPackagerProperties extends AbstractPackagerTest {
	@Property
	public void concatenationLength(String s1, String s2) {
		assertEquals(s1.length() + s2.length(), (s1 + s2).length());
	}
}
