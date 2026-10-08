package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.point.Point;

public class Placement {

	protected BoxStackValue stackValue;
	/** The box item of the placed box, or null (for example the boundary placements of point calculators) */
	protected BoxItem boxItem;
	protected int x;
	protected int y;
	protected int z;
	
	protected int pointIndex;
	protected int index;

	protected long supportedArea;

	// -----------------------------------------------------------------------
	// Box-load tracking
	// -----------------------------------------------------------------------

	protected List<PlacementLoad> supporters;
	protected List<PlacementLoad> supportees;
	/**
	 * Total weight of all boxes resting on top of this placement.
	 * Includes all boxes in the vertical stack above, adjusted for area-proportional distribution.
	 * The value can be fractional when a box is shared by multiple supporters.
	 */
	protected double loadWeight;

	// scratch for propagateLoad(double)
	private transient double pendingLoad;
	private transient boolean pendingLoadQueued;
	
	protected Object properties;

	public Placement(BoxStackValue stackValue, int index, int x, int y, int z) {
		this(stackValue, index, x, y, z, true);
	}

	public Placement(BoxItem boxItem, BoxStackValue stackValue, int index, int x, int y, int z) {
		this(boxItem, stackValue, index, x, y, z, true);
	}

	public Placement(BoxStackValue stackValue, int index, int x, int y, int z, boolean load) {
		this(null, stackValue, index, x, y, z, load);
	}

	public Placement(BoxItem boxItem, BoxStackValue stackValue, int index, int x, int y, int z, boolean load) {
		super();
		this.boxItem = boxItem;
		this.stackValue = stackValue;
		this.pointIndex = index;
		
		this.x = x;
		this.y = y;
		this.z = z;
		if(load) {
			initializeLoad();
		}
	}

	public Placement(BoxStackValue stackValue, Point point) {
		this(stackValue, point, true);
	}

	public Placement(BoxItem boxItem, BoxStackValue stackValue, Point point) {
		this(boxItem, stackValue, point, true);
	}

	public Placement(BoxStackValue stackValue, Point point, boolean load) {
		this(stackValue, point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ(), load);
	}

	public Placement(BoxItem boxItem, BoxStackValue stackValue, Point point, boolean load) {
		this(boxItem, stackValue, point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ(), load);
	}

	public Placement() {
		this(true);
	}

	public Placement(boolean load) {
		if(load) {
			initializeLoad();
		}
	}

	private void initializeLoad() {
		supporters = new ArrayList<>(4);
		supportees = new ArrayList<>(4);
	}

	public BoxStackValue getStackValue() {
		return stackValue;
	}

	public void setStackValue(BoxStackValue stackValue) {
		this.stackValue = stackValue;
	}

	/**
	 * @param boxItem the box item of the placed box
	 * @param stackValue the rotation of the box
	 */
	public void setStackValue(BoxItem boxItem, BoxStackValue stackValue) {
		this.boxItem = boxItem;
		this.stackValue = stackValue;
	}

	public void setBoxItem(BoxItem boxItem) {
		this.boxItem = boxItem;
	}

