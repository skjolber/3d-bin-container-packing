package com.github.skjolber.packing.test.generator.egy;

import java.math.BigDecimal;

import org.apache.commons.math3.random.RandomDataGenerator;

public enum Category {

	K1(new BigDecimal("2.72"), new BigDecimal("12.04")),
	K2(new BigDecimal("12.05"), new BigDecimal("20.23")),
	K3(new BigDecimal("20.28"), new BigDecimal("32.42")),
	K4(new BigDecimal("32.44"), new BigDecimal("54.08")),
	K5(new BigDecimal("54.31"), new BigDecimal("100.21"));

	private final BigDecimal min;
	private final BigDecimal max;

	private Category(BigDecimal min, BigDecimal max) {
		this.min = min;
		this.max = max;
	}

	public BigDecimal getMax() {
		return max;
	}

	public BigDecimal getMin() {
		return min;
	}

	public double getVolume(RandomDataGenerator r) {
		// https://stackoverflow.com/questions/3680637/generate-a-random-double-in-a-range/3680648
		return r.nextUniform(min.doubleValue(), max.doubleValue());
	}
}
