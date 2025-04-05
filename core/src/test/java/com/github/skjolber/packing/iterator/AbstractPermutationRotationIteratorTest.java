package com.github.skjolber.packing.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractPermutationRotationIteratorTest {

	protected static void assertMinStackableVolumeValid(AbstractPermutationRotationIterator iterator) {
		for (int i = 0; i < iterator.length(); i++) {
			long calculatedMinStackableVolume = getMinStackableVolume(iterator, i);
			long cachedMinStackableVolume = iterator.getMinStackableVolume(i);

			assertEquals(calculatedMinStackableVolume, cachedMinStackableVolume, "Calculated " + calculatedMinStackableVolume + ", got " + cachedMinStackableVolume);
		}
	}

	protected static long getMinStackableVolume(AbstractPermutationRotationIterator iterator, int offset) {
		long minVolume = Long.MAX_VALUE;
		for (int i = offset; i < iterator.length(); i++) {
			PermutationRotation permutationRotation = iterator.get(i);
			long volume = permutationRotation.getBoxStackValue().getVolume();
			if(volume < minVolume) {
				minVolume = volume;
			}
		}
		return minVolume;
	}

}
