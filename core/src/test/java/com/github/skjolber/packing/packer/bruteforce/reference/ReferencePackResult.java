package com.github.skjolber.packing.packer.bruteforce.reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;

/**
 * Ported from master @ e79cf716 (4.2.4) as a reference implementation for differential tests. The recursion and
 * enumeration order are intentionally preserved - do not modernize or optimize.
 * <p>
 * Origin: {@code BruteForceIntermediatePackagerResult}, reduced to what the single-container search needs: the
 * points of the placed boxes and the permutation and rotation state they were found for. The placements, the packed
 * box count and the load are derived from those, as 4.x did (its placements were reused work objects, so a result
 * only kept the points and the state).
 */

public class ReferencePackResult {

	public static ReferencePackResult empty() {
		return new ReferencePackResult(null);
	}

	// work objects
	private final ReferencePermutationRotationIterator iterator;

	// state
	private ReferencePermutationRotationState state;
	private List<Point> points = Collections.emptyList();

	private boolean dirty = true;

	private long loadVolume;
	private int loadWeight;

	public ReferencePackResult(ReferencePermutationRotationIterator iterator) {
		this.iterator = iterator;
	}

	public void calculateWeightAndVolume() {
		if(dirty) {
			dirty = false;

			long loadVolume = 0;
			int loadWeight = 0;

			if(!points.isEmpty()) {
				List<BoxStackValue> list = iterator.get(state, points.size());

				for(BoxStackValue v : list) {
					Box box = v.getBox();

					loadVolume += box.getVolume();
					loadWeight += box.getWeight();
				}
			}

			this.loadVolume = loadVolume;
			this.loadWeight = loadWeight;
		}
	}

	public void setState(List<Point> items, ReferencePermutationRotationState state) {
		this.points = items;
		this.state = state;
		this.dirty = true;
	}

	public void reset() {
		this.points = Collections.emptyList();
		this.state = null;
		this.dirty = true;
	}

	public boolean isEmpty() {
		return points.isEmpty();
	}

	/**
	 * @return the number of packed boxes
	 */
	public int getSize() {
		return points.size();
	}

	/**
	 * @return the number of packed boxes
	 */
	public int getBoxCount() {
		return points.size();
	}

	/**
	 * @return the total volume of the packed boxes
	 */
	public long getLoadVolume() {
		calculateWeightAndVolume();
		return loadVolume;
	}

	/**
	 * @return the total weight of the packed boxes
	 */
	public int getLoadWeight() {
		calculateWeightAndVolume();
		return loadWeight;
	}

	public ReferencePermutationRotationState getState() {
		return state;
	}

	/**
	 * @return new placements, in the order the boxes were placed
	 */
	public List<Placement> getPlacements() {
		if(points.isEmpty()) {
			return Collections.emptyList();
		}
		int[] permutations = state.getPermutations();
		List<BoxStackValue> list = iterator.get(state, points.size());

		List<Placement> placements = new ArrayList<>(points.size());
		for (int i = 0; i < points.size(); i++) {
			BoxStackValue value = list.get(i);

			if(value.getBox() != iterator.getBoxItems()[permutations[i]].getBox()) {
				throw new IllegalStateException();
			}
			placements.add(new Placement(iterator.getBoxItems()[permutations[i]], value, points.get(i), false));
		}
		return placements;
	}

	/**
	 * @return the box items of the packed boxes, in the order the boxes were placed
	 */
	public List<BoxItem> getBoxItems() {
		List<BoxItem> result = new ArrayList<>(points.size());
		if(!points.isEmpty()) {
			int[] permutations = state.getPermutations();
			for (int i = 0; i < points.size(); i++) {
				result.add(iterator.getBoxItems()[permutations[i]]);
			}
		}
		return result;
	}

}
