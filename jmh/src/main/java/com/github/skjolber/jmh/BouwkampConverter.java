package com.github.skjolber.jmh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.test.BouwkampCode;
import com.github.skjolber.packing.test.BouwkampCodeLine;

public class BouwkampConverter {

	public static Container getContainer3D(BouwkampCode bouwkampCode) {
		return Container.newBuilder().withName("Container").withEmptyWeight(1).withRotate(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1, bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1, bouwkampCode.getWidth() * bouwkampCode.getDepth(), null).withStack(new DefaultStack()).build();
	}
	
	public static List<StackableItem> getStackableItems3D(BouwkampCode bouwkampCode) {
		// map similar items to the same stack item - this actually helps a lot
		List<Integer> squares = new ArrayList<>(); 
		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			squares.addAll(bouwkampCodeLine.getSquares());
		}

		Map<Integer, Integer> frequencyMap = new HashMap<>();
		squares.forEach(word ->
        	frequencyMap.merge(word, 1, (v, newV) -> v + newV)
		);
		
		List<StackableItem> products = new ArrayList<>();
		for (Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
			int square = entry.getKey();
			int count = entry.getValue();
			products.add(new StackableItem(Box.newBuilder().withName(Integer.toString(square)).withRotateXYZ(square, square, 1).withWeight(1).build(), count));
		}

		Collections.shuffle(products);
		
		return products;
	}

}