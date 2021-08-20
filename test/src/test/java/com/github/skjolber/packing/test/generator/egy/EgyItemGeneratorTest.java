package com.github.skjolber.packing.test.generator.egy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.generator.Item;
import com.github.skjolber.packing.test.generator.ItemGenerator;

public class EgyItemGeneratorTest {

	@Test
	public void testGenerator() {
		
		ItemGenerator generator = new EgyItemGenerator(EgyItemGenerator.CLASS_1);
		
		List<Item> items = generator.getItems(250);
		for(Item item : items) {
			System.out.println(item);
		}
	}
}
