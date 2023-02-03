package com.github.skjolber.packing.test.generator.egy;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.generator.Item;
import com.github.skjolber.packing.test.generator.ItemGenerator;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class EgyItemGeneratorTest {

	@Test
	public void testGenerator() {
		ItemGenerator generator = new EgyItemGenerator(EgyItemGenerator.CLASS_1);

		List<Item> items = generator.getItems(250);

		int count = 0;
		for (Item item : items) {
			count += item.getCount();
		}
		assertEquals(count, 250);
	}
}
