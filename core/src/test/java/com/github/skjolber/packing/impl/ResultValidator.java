package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import net.jqwik.api.Tuple;
import org.junit.Assert;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultValidator {
	public static void assertContained(Container container, Container result) {
		final ContainerStackValue containerStackValue = container.getStackValues()[0];
		final List<Integer> dimensions = Stream.of(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz()).sorted().collect(Collectors.toList());
		final Tuple.Tuple3<Integer, Integer, Integer> packaging = result.getStack().getPlacements().stream().map(placement -> Tuple.of(placement.getAbsoluteEndX(), placement.getAbsoluteEndY(), placement.getAbsoluteEndZ()))
			.reduce((d1, d2) -> Tuple.of(Math.max(d1.get1(), d2.get1()), Math.max(d1.get2(), d2.get2()), Math.max(d1.get3(), d2.get3()))).get();
		final List<Integer> packagingDimensions = Stream.of(packaging.get1(), packaging.get2(), packaging.get3()).sorted().collect(Collectors.toList());
		for (int i = 0; i < 3; i++) {
			Assert.assertTrue(String.format("packaging %s (%s)\n should fit into container %s", packagingDimensions, result.getStack().getPlacements(), dimensions), packagingDimensions.get(i) < dimensions.get(i));
		}
	}
}
