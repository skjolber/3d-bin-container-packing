package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Stack implements Serializable, Iterable<Placement> {

	private static final long serialVersionUID = 1L;

	protected final List<Placement> entries = new ArrayList<>();

	public Stack() {
	}

	public void addAll(List<Placement> placements) {
		for (Placement p : placements) {
			add(p);
		}
	}

	public List<Placement> getPlacements() {
		return entries;
	}

	public void add(Placement e) {
		entries.add(e);
	}

	public void remove(Placement e) {
		entries.remove(e);
	}

	public void clear() {
		entries.clear();
	}

	public int getWeight() {
		int weight = 0;

		for (Placement stackEntry : entries) {
			weight += stackEntry.getStackValue().getBox().getWeight();
		}

		return weight;
	}

	public int getDz() {
		int dz = 0;

		for (Placement stackEntry : entries) {
			dz = Math.max(dz, stackEntry.getAbsoluteEndZ());
		}

		return dz;
	}

	public long getVolume() {
		long volume = 0;

		for (Placement stackEntry : entries) {
			volume += stackEntry.getStackValue().getBox().getVolume();
		}

		return volume;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public int size() {
		return entries.size();
	}

	public void setSize(int size) {
		while (size < entries.size()) {
			entries.remove(entries.size() - 1);
		}
	}

	@Override
	public Iterator<Placement> iterator() {
		return entries.listIterator();
	}

}