	public boolean intersects(Placement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(Placement placement) {
		return y <= placement.getAbsoluteEndY() && placement.getAbsoluteY() <= getAbsoluteEndY();
	}

	public boolean intersectsX(Placement placement) {
		return x <= placement.getAbsoluteEndX() && placement.getAbsoluteX() <= getAbsoluteEndX();
	}

	public boolean intersectsZ(Placement placement) {
		return z <= placement.getAbsoluteEndZ() && placement.getAbsoluteZ() <= getAbsoluteEndZ();
	}

	public int getAbsoluteX() {
		return x;
	}

	public int getAbsoluteY() {
		return y;
	}

	public int getAbsoluteZ() {
		return z;
	}

	public int getAbsoluteEndX() {
		return x + stackValue.getDx() - 1;
	}

	public int getAbsoluteEndY() {
		return y + stackValue.getDy() - 1;
	}

	public int getAbsoluteEndZ() {
		return z + stackValue.getDz() - 1;
	}

	public long getVolume() {
		return stackValue.getBox().getVolume();
	}
	
	public boolean intersects2D(int placementX, int placementEndX, int placementY, int placementEndY) {
		return !(
				placementEndX < x || placementX > getAbsoluteEndX() || 
				placementEndY < y || placementY > getAbsoluteEndY()
				);
	}

	public boolean intersects2D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x || placement.getAbsoluteX() > getAbsoluteEndX() || 
				placement.getAbsoluteEndY() < y || placement.getAbsoluteY() > getAbsoluteEndY()
				);
	}

	// insertion order: the order in which boxes can be inserted into a container, see ContainerAccess.

	/**
	 * @param supporter another placement
	 * @return true if this placement rests on {@code supporter}: its bottom touches the top of {@code supporter}, and
	 *         their footprints overlap
	 */
	public boolean restsOn(Placement supporter) {
		return supporter.getAbsoluteEndZ() + 1 == z && intersects2D(supporter);
	}

	/**
	 * @param other another placement
	 * @param access how boxes get into the container
	 * @return true if this placement must be inserted before {@code other}: {@code other} rests on it, or it could not
	 *         be inserted after {@code other} (see {@link #isBlockedBy(Placement, ContainerAccess)})
	 */
	public boolean mustPrecede(Placement other, ContainerAccess access) {
		return other.restsOn(this) || isBlockedBy(other, access);
	}

	/**
	 * @param other another placement
	 * @param access how boxes get into the container
	 * @return true if inserting this placement would pass through {@code other}, were it already there: for
	 *         {@link ContainerAccess#TOP} when {@code other} is above it, for {@link ContainerAccess#FRONT} when
	 *         {@code other} is between it and the door; always false for {@link ContainerAccess#ANY}
	 */
	public boolean isBlockedBy(Placement other, ContainerAccess access) {
		return other.isInPathOf(x, y, z, getAbsoluteEndX(), getAbsoluteEndY(), getAbsoluteEndZ(), access);
	}

	/**
	 * As {@link #mustPrecede(Placement, ContainerAccess)}, for a box at the given coordinates (inclusive) which is not
	 * placed yet.
	 *
	 * @return true if this placement must be inserted before a box at the given coordinates
	 */
	public boolean mustPrecede(int x, int y, int z, int endX, int endY, int endZ, ContainerAccess access) {
		if(getAbsoluteEndZ() + 1 == z && intersects2D(x, endX, y, endY)) {
			// the box rests on this placement
			return true;
		}
		switch (access) {
			case TOP:
				// the box is above this placement
				return z > getAbsoluteEndZ() && intersects2D(x, endX, y, endY);
			case FRONT:
				// the box is between this placement and the door
				return x > getAbsoluteEndX()
						&& y <= getAbsoluteEndY() && this.y <= endY
						&& z <= getAbsoluteEndZ() && this.z <= endZ;
			default:
				return false;
		}
	}

	/**
	 * As {@link #mustPrecede(Placement, ContainerAccess)}, the other way around, for a box at the given coordinates
	 * (inclusive) which is not placed yet.
	 *
	 * @return true if a box at the given coordinates must be inserted before this placement
	 */
	public boolean mustFollow(int x, int y, int z, int endX, int endY, int endZ, ContainerAccess access) {
		if(endZ + 1 == this.z && intersects2D(x, endX, y, endY)) {
			// this placement rests on the box
			return true;
		}
		return isInPathOf(x, y, z, endX, endY, endZ, access);
	}

	/**
	 * @return true if this placement is in the path of a box at the given coordinates (inclusive), see
	 *         {@link #isBlockedBy(Placement, ContainerAccess)}
	 */
	protected boolean isInPathOf(int x, int y, int z, int endX, int endY, int endZ, ContainerAccess access) {
		switch (access) {
			case TOP:
				// this placement is above the box
				return this.z > endZ && intersects2D(x, endX, y, endY);
			case FRONT:
				// this placement is between the box and the door
				return this.x > endX
						&& y <= getAbsoluteEndY() && this.y <= endY
						&& z <= getAbsoluteEndZ() && this.z <= endZ;
			default:
				return false;
		}
	}

	/**
	 * The area in the xy plane shared with a rectangle (inclusive coordinates), for example the
	 * contact area with a box resting on this placement.
	 *
	 * <pre>
	 * |
	 * |           |---------|  rectangle
	 * |           |         |
	 * |    |-----------|    |
	 * |    |      |xxxx|    |  xxxx: overlap
	 * |    |      -----|----|
	 * |    |           |       this placement
	 * |    |-----------|
	 * |
	 * --------------------------------
	 * </pre>
	 *
	 * @return the overlap area, or 0 if the rectangle does not overlap this placement
	 */
	public long overlapArea2D(int placementX, int placementEndX, int placementY, int placementEndY) {
		int overlapMinX = Math.max(x, placementX);
		int overlapMaxX = Math.min(getAbsoluteEndX(), placementEndX);
		if(overlapMaxX < overlapMinX) {
			return 0L;
		}
		int overlapMinY = Math.max(y, placementY);
		int overlapMaxY = Math.min(getAbsoluteEndY(), placementEndY);
		if(overlapMaxY < overlapMinY) {
			return 0L;
		}
		// long arithmetic: areas can exceed the int range for fine-grained units
		return (long) (overlapMaxX - overlapMinX + 1) * (overlapMaxY - overlapMinY + 1);
	}

	/**
	 * @return the area in the xy plane shared with another placement, or 0 if they do not overlap
	 */
	public long overlapArea2D(Placement placement) {
		return overlapArea2D(placement.getAbsoluteX(), placement.getAbsoluteEndX(), placement.getAbsoluteY(), placement.getAbsoluteEndY());
	}

	public boolean intersects3D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x ||
				placement.getAbsoluteX() > getAbsoluteEndX() ||
				placement.getAbsoluteEndY() < y ||
				placement.getAbsoluteY() > getAbsoluteEndY() ||
				placement.getAbsoluteEndZ() < z ||
				placement.getAbsoluteZ() > getAbsoluteEndZ()
				);
	}

	@Override
	public String toString() {		
		Box box = stackValue.getBox();
		return (box != null ? box.getId() : "") + "[" +x + "x" + y + "x" + z + " " + getAbsoluteEndX() + "x"
				+ getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
	}
	
	public void setPoint(int index, int x, int y, int z) {
		this.pointIndex = index;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setPoint(Point point) {
		setPoint(point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ());
	}
	
	public int getWeight() {
		return stackValue.getBox().getWeight();
	}

	public BoxItem getBoxItem() {
		return boxItem;
	}
	
	public Box getBox() {
		return stackValue.getBox();
	}

	public int getPointIndex() {
		return pointIndex;
	}
	
	/**
	 * Total weight of all boxes resting on top of this placement.
	 * Includes all boxes in the vertical stack above, adjusted for area-proportional distribution.
	 *
	 * @return accumulated load weight, in the same units as {@link Box#getWeight()}
	 */
	public double getLoadWeight() {
		return loadWeight;
	}

	/**
	 * Returns the load pressure on the top surface of this placement,
	 * expressed as {@code loadWeight / topArea}, matching the
	 * convention used by {@link Box#getMinimumPressure()}.
	 *
	 * @return load pressure, or 0.0 if the area is zero
	 */
	public double getLoadPressure() {
		long area = stackValue.getArea();
		return Box.calculatePressure(area, loadWeight);
	}

	/**
	 * Returns the list of placements that are directly supported by this placement.
	 *
	 * @return list of supportees
	 */
	public List<PlacementLoad> getSupportees() {
		return supportees != null ? supportees : Collections.emptyList();
	}

	/**
	 * Returns the list of placements that are directly supporting this placement.
	 *
	 * @return list of supporters
	 */
	public List<PlacementLoad> getSupporters() {
		return supporters != null ? supporters : Collections.emptyList();
	}

	
	/**
	 * Records that {@code supportee} is resting on top of this placement.
	 * Sets up a two-way relationship and propagates weight and stack levels 
	 * down through the support graph.
	 *
	 * @param supportee the placement resting on top
	 * @param area the area shared between the two
	 * @param weight the initial weight share of the supportee box itself
	 */
	public void addLoad(Placement supportee, long area, double weight) {
		addSupportee(new PlacementLoad(supportee, area, weight));
		supportee.addSupporter(new PlacementLoad(this, area, weight));

		propagateLoad(weight);
	}
	
	protected void addSupportee(PlacementLoad supporter) {
		supportees.add(supporter);
	}

	protected void addSupporter(PlacementLoad supporter) {
		supporters.add(supporter);
		
		supportedArea += supporter.getArea();
	}

	/**
	 * @return the share of the weight passed down by this placement (its own and the load on it) which a supporter
	 *         carries: by contact area
	 */
	public double getShare(PlacementLoad supporterLink) {
		return supportedArea == 0 ? 0.0 : (double)supporterLink.getArea() / supportedArea;
	}

	/**
	 * Add a weight change to this placement, and spread it down the support graph, shared between
	 * supporters by contact area.
	 * <p>
	 * A placement which supports several placements can be reached through several paths (for example
	 * with staggered stacking), and walking every path grows exponentially with the stack height.
	 * So placements with a single supportee are updated right away (they are reached once), while
	 * placements with several supportees are queued and updated once all of their shares have
	 * arrived, highest first.
	 */
	protected void propagateLoad(double weightIncrement) {
		this.loadWeight += weightIncrement;

		List<Placement> pending = spreadLoad(weightIncrement, null);
		if(pending == null) {
			return;
		}
		int processed = 0;
		while (processed < pending.size()) {
			// supporters lie strictly below their supportees: the highest queued placement has
			// received all of its shares
			int highest = processed;
			for (int i = processed + 1; i < pending.size(); i++) {
				if(pending.get(i).z > pending.get(highest).z) {
					highest = i;
				}
			}
			Placement next = pending.get(highest);
			pending.set(highest, pending.get(processed));
			processed++;

			double increment = next.pendingLoad;
			next.pendingLoad = 0.0;
			next.pendingLoadQueued = false;
			next.loadWeight += increment;
			pending = next.spreadLoad(increment, pending);
		}
	}

	/**
	 * @return the queue of placements with several supportees, or null if none so far
	 */
	private List<Placement> spreadLoad(double increment, List<Placement> pending) {
		if(supporters == null) {
			return pending;
		}
		for (int i = 0; i < supporters.size(); i++) {
			PlacementLoad supporterLink = supporters.get(i);
			Placement supporter = supporterLink.getPlacement();
			double share = increment * getShare(supporterLink);
			if(supporter.supportees.size() == 1) {
				// reached only from this placement
				supporter.loadWeight += share;
				pending = supporter.spreadLoad(share, pending);
			} else {
				if(!supporter.pendingLoadQueued) {
					supporter.pendingLoadQueued = true;
					if(pending == null) {
						pending = new ArrayList<>();
					}
					pending.add(supporter);
				}
				supporter.pendingLoad += share;
			}
		}
		return pending;
	}

	public void removeLoad(Placement supportee) {
		if(supportees == null) {
			return;
		}
		for(int i = supportees.size() - 1; i >= 0; i--) {
			PlacementLoad supporteeLink = supportees.get(i);
			if(supporteeLink.getPlacement() == supportee) {
				supportees.remove(i);
				supportee.removeSupporter(this);
				propagateLoad(-supporteeLink.getWeight());
				break;
			}
		}
	}

	public void clearLoad() {
		if(supportees != null) {
			supportees.clear();
		}
		if(supporters != null) {
			supporters.clear();
		}
		
		loadWeight = 0.0;
		supportedArea = 0;
	}

	public void removeSupporter(Placement placement) {
		if(supporters == null) {
			return;
		}
		for(int i = 0; i < supporters.size(); i++) {
			PlacementLoad supporterLink = supporters.get(i);
			if(supporterLink.getPlacement() == placement) {
				supporters.remove(i);
				supportedArea -= supporterLink.getArea();
				
				propagateLoad(-supporterLink.getWeight());
				break;
			}
		}
	}

	public long getSupportedArea() {
		return supportedArea;
	}

	public void setSupportedArea(long supportedArea) {
		this.supportedArea = supportedArea;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}

	public boolean isWithinMaxLoadBoxCount(int levels) {
		if(stackValue.isMaxLoadBoxCount()) {
			if(stackValue.getMaxLoadBoxCount() < levels) {
				return false;
			}
		}
		
		levels++;
		if(supporters != null) {
			for (PlacementLoad placementLoad : supporters) {
				if(!placementLoad.getPlacement().isWithinMaxLoadBoxCount(levels)) {
					return false;
				}
			}
		}
		
		return true;
	}

	public <T> T getProperties() {
		return (T) properties;
	}

	public <T> void setProperties(T properties) {
		this.properties = properties;
	}

	/**
	 * Records that this placement touches the bottom of {@code supportee}, which was placed before it (in the order of
	 * the packager's search, for example into a gap under an overhang). The weight passed down by {@code supportee},
	 * its own and the load on it, is shared again between all of its supporters by contact area. In the result, the
	 * placements are put in insertion order (this placement before {@code supportee}), see {@link #mustPrecede(Placement, ContainerAccess)}.
	 * <p>
	 * Undo with {@link #removeSupporteesAbove()} before removing this placement's own supporter links.
	 *
	 * @param supportee a placement resting on this placement, placed before it
	 * @param area the contact area
	 */
	public void addSupporteeAbove(Placement supportee, long area) {
		double carried = supportee.getWeight() + supportee.loadWeight;
		supportee.shareLoad(-carried);
		supportees.add(new PlacementLoad(supportee, area, 0.0));
		supportee.addSupporter(new PlacementLoad(this, area, 0.0));
		supportee.shareLoad(carried);
	}

	/**
	 * Undo {@link #addSupporteeAbove(Placement, long)} for all supportees of this placement, in reverse order.
	 * Call when this placement is the last placed (any placements above it, placed after it, are removed),
	 * so that all of its supportees were linked by {@link #addSupporteeAbove(Placement, long)}.
	 */
	public void removeSupporteesAbove() {
		if(supportees == null) {
			return;
		}
		for(int i = supportees.size() - 1; i >= 0; i--) {
			Placement supportee = supportees.get(i).getPlacement();
			double carried = supportee.getWeight() + supportee.loadWeight;
			supportee.shareLoad(-carried);
			supportees.remove(i);
			// this placement was linked last
			PlacementLoad link = supportee.supporters.remove(supportee.supporters.size() - 1);
			supportee.supportedArea -= link.getArea();
			supportee.shareLoad(carried);
		}
	}

	/**
	 * Pass weight down to the supporters, shared by contact area.
	 */
	private void shareLoad(double weight) {
		if(supporters == null || supportedArea == 0) {
			return;
		}
		for (int i = 0; i < supporters.size(); i++) {
			PlacementLoad link = supporters.get(i);
			link.getPlacement().propagateLoad(weight * link.getArea() / supportedArea);
		}
	}

	public void removeLastSupportee() {
		PlacementLoad supporteeLink = supportees.remove(supportees.size() - 1);
		propagateLoad(-supporteeLink.getWeight());
	}


}
