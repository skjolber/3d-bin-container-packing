package com.github.skjolber.packing.ep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement;

/**
 * 
 * Custom list for working with placements. Capacity must be specified in constructor or using designated 
 * methods.
 * 
 */

@SuppressWarnings("unchecked")
public class PlacementList {

	private int size = 0;
	private Placement[] placements;

	public PlacementList() {
		this(16);
	}

	public PlacementList(int initialSize) {
		placements = new Placement[initialSize];
	}
	
	public PlacementList(PlacementList list) {
		placements = new Placement[list.placements.length];
		System.arraycopy(list.placements, 0, placements, 0, list.size);
		size = list.size;
	}

	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	public void ensureCapacity(int size) {
		if(placements.length < size) {
			int nextSize = size + 16;
			Placement[] nextPoints = new Placement[nextSize];
			System.arraycopy(this.placements, 0, nextPoints, 0, this.size);
			this.placements = nextPoints;
		}
	}

	public void add(Placement placement) {
		placements[size] = placement;
		size++;
	}

	public int size() {
		return size;
	}

	public void reset() {
		Arrays.fill(this.placements, 0, size, null);
		size = 0;
	}

	public Placement get(int i) {
		return placements[i];
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void clear() {
		size = 0;
	}

	/**
	 * Returns the hash code value for this list.
	 *
	 * 
	 * This implementation uses exactly the code that is used to define the
	 * list hash function in the documentation for the {@link List#hashCode}
	 * method.
	 *
	 * @return the hash code value for this list
	 */
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < size; i++) {
			hashCode = 31 * hashCode + placements[i].hashCode();
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PlacementList) {
			PlacementList other = (PlacementList)obj;
			if(other.size() == size) {
				for (int i = 0; i < size; i++) {
					if(!placements[i].equals(other.get(i))) {
						return false;
					}
				}
			}
			return true;
		}
		return super.equals(obj);
	}

	public Placement[] getPlacements() {
		return placements;
	}

	public void sort(Comparator<Placement> comparator) {
		Arrays.sort(placements, 0, size, comparator);
	}
	
	public void insertionSort(Comparator<Placement> comparator) {
        int n = size;
        for (int i = 1; i < n; i++) { // Start from the second element
            Placement key = placements[i]; // Element to be inserted
            int j = i - 1;

            // Move elements of arr[0..i-1], that are greater than key,
            // to one position ahead of their current position
            while (j >= 0 && comparator.compare(placements[j], key) > 0) {
            	placements[j + 1] = placements[j];
                j--;
            }
            placements[j + 1] = key; // Place key at its correct position
        }
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public List<Placement> toList() {
		List<Placement> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(placements[i]);
		}
		return list;
	}

	public void addAll(PlacementList placements) {
		for(int i = 0; i < placements.size; i++) {
			add(placements.placements[i]);
		}
	}

	public int getCapacity() {
		return placements.length;
	}
	
}
