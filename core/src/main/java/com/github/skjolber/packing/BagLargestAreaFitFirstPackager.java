package com.github.skjolber.packing;

import java.util.List;

/**
 * Created: 09.01.22   by: Armin Haaf
 * <p>
 * A Simple Bag Adaption -> if a product Box exists, which only fits in a folded box, use this as first product
 * and change the box size
 * if first level is done, fold the bag size to win some additional height
 *
 * @author Armin Haaf
 */
public class BagLargestAreaFitFirstPackager extends LargestAreaFitFirstPackager {
	public BagLargestAreaFitFirstPackager(final List<Container> containers) {
		super(containers);
	}

	public BagLargestAreaFitFirstPackager(final List<Container> containers, final boolean rotate3D, final boolean footprintFirst, final boolean binarySearch, final int checkpointsPerDeadlineCheck) {
		super(containers, rotate3D, footprintFirst, binarySearch, checkpointsPerDeadlineCheck);
	}

	@Override
	protected int getBestBox(final Container pContainer, final Dimension pFreeSpace, final List<Box> pContainerProducts) {
		// falls Container leer ist und pContainer ein Bag -> das spezial logik
		if (pContainer instanceof BagContainer && ((BagContainer)pContainer).isEmpty()) {
			return getFirstBoxForBag((BagContainer)pContainer, pFreeSpace, pContainerProducts);
		}
		return super.getBestBox(pContainer, pFreeSpace, pContainerProducts);
	}

	private int getFirstBoxForBag(final BagContainer pHolder, final Dimension pFreeSpace, final List<Box> pContainerProducts) {
		int tCurrentIndex = -1;

		// check if a Box exists, which fits only by folding the bag
		// if one exists, use it, if more than one exists, use the one with the largest volume
		for (int i = 0; i < pContainerProducts.size(); ++i) {
			final Box tBox = pContainerProducts.get(i);
			// check max weight
			if (tBox.getWeight() <= pHolder.getFreeWeight()) {
				// is it a box, which fits only by folding
				if (!pHolder.canHold3D(tBox) && pHolder.canHoldWithFolding(tBox)) {
					// use the best box
					if (tCurrentIndex < 0 || isBetterFit(tBox, pContainerProducts.get(tCurrentIndex))) {
						tCurrentIndex = i;
					}
				}
			}
		}

		if (tCurrentIndex >= 0) {
			// fold box to height of selected Box
			final Box tSelectedBox = pContainerProducts.get(tCurrentIndex);
			pHolder.foldBoxToHeight(getShortestSide(tSelectedBox));
		} else {
			tCurrentIndex = super.getBestBox(pHolder, pFreeSpace, pContainerProducts);
		}

		return tCurrentIndex;
	}

	private int getShortestSide(Box pBox) {
		return Math.min(pBox.getWidth(), Math.min(pBox.getDepth(), pBox.getHeight()));
	}

	protected boolean isBetterFit(final Box pBox, final Box pReference) {
		return pBox.getVolume() > pReference.getVolume();
	}
}
