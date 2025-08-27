package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Stack implements Serializable, Iterable<StackPlacement> {

	private static final long serialVersionUID = 1L;

	protected final List<StackPlacement> entries = new ArrayList<>();

	public Stack() {
	}

	public void addAll(List<StackPlacement> placements) {
		for (StackPlacement p : placements) {
			add(p);
		}
	}

	public List<StackPlacement> getPlacements() {
		return entries;
	}

	public void add(StackPlacement e) {
		entries.add(e);
	}

	public void remove(StackPlacement e) {
		entries.remove(e);
	}

	public void clear() {
		entries.clear();
	}

	public int getWeight() {
		int weight = 0;

		for (StackPlacement stackEntry : entries) {
			weight += stackEntry.getStackValue().getBox().getWeight();
		}

		return weight;
	}

	public int getDz() {
		int dz = 0;

		for (StackPlacement stackEntry : entries) {
			dz = Math.max(dz, stackEntry.getAbsoluteEndZ());
		}

		return dz;
	}

	public long getVolume() {
		long volume = 0;

		for (StackPlacement stackEntry : entries) {
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
	public Iterator<StackPlacement> iterator() {
		return entries.listIterator();
	}

}
