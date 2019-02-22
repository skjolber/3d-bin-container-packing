package com.github.skjolberg.packing;

import com.github.skjolberg.packing.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public class LargestAreaFitFirstPackager extends Packager {

	private final boolean footprintFirst;

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 */
	public LargestAreaFitFirstPackager(List<Container> containers) {
		this(containers, true, true, true);
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param footprintFirst start with box which has the largest footprint. If not, the highest box is first.
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a container that can hold the boxes, given time, it also tries to find a better match.
	 */

	public LargestAreaFitFirstPackager(List<Container> containers, boolean rotate3D, boolean footprintFirst, boolean binarySearch) {
		super(containers, rotate3D, binarySearch);

		this.footprintFirst = footprintFirst;
	}

	/**
	 *
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param containerProducts list of boxes to fit in a container.
	 * @param targetContainer the container to fit within
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return null if no match, or deadline reached
	 */

	public LAFFResult pack(List<Box> containerProducts, Container targetContainer, long deadline) {
		return pack(containerProducts, targetContainer, deadLinePredicate(deadline));
	}

	public LAFFResult pack(List<Box> containerProducts, Container targetContainer, long deadline, AtomicBoolean interrupt) {
		return pack(containerProducts, targetContainer, () -> deadlineReached(deadline) || interrupt.get());
	}

	public LAFFResult pack(List<Box> containerProducts, Container targetContainer,  BooleanSupplier interrupt) {
		Container holder = new Container(targetContainer);

		Dimension freeSpace = targetContainer;

		while(!containerProducts.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			// choose the box with the largest surface area, that fits
			// if the same then the one with minimum height

			// use a special case for boxes with full height
			Box currentBox = null;
			int currentIndex = -1;

			boolean fullHeight = false;
			for (int i = 0; i < containerProducts.size(); i++) {
				Box box = containerProducts.get(i);
				boolean fits;
				if(rotate3D) {
					fits = box.rotateLargestFootprint3D(freeSpace);
				} else {
					fits = box.fitRotate2D(freeSpace);
				}
				if(fits && box.getWeight() <= holder.getFreeWeight()) {
					if(currentBox == null) {
						currentBox = box;
						currentIndex = i;

						fullHeight = box.getHeight() == freeSpace.getHeight();
					} else {
						if(fullHeight) {
							if(box.getHeight() == freeSpace.getHeight()) {
								if(currentBox.getFootprint() < box.getFootprint()) {
									currentBox = box;
									currentIndex = i;
								}
							}
						} else {
							if(box.getHeight() == freeSpace.getHeight()) {
								fullHeight = true;

								currentBox = box;
								currentIndex = i;
							} else if(footprintFirst) {
								if(currentBox.getFootprint() < box.getFootprint()) {
									currentBox = box;
									currentIndex = i;
								} else if(currentBox.getFootprint() == box.getFootprint() && currentBox.getHeight() < box.getHeight()) {
									currentBox = box;
									currentIndex = i;
								}
							} else {
								if(currentBox.getHeight() < box.getHeight()) {
									currentBox = box;
									currentIndex = i;
								} else if(currentBox.getHeight() == box.getHeight() && currentBox.getFootprint() < box.getFootprint()) {
									currentBox = box;
									currentIndex = i;
								}
							}
						}
					}
				}
			}

			if(currentBox == null) {
				break;
			}

			// current box should have the optimal orientation already
			// create a space which holds the full level
			Space levelSpace = new Space(
						targetContainer.getWidth(),
						targetContainer.getDepth(),
						currentBox.getHeight(),
						0,
						0,
						holder.getStackHeight()
						);

			holder.addLevel();
			containerProducts.remove(currentIndex);

			if(!fit2D(containerProducts, holder, currentBox, levelSpace, interrupt)) {
				return null;
			}

			freeSpace = holder.getFreeSpace();
		}

		return new LAFFResult(containerProducts, holder);
	}

	/**
	 * Remove from list, more explicit implementation than {@linkplain List#remove} with no equals.
	 * @param containerProducts list of products
	 * @param currentBox item to remove
	 */
	private void removeIdentical(List<Box> containerProducts, Box currentBox) {
		for(int i = 0; i < containerProducts.size(); i++) {
			if(containerProducts.get(i) == currentBox) {
				containerProducts.remove(i);

				return;
			}
		}
		throw new IllegalArgumentException();
	}
	
	/**
	 * Fit in two dimensions
	 * 
	 * @param containerProducts products to fit
	 * @param holder target container
	 * @param usedSpace space to subtract
	 * @param freeSpace available space
	 * @param interrupt interrupt
	 * @return false if interrupted
	 */

	private boolean fit2D(List<Box> containerProducts, Container holder, Box usedSpace, Space freeSpace, BooleanSupplier interrupt) {

		if(rotate3D) {
			// minimize footprint
			usedSpace.fitRotate3DSmallestFootprint(freeSpace);
		}

		// add used space box now, but possibly rotate later - this depends on the actual remaining free space selected further down
		// there is up to possible 4 free spaces, 2 in which the used space box is rotated
		holder.add(new Placement(freeSpace, usedSpace));

		if(containerProducts.isEmpty()) {
			// no additional boxes
			// just make sure the used space fits in the free space
			usedSpace.fitRotate2D(freeSpace);

			return true;
		}

		if(interrupt.getAsBoolean()) {
			return false;
		}

		Space[] spaces = getFreespaces(freeSpace, usedSpace);

		Placement nextPlacement = bestVolumePlacement(containerProducts, spaces, holder.getFreeWeight());
		if(nextPlacement == null) {
			// no additional boxes
			// just make sure the used space fits in the free space
			usedSpace.fitRotate2D(freeSpace);

			return true;
		}

		// check whether the selected free space requires the used space box to be rotated
		if(nextPlacement.getSpace() == spaces[2] || nextPlacement.getSpace() == spaces[3]) {
			// the desired space implies that we rotate the used space box
			usedSpace.rotate2D();
		}

		// holder.validateCurrentLevel(); // uncomment for debugging

		removeIdentical(containerProducts, nextPlacement.getBox());

		// attempt to fit in the remaining (usually smaller) space first

		// stack in the 'sibling' space - the space left over between the used box and the selected free space
		Space remainder = nextPlacement.getSpace().getRemainder();
		if(remainder.nonEmpty()) {
			Box box = bestVolume(containerProducts, remainder, holder.getFreeWeight());
			if(box != null) {
				removeIdentical(containerProducts, box);

				if(!fit2D(containerProducts, holder, box, remainder, interrupt)) {
					return false;
				}
			}
		}
		// double check that there is still free weight
		// TODO possibly rewind if the value of the boxes in the remainder is lower than the one in next placement
		if(holder.getFreeWeight() >= nextPlacement.getBox().getWeight()) {
			if(!fit2D(containerProducts, holder, nextPlacement.getBox(), nextPlacement.getSpace(), interrupt)) {
				return false;
			}
		}
		return true;
	}

	private Space[] getFreespaces(Space freespace, Box used) {

		// Two free spaces, on each rotation of the used space.
		// Height is always the same, used box is assumed within free space height.
		// First:
		// ........................  ........................  .............
		// .                      .  .                      .  .           .
		// .                      .  .                      .  .           .
		// .          A           .  .          A           .  .           .
		// .                      .  .                      .  .           .
		// .                B     .  .                      .  .    B      .
		// ............           .  ........................  .           .
		// .          .           .                            .           .
		// .          .           .                            .           .
		// ........................                            .............
        //
		// Second:
		//
		// ........................   ........................  ..................
		// .                      .   .                      .  .                .
		// .          C           .   .         C            .  .                .
		// .                      .   .                      .  .                .
		// .......                .   ........................  .                .
		// .     .       D        .                             .        D       .
		// .     .                .                             .                .
		// .     .                .                             .                .
		// .     .                .                             .                .
		// ........................                             ..................
		//
		// So there is always a 'big' and a 'small' leftover area (the small is not shown).

		Space[] freeSpaces = new Space[4];
		if(freespace.getWidth() >= used.getWidth() && freespace.getDepth() >= used.getDepth()) {

			// if B is empty, then it is sufficient to work with A and the other way around

			// B
			if(freespace.getWidth() > used.getWidth()) {
				Space right = new Space(
						freespace.getWidth() - used.getWidth(), freespace.getDepth(), freespace.getHeight(),
						freespace.getX() + used.getWidth(), freespace.getY(), freespace.getZ()
						);

				Space rightRemainder = new Space(
						used.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight(),
					freespace.getX(), freespace.getY() + used.getDepth(), freespace.getZ()
						);
				right.setRemainder(rightRemainder);
				rightRemainder.setRemainder(right);
				freeSpaces[0] = right;
			}

			// A
			if(freespace.getDepth() > used.getDepth()) {
				Space top = new Space(
							freespace.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight(),
					freespace.getX(), freespace.getY() + used.depth, freespace.getZ()
						);
				Space topRemainder = new Space(
							freespace.getWidth() - used.getWidth(), used.getDepth(), freespace.getHeight(),
							freespace.getX() + used.getWidth(), freespace.getY(), freespace.getZ()
						);
				top.setRemainder(topRemainder);
				topRemainder.setRemainder(top);
				freeSpaces[1] = top;
			}
		}

		if(freespace.getWidth() >= used.getDepth() && freespace.getDepth() >= used.getWidth()) {
			// if D is empty, then it is sufficient to work with C and the other way around

			// D
			if(freespace.getWidth() > used.getDepth()) {
				Space right = new Space(
						freespace.getWidth() - used.getDepth(), freespace.getDepth(), freespace.getHeight(),
						freespace.getX() + used.getDepth(), freespace.getY(), freespace.getZ()
						);
				Space rightRemainder = new Space(
						used.getDepth(), freespace.getDepth() - used.getWidth(), freespace.getHeight(),
					freespace.getX(), freespace.getY() + used.getWidth(), freespace.getZ()
						);
				right.setRemainder(rightRemainder);
				rightRemainder.setRemainder(right);
				freeSpaces[2] = right;
			}

			// C
			if(freespace.getDepth() > used.getWidth()) {
				Space top = new Space(
						freespace.getWidth(), freespace.getDepth() - used.getWidth(), freespace.getHeight(),
					freespace.getX(), freespace.getY() + used.getWidth(), freespace.getZ()
						);
				Space topRemainder = new Space(
						freespace.getWidth() - used.getDepth(), used.getWidth(), freespace.getHeight(),
						freespace.getX() + used.getDepth(), freespace.getY(), freespace.getZ()
						);
				top.setRemainder(topRemainder);
				topRemainder.setRemainder(top);
				freeSpaces[3] = top;
			}
		}
		return freeSpaces;
	}

	private Box bestVolume(List<Box> containerProducts, Space space, int freeWeight) {

		Box bestBox = null;
		for(Box box : containerProducts) {
			if(box.getWeight() > freeWeight) {
				continue;
			}
			if(rotate3D) {
				if(box.canFitInside3D(space)) {
					if(bestBox == null) {
						bestBox = box;

						bestBox.fitRotate3DSmallestFootprint(space);
					} else if(bestBox.getVolume() < box.getVolume()) {
						bestBox = box;

						bestBox.fitRotate3DSmallestFootprint(space);
					} else if(bestBox.getVolume() == box.getVolume()) {
						// determine lowest fit
						box.fitRotate3DSmallestFootprint(space);

						if(box.getFootprint() < bestBox.getFootprint()) {
							bestBox = box;
						}
					}
				}
			} else {
				if(box.canFitInside2D(space)) {
					if(bestBox == null) {
						bestBox = box;
					} else if(bestBox.getVolume() < box.getVolume()) {
						bestBox = box;
					} else if(bestBox.getVolume() == box.getVolume()) {
						// TODO use the aspect ratio in some meaningful way
					}
				}
			}
		}
		return bestBox;
	}

	private Placement bestVolumePlacement(List<Box> containerProducts, Space[] spaces, int freeWeight) {

		Box bestBox = null;
		Space bestSpace = null;
		for(Space space : spaces) {
			if(space == null) {
				continue;
			}
			for(Box box : containerProducts) {
				if(box.getWeight() > freeWeight) {
					continue;
				}
				if(rotate3D) {
					if(box.canFitInside3D(space)) {
						if(bestBox == null) {
							bestBox = box;
							bestSpace = space;

							bestBox.fitRotate3DSmallestFootprint(bestSpace);
						} else if(bestBox.getVolume() < box.getVolume()) {
							bestBox = box;
							bestSpace = space;

							bestBox.fitRotate3DSmallestFootprint(bestSpace);
						} else if(bestBox.getVolume() == box.getVolume()) {
							// determine lowest fit
							box.fitRotate3DSmallestFootprint(space);

							if(box.getFootprint() < bestBox.getFootprint()) {
								bestBox = box;
								bestSpace = space;
							}

							// TODO if all else is equal, which free space is preferred?
						}
					}
				} else {
					if(box.canFitInside2D(space)) {
						if(bestBox == null) {
							bestBox = box;
							bestSpace = space;
						} else if(bestBox.getVolume() < box.getVolume()) {
							bestBox = box;
							bestSpace = space;
						}
						// TODO use the aspect ratio in some meaningful way

						// TODO if all else is equal, which free space is preferred?
					}
				}
			}
		}
		if(bestBox != null) {
			return new Placement(bestSpace, bestBox);
		}
		return null;
	}

	@Override
	protected Adapter adapter() {
		return new Adapter() {

			private List<Box> boxes;
			private LAFFResult previous;
			private List<Container> containers;

			@Override
			public PackResult attempt(int index, BooleanSupplier interrupt) {
				LAFFResult result = LargestAreaFitFirstPackager.this.pack(new ArrayList<>(boxes), containers.get(index), interrupt);

				return previous = result;
			}

			@Override
			public void initialize(List<BoxItem> boxItems, List<Container> container) {
				this.containers = container;

				List<Box> boxClones = new ArrayList<>(boxItems.size() * 2);

				for(BoxItem item : boxItems) {
					Box box = item.getBox();
					boxClones.add(box);
					for(int i = 1; i < item.getCount(); i++) {
						boxClones.add(box.clone());
					}
				}

				this.boxes = boxClones;
			}

			@Override
			public Container accepted(PackResult result) {
				LAFFResult laffResult = (LAFFResult)result;

				this.boxes = laffResult.getRemainingBoxes();

				if(previous == result) {
					return laffResult.getContainer();
				}

				// calculate again
				Container container = laffResult.getContainer();
				List<Box> boxes = new ArrayList<>(this.boxes.size());
				for(Level level : container.getLevels()) {
					for(Placement placement : level) {
						boxes.add(placement.getBox());
					}
				}

				container.clear();

				LAFFResult pack = LargestAreaFitFirstPackager.this.pack(boxes, container, Long.MAX_VALUE);

				return pack.getContainer();
			}

			@Override
			public boolean hasMore(PackResult result) {
				LAFFResult laffResult = (LAFFResult)result;
				return !laffResult.getRemainingBoxes().isEmpty();
			}

		};

	}
}
