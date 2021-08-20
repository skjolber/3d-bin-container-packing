package com.github.skjolber.packing.test.generator;

import java.util.List;

public interface ItemGenerator<I extends Item> {

	List<I> getItems(int count);

}