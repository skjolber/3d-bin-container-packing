package com.github.skjolber.packing;


/**
 * Created: 09.01.22   by: Armin Haaf
 * <p>
 *
 * @author Armin Haaf
 */
public class BagContainer extends Container {

	public BagContainer(final Container pContainer) {
		super(pContainer);

		rotateLargestAreaDown();
	}

	public BagContainer(final Dimension dimension, final int weight) {
		super(dimension, weight);
	}

	public BagContainer(final int w, final int d, final int h, final int weight) {
		super(w, d, h, weight);
	}

	public BagContainer(final String name, final int w, final int d, final int h, final int weight) {
		super(name, w, d, h, weight);
	}

	@Override
	public Container clone() {
		return new BagContainer(this);
	}

	/**
	 * Fits the given dimension into this bag, when use folding
	 */
	boolean canHoldWithFolding(final Dimension pDimension) {
		// bag logik, passt das mit auffalten
		// grösste Flache nach unten drehen
		final Dimension tLargestAreaDownDim = rotateLargestAreaDown(pDimension);

		final Dimension tFoldedDimension = new Dimension(tLargestAreaDownDim.getWidth(), tLargestAreaDownDim.getDepth()
				, calcFoldedHeightForBaseArea(tLargestAreaDownDim.getWidth(), tLargestAreaDownDim.getDepth()));

		return tFoldedDimension.canHold3D(pDimension);
	}

	private void rotateLargestAreaDown() {
		final Dimension tDimension = rotateLargestAreaDown(this);

		width = tDimension.getWidth();
		depth = tDimension.getDepth();
		height = tDimension.getHeight();
	}

	private Dimension rotateLargestAreaDown(Dimension pDimension) {
		final int tMax1Dim = Math.max(pDimension.getWidth(), Math.max(pDimension.getHeight(), pDimension.getDepth()));
		final int tMax2Dim = Math.min(pDimension.getWidth(), Math.max(pDimension.getHeight(), pDimension.getDepth()));
		final int tMax3Dim = Math.min(pDimension.getWidth(), Math.min(pDimension.getHeight(), pDimension.getDepth()));

		return new Dimension(tMax1Dim, tMax2Dim, tMax3Dim);
	}


	public Level addLevel() {
		// adapt box size to used base area
		if (levels.size() == 1) {
			// calc base area
			int tMaxX = 0;
			int tMaxY = 0;
			for (Placement tPlacement : levels.get(0)) {
				tMaxX = Math.max(tPlacement.getAbsoluteEndX(), tMaxX);
				tMaxY = Math.max(tPlacement.getAbsoluteEndY(), tMaxY);
			}

			foldBoxToBaseArea(tMaxX, tMaxY);
		}

		return super.addLevel();
	}

	void foldBoxToBaseArea(final int pMaxX, final int pMaxY) {
		final int tFoldLength = Math.min(width - pMaxX, depth - pMaxY);
		height += tFoldLength;
		width -= tFoldLength;
		depth -= tFoldLength;
		calculateVolume();
	}

	void foldBoxToHeight(final int tHeight) {
		if (tHeight > height) {
			final int tFoldLength = tHeight - height;
			height += tFoldLength;
			width -= tFoldLength;
			depth -= tFoldLength;
			calculateVolume();
		}
	}

	protected int calcFoldedHeightForBaseArea(int pWidth, int pDepth) {
		final int tFoldLength = Math.min(pWidth, pDepth);
		return height + tFoldLength;
	}


	public boolean isEmpty() {
		return levels.isEmpty() || levels.get(0).isEmpty();
	}
}
