package com.github.skjolber.packing.virtualbox.bounds;

import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/** Complete-layout load checks using O(n) reusable storage and O(n²) work per candidate. */
public class VirtualBoxBoundsLoadSupport {

	protected final PackagerInterruptSupplier interrupt;

	public VirtualBoxBoundsLoadSupport(PackagerInterruptSupplier interrupt) {
		this.interrupt = interrupt;
	}

	protected void checkInterrupt() throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
	}

	/** Validate a complete assembly with its original orientations. */
	public boolean isValidLayout(Placement[] placements) throws PackagerInterruptedException {
		return isValidLayout(placements, null);
	}

	protected int[] bottomUp;
	protected int[] levels;
	protected double[] weights;
	protected double[] pressures;
	protected long[] supportedAreas;
	protected BoxItem[] identities;
	protected boolean[] identicalSubtree;

	public boolean isValidLayout(Placement[] placements, Map<BoxStackValue, BoxStackValue> originalValues) throws PackagerInterruptedException {
		int count = placements.length;
		if(bottomUp == null || bottomUp.length < count) {
			bottomUp = new int[count];
			levels = new int[count];
			weights = new double[count];
			pressures = new double[count];
			supportedAreas = new long[count];
			identities = new BoxItem[count];
			identicalSubtree = new boolean[count];
		}

		// Stable insertion sort avoids boxing indexes. A supporter always has a
		// lower bottom Z than its supportee; insertion/permutation order is irrelevant.
		for(int i = 0; i < count; i++) {
			checkInterrupt();
			weights[i] = 0;
			pressures[i] = 0;
			levels[i] = 0;
			identicalSubtree[i] = true;
			BoxStackValue value = placements[i].getStackValue();
			identities[i] = (originalValues == null ? value : originalValues.get(value)).getBox().getBoxItem();
			int position = i;
			while(position > 0 && placements[bottomUp[position - 1]].getAbsoluteZ() > placements[i].getAbsoluteZ()) {
				bottomUp[position] = bottomUp[position - 1];
				position--;
			}
			bottomUp[position] = i;
		}

		// Do not reject an overloaded partial assembly: another supporter placed
		// later can redistribute the load. Validate the complete contact graph top
		// down, carrying weight, maximum contact pressure, depth and type uniformity.
		for(int position = count - 1; position >= 0; position--) {
			checkInterrupt();
			int index = bottomUp[position];
			Placement upper = placements[index];
			BoxStackValue value = upper.getStackValue();
			if((value.isMaxLoadWeight() && weights[index] > value.getMaxLoadWeight())
					|| (value.isMaxLoadPressure() && pressures[index] > value.getMaxLoadPressure())
					|| (value.isMaxLoadBoxCount() && levels[index] > value.getMaxLoadBoxCount())
					|| (value.isLoadIdenticalBoxOnly() && !identicalSubtree[index])) {
				return false;
			}
			if(upper.getAbsoluteZ() == 0) {
				supportedAreas[index] = value.getArea();
				continue;
			}
			long totalArea = 0;
			for(int lowerPosition = 0; lowerPosition < position; lowerPosition++) {
				totalArea += contactArea(placements[bottomUp[lowerPosition]], upper);
			}
			supportedAreas[index] = totalArea;
			if(totalArea == 0) {
				// Like the existing load utilities, do not impose a stability rule.
				continue;
			}
			double totalWeight = upper.getWeight() + weights[index];
			for(int lowerPosition = 0; lowerPosition < position; lowerPosition++) {
				int lowerIndex = bottomUp[lowerPosition];
				long area = contactArea(placements[lowerIndex], upper);
				if(area == 0) {
					continue;
				}
				double share = totalWeight * area / totalArea;
				weights[lowerIndex] += share;
				pressures[lowerIndex] = Math.max(pressures[lowerIndex], Box.calculatePressure(area, share));
				levels[lowerIndex] = Math.max(levels[lowerIndex], levels[index] + 1);
				identicalSubtree[lowerIndex] &= identicalSubtree[index] && identities[lowerIndex] == identities[index];
			}
		}
		return true;
	}

	/** Build an independent support graph for the same placements that were just validated. */
	public Stack createSnapshot(Placement[] placements, Map<BoxStackValue, BoxStackValue> originalValues) {
		Stack snapshot = new Stack(placements.length);
		for(Placement placement : placements) {
			BoxStackValue value = placement.getStackValue();
			if(originalValues != null) {
				value = originalValues.get(value);
			}
			Placement copy = new Placement(value, placement.getPointIndex(), placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(), true);
			copy.setIndex(snapshot.size());
			snapshot.add(copy);
		}
		List<Placement> copies = snapshot.getPlacements();
		// Build bottom up: each supporter's links already exist when a new box
		// adds its own weight, so Placement propagates that weight to all ancestors.
		for(int position = 0; position < placements.length; position++) {
			int index = bottomUp[position];
			Placement upper = copies.get(index);
			if(upper.getAbsoluteZ() == 0) {
				upper.setSupportedArea(supportedAreas[index]);
				continue;
			}
			for(int lowerPosition = 0; lowerPosition < position; lowerPosition++) {
				Placement lower = copies.get(bottomUp[lowerPosition]);
				long area = contactArea(lower, upper);
				if(area != 0) {
					lower.addLoad(upper, area, (double) upper.getWeight() * area / supportedAreas[index]);
				}
			}
		}
		return snapshot;
	}

	protected static long contactArea(Placement lower, Placement upper) {
		if(lower.getAbsoluteEndZ() != upper.getAbsoluteZ() - 1) {
			return 0;
		}
		return LoadPlacementUtility.overlapArea(upper.getAbsoluteX(), upper.getAbsoluteY(), upper.getAbsoluteEndX(), upper.getAbsoluteEndY(), lower);
	}
}
