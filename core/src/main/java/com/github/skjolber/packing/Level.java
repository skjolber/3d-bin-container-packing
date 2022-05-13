package com.github.skjolber.packing;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * A level within a container
 */
public class Level {

	final private ArrayList<Placement> placements = new ArrayList<>();
	private int height = 0;
	private int weight = 0;

	public int getHeight() {
		return height;
	}

	public int getWeight() {
		return weight;
	}

	/**
	 * Check whether placement is valid, i.e. no overlaps.
	 */
	public void validate() {
		for (int i = 0; i < placements.size(); i++) {
			for (int j = 0; j < placements.size(); j++) {
				if (j == i) {
					if (!placements.get(i).intersects(placements.get(j))) {
						throw new IllegalArgumentException();
					}
				} else {
					if (placements.get(i).intersects(placements.get(j))) {
						throw new IllegalArgumentException(i + " vs " + j + ": " + placements.get(i) + " vs " + placements.get(j));
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return placements.stream().map(Placement::toString).collect(Collectors.joining(","));
	}

	public int size() {
		return placements.size();
	}

	public Placement get(final int i) {
		return placements.get(i);
	}

	public void add(final Placement placement) {
		placements.add(placement);
		height = Math.max(height, placement.getBox().getHeight());
		weight += placement.getBox().getWeight();
	}

	public Iterable<Placement> iterable() {
		return placements;
	}

	public boolean isEmpty() {
		return placements.isEmpty();
	}
}
