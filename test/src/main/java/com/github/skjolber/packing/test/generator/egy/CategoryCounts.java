package com.github.skjolber.packing.test.generator.egy;

import java.util.HashMap;
import java.util.Map;

public class CategoryCounts {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String name;

		private final Map<Category, Integer> counts = new HashMap<>();

		public Builder withCategory(Category c, int count) {
			counts.put(c, count);
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public CategoryCounts build() {
			if(name == null) {
				throw new IllegalStateException();
			}

			return new CategoryCounts(name, counts);
		}
	}

	private final String name;
	private final Map<Category, Integer> counts;

	public CategoryCounts(String name, Map<Category, Integer> counts) {
		super();
		this.name = name;
		this.counts = counts;
	}

	public int getCount(Category category) {
		Integer integer = counts.get(category);
		if(integer != null) {
			return integer;
		}
		return 0;
	}

	public String getName() {
		return name;
	}

}
