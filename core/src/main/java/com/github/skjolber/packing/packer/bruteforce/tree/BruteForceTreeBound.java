package com.github.skjolber.packing.packer.bruteforce.tree;

import java.util.Arrays;

/**
 * The most the remaining boxes can add: the sub-multiset of them within the load volume and weight left with the most volume, then weight, then
 * boxes, which is what the first boxes of the best order of them can load. With box item groups an order takes the groups in sequence, so the
 * sub-multiset is the first groups whole and a sub-multiset of the next. It depends only on the remaining multiset (the placed boxes are the
 * rest), so it is computed once per multiset, by enumerating the sub-multisets of a group; for a group with too many (over 4096) the whole
 * group within the load left is taken, which is a looser bound. When all boxes fit the load limits there is nothing to compute.
 */
final class BruteForceTreeBound {

	static final long MAX_SUBSETS = 1L << 12;

	private long[] volumes;
	private long[] weights;
	private final int types;
	final boolean allFit;
	private final long maxLoadVolume;
	private final long maxLoadWeight;
	private long totalVolume;
	private long totalWeight;
	private final int[] counts;
	private final long[] fitVolume;
	private final long[] fitWeight;
	private final int[] fitCount;
	/** the smallest area and volume of the remaining boxes, by multiset key (0 until computed); null for too many keys */
	private final long[] minArea;
	private final long[] minVolume;
	private long bestVolume;
	private long bestWeight;
	private int bestCount;

	/** per type, the end of its group's range of types */
	private final int[] groupEnd;
	/** per type, whether its group has too many sub-multisets to enumerate */
	private final boolean[] manySubsets;

	/**
	 * @param keys the number of multiset keys
	 * @param groupEnd per type, the end of its group's range of types
	 */
	BruteForceTreeBound(int[] counts, long[] volumes, long[] weights, long maxLoadVolume, long maxLoadWeight, int keys, int[] groupEnd) {
		this.counts = counts;
		this.volumes = volumes;
		this.weights = weights;
		this.types = counts.length;
		this.maxLoadVolume = maxLoadVolume;
		this.maxLoadWeight = maxLoadWeight;
		this.groupEnd = groupEnd;
		this.manySubsets = new boolean[types];
		for (int t = 0; t < types; t++) {
			if(counts[t] == 0) {
				continue;
			}
			int end = groupEnd[t];
			long subsets = 1L;
			for (int k = t; k < end && subsets <= MAX_SUBSETS; k++) {
				subsets *= counts[k] + 1;
			}
			for (int k = t; k < end; k++) {
				manySubsets[k] = subsets > MAX_SUBSETS;
			}
			t = end - 1;
		}
		long volume = 0L;
		long weight = 0L;
		for (int t = 0; t < types; t++) {
			volume += counts[t] * volumes[t];
			weight += counts[t] * weights[t];
		}
		totalVolume = volume;
		totalWeight = weight;
		allFit = volume <= maxLoadVolume && weight <= maxLoadWeight;
		int size = keys;
		if(allFit) {
			fitVolume = null;
			fitWeight = null;
			fitCount = null;
		} else {
			fitVolume = new long[size];
			fitWeight = new long[size];
			fitCount = new int[size];
			Arrays.fill(fitVolume, -1L);
		}
		minArea = new long[size];
		minVolume = new long[size];
	}

	/**
	 * Reset for other boxes of the same counts and groups.
	 *
	 * @return false if the boxes do not fit the load limits as the previous ones did (or do): then the tables are built anew
	 */
	boolean reset(long[] volumes, long[] weights) {
		long volume = 0L;
		long weight = 0L;
		for (int t = 0; t < types; t++) {
			volume += counts[t] * volumes[t];
			weight += counts[t] * weights[t];
		}
		if(allFit != (volume <= maxLoadVolume && weight <= maxLoadWeight)) {
			return false;
		}
		this.volumes = volumes;
		this.weights = weights;
		totalVolume = volume;
		totalWeight = weight;
		if(fitVolume != null) {
			Arrays.fill(fitVolume, -1L);
		}
		Arrays.fill(minArea, 0L);
		Arrays.fill(minVolume, 0L);
		return true;
	}

	/** the smallest area of the remaining boxes (with the key), over their rotations */
	long minArea(int[] remaining, int key, long[] minAreas) {
		long area = minArea[key];
		if(area == 0L) {
			area = Long.MAX_VALUE;
			for (int t = 0; t < types; t++) {
				if(remaining[t] > 0 && minAreas[t] < area) {
					area = minAreas[t];
				}
			}
			minArea[key] = area;
		}
		return area;
	}

