package com.github.skjolber.packing.test.generator.egy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Clazz {

	private static class Weight {
		Category category;
		BigDecimal percent;

		public Weight(Category c, BigDecimal percent) {
			this.category = c;
			this.percent = percent;
		}

		public Category getCategory() {
			return category;
		}

		public BigDecimal getPercent() {
			return percent;
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String name;

		private final List<Weight> items = new ArrayList<>();

		public Builder withCategory(Category c, BigDecimal percent) {
			items.add(new Weight(c, percent));
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Clazz build() {

			BigDecimal sum = new BigDecimal("0");

			for (Weight item : items) {
				sum = sum.add(item.getPercent());
			}

			sum = sum.setScale(1, RoundingMode.HALF_UP);
			if(!sum.equals(new BigDecimal("100.0"))) {
				throw new IllegalStateException("Sum was " + sum + " for " + name);
			}

			if(name == null) {
				throw new IllegalStateException();
			}

			return new Clazz(items, name);
		}

		public static double round(double value, int places) {
			if(places < 0)
				throw new IllegalArgumentException();

			BigDecimal bd = BigDecimal.valueOf(value);
			bd = bd.setScale(places, RoundingMode.HALF_UP);
			return bd.doubleValue();
		}
	}

	private final String name;
	private final List<Weight> items;

	public Clazz(List<Weight> items, String name) {
		this.name = name;
		this.items = items;
	}

	public String getName() {
		return name;
	}

	public CategoryCounts getCounts(int count) {

		int sum = 0;

		int[] categoryCounts = new int[items.size()];

		double[] categoryCountRemainder = new double[items.size()];

		for (int i = 0; i < items.size(); i++) {
			Weight item = items.get(i);

			double countDouble = (count * item.getPercent().doubleValue()) / 100;
			int countInt = (int)(countDouble);

			categoryCounts[i] = countInt;
			categoryCountRemainder[i] = countDouble - countInt;

			sum += countInt;
		}

		while (sum < count) {

			int maxIndex = 0;
			for (int i = 1; i < items.size(); i++) {
				if(categoryCountRemainder[maxIndex] < categoryCountRemainder[i]) {
					maxIndex = i;
				}
			}

			categoryCounts[maxIndex]++;
			categoryCountRemainder[maxIndex] -= count / 100.0;

			sum++;
		}

		CategoryCounts.Builder newBuilder = CategoryCounts.newBuilder().withName(name);

		for (int i = 0; i < items.size(); i++) {
			newBuilder.withCategory(items.get(i).getCategory(), categoryCounts[i]);
		}

		return newBuilder.build();
	}

}