	/** the smallest volume of the remaining boxes (with the key) */
	long minVolume(int[] remaining, int key) {
		long volume = minVolume[key];
		if(volume == 0L) {
			volume = Long.MAX_VALUE;
			for (int t = 0; t < types; t++) {
				if(remaining[t] > 0 && volumes[t] < volume) {
					volume = volumes[t];
				}
			}
			minVolume[key] = volume;
		}
		return volume;
	}

	/** whether a prefix with the volume, weight and count, plus the best fit of the remaining boxes, can beat neither the bound nor the best arrangement */
	/** @param anyBest whether a prefix which can at most tie the best is hopeless too */
	boolean isHopeless(int[] remaining, int key, long volume, long weight, int count, long boundVolume, boolean haveBest, long bestVolume, long bestWeight, int bestCount, boolean anyBest) {
		long v;
		long w;
		int c;
		if(allFit) {
			v = totalVolume;
			w = totalWeight;
			c = count;
			for (int t = 0; t < types; t++) {
				c += remaining[t];
			}
		} else {
			maxFit(remaining, key);
			v = volume + fitVolume[key];
			w = weight + fitWeight[key];
			c = count + fitCount[key];
		}
		if(v < boundVolume) {
			return true;
		}
		return haveBest && compare(v, w, c, bestVolume, bestWeight, bestCount) < (anyBest ? 1 : 0);
	}

	private void maxFit(int[] remaining, int key) {
		if(fitVolume[key] == -1L) {
			long volume = 0L;
			long weight = 0L;
			for (int t = 0; t < types; t++) {
				volume += remaining[t] * volumes[t];
				weight += remaining[t] * weights[t];
			}
			// the load left for the remaining boxes: the placed ones are the rest
			long volumeLeft = maxLoadVolume - (totalVolume - volume);
			long weightLeft = maxLoadWeight - (totalWeight - weight);
			bestVolume = 0L;
			bestWeight = 0L;
			bestCount = 0;
			// the groups in sequence: the first ones whole, as far as they fit, and the sub-multisets of the next
			long prefixVolume = 0L;
			long prefixWeight = 0L;
			int prefixCount = 0;
			int start = 0;
			while (start < types && volumeLeft - prefixVolume >= 0L && weightLeft - prefixWeight >= 0L) {
				if(remaining[start] == 0) {
					start++;
					continue;
				}
				int end = groupEnd[start];
				if(manySubsets[start]) {
					// the whole group within the load left: at least what its sub-multisets can load
					long groupVolume = 0L;
					long groupWeight = 0L;
					int groupCount = 0;
					for (int t = start; t < end; t++) {
						groupVolume += remaining[t] * volumes[t];
						groupWeight += remaining[t] * weights[t];
						groupCount += remaining[t];
					}
					offer(prefixVolume + Math.min(groupVolume, volumeLeft - prefixVolume), prefixWeight + Math.min(groupWeight, weightLeft - prefixWeight), prefixCount + groupCount);
				} else {
					best(remaining, start, end, volumeLeft - prefixVolume, weightLeft - prefixWeight, prefixVolume, prefixWeight, prefixCount);
				}
				for (int t = start; t < end; t++) {
					prefixVolume += remaining[t] * volumes[t];
					prefixWeight += remaining[t] * weights[t];
					prefixCount += remaining[t];
				}
				start = end;
			}
			fitVolume[key] = bestVolume;
			fitWeight[key] = bestWeight;
			fitCount[key] = bestCount;
		}
	}

	static int compare(long volume, long weight, int count, long otherVolume, long otherWeight, int otherCount) {
		if(volume != otherVolume) {
			return volume < otherVolume ? -1 : 1;
		}
		if(weight != otherWeight) {
			return weight < otherWeight ? -1 : 1;
		}
		return Integer.compare(count, otherCount);
	}

	private void offer(long volume, long weight, int count) {
		if(volume > bestVolume || (volume == bestVolume && (weight > bestWeight || (weight == bestWeight && count > bestCount)))) {
			bestVolume = volume;
			bestWeight = weight;
			bestCount = count;
		}
	}

	/** the sub-multisets of the types t..end within the load left, on top of the volume, weight and count */
	private void best(int[] remaining, int t, int end, long volumeLeft, long weightLeft, long volume, long weight, int count) {
		if(t == end) {
			offer(volume, weight, count);
			return;
		}
		long v = volumes[t];
		long w = weights[t];
		for (int k = 0; k <= remaining[t]; k++) {
			long kv = k * v;
			long kw = k * w;
			if(kv > volumeLeft || kw > weightLeft) {
				break;
			}
			best(remaining, t + 1, end, volumeLeft - kv, weightLeft - kw, volume + kv, weight + kw, count + k);
		}
	}
}
