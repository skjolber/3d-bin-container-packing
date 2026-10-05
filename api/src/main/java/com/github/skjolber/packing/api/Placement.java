package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.point.Point;

public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	protected BoxStackValue stackValue;
	protected int x;
	protected int y;
	protected int z;
	
	protected int pointIndex;
	protected int index;

	protected long supportedArea;
	/** Area resting on boxes placed later, which carry part of the load but do not count as support (see {@link Unloading#ANY_ORDER}) */
	protected long lateSupportedArea;

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

	public Placement(BoxStackValue stackValue, int index, int x, int y, int z, boolean load) {
		super();
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

	public Placement(BoxStackValue stackValue, Point point, boolean load) {
		this(stackValue, point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ(), load);
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

	public boolean intersects(Placement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(Placement placement) {
		int endY = y + stackValue.getDy() - 1;

		if (y <= placement.getAbsoluteY() && placement.getAbsoluteY() <= endY) {
			return true;
		}

		return y <= placement.getAbsoluteY() + placement.getStackValue().getDy() - 1
				&& placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(Placement placement) {
		int endX = x + stackValue.getDx() - 1;

		if (x <= placement.getAbsoluteX() && placement.getAbsoluteX() <= endX) {
			return true;
		}

		return x <= placement.getAbsoluteX() + placement.getStackValue().getDx() - 1
				&& placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 <= endX;
	}

	public boolean intersectsZ(Placement placement) {
		int endZ = z + stackValue.getDz() - 1;

		if (z <= placement.getAbsoluteZ() && placement.getAbsoluteZ() <= endZ) {
			return true;
		}

		return z <= placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1
				&& placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1 <= endZ;
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
		return stackValue.getBox().getBoxItem();
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
		
		if(supporter.isLate()) {
			lateSupportedArea += supporter.getArea();
		} else {
			supportedArea += supporter.getArea();
		}
	}

	/**
	 * The share of the weight passed down by this placement which a supporter carries: by contact area, among the
	 * supporters placed before this placement for those, and among all supporters for late supporters
	 * (see {@link Unloading#ANY_ORDER}). The shares of the earlier supporters sum to one.
	 */
	public double getShare(PlacementLoad supporterLink) {
		if(supporterLink.isLate()) {
			return (double)supporterLink.getArea() / (supportedArea + lateSupportedArea);
		}
		return supportedArea == 0 ? 0.0 : (double)supporterLink.getArea() / supportedArea;
	}

	/**
	 * @return the area resting on boxes placed later, which carry part of the load but do not count as support
	 */
	public long getLateSupportedArea() {
		return lateSupportedArea;
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
		lateSupportedArea = 0;
	}

	public void removeSupporter(Placement placement) {
		if(supporters == null) {
			return;
		}
		for(int i = 0; i < supporters.size(); i++) {
			PlacementLoad supporterLink = supporters.get(i);
			if(supporterLink.getPlacement() == placement) {
				supporters.remove(i);
				if(supporterLink.isLate()) {
					lateSupportedArea -= supporterLink.getArea();
				} else {
					supportedArea -= supporterLink.getArea();
				}
				
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
	 * Records that this placement touches the bottom of {@code supportee}, which was placed before it (for example, this
	 * placement went into a gap under an overhang). The weight passed down by {@code supportee}, its own and the load on it,
	 * is shared again between all of its supporters by contact area, so its earlier supporters are relieved.
	 * <p>
	 * Undo with {@link #removeSupporteesAbove()} before removing this placement's own supporter links.
	 *
	 * @param supportee a placement resting on this placement, placed before it
	 * @param area the contact area
	 * @param unloading how the boxes are unloaded: with {@link Unloading#REVERSE_LOADING_ORDER}, this placement relieves the other supporters of {@code supportee}
	 */
	public void addSupporteeAbove(Placement supportee, long area, Unloading unloading) {
		boolean late = unloading == Unloading.ANY_ORDER;
		double carried = supportee.getWeight() + supportee.loadWeight;
		// with late links, only the shares of the late links change
		supportee.shareLoad(-carried, late);
		supportees.add(new PlacementLoad(supportee, area, 0.0, late));
		supportee.addSupporter(new PlacementLoad(this, area, 0.0, late));
		supportee.shareLoad(carried, late);
	}

	/**
	 * Undo {@link #addSupporteeAbove(Placement, long, Unloading)} for all supportees of this placement, in reverse order.
	 * Call when this placement is the last placed (any placements above it, placed after it, are removed),
	 * so that all of its supportees were linked by {@link #addSupporteeAbove(Placement, long, Unloading)}.
	 */
	public void removeSupporteesAbove() {
		if(supportees == null) {
			return;
		}
		for(int i = supportees.size() - 1; i >= 0; i--) {
			PlacementLoad supporteeLink = supportees.get(i);
			Placement supportee = supporteeLink.getPlacement();
			boolean late = supporteeLink.isLate();
			double carried = supportee.getWeight() + supportee.loadWeight;
			supportee.shareLoad(-carried, late);
			supportees.remove(i);
			// this placement was linked last
			PlacementLoad link = supportee.supporters.remove(supportee.supporters.size() - 1);
			if(late) {
				supportee.lateSupportedArea -= link.getArea();
			} else {
				supportee.supportedArea -= link.getArea();
			}
			supportee.shareLoad(carried, late);
		}
	}

	/**
	 * Pass weight down to the supporters, see {@link #getShare(PlacementLoad)}.
	 *
	 * @param lateOnly only to the late supporters
	 */
	private void shareLoad(double weight, boolean lateOnly) {
		if(supporters == null) {
			return;
		}
		for (int i = 0; i < supporters.size(); i++) {
			PlacementLoad link = supporters.get(i);
			if(lateOnly && !link.isLate()) {
				continue;
			}
			double share = getShare(link);
			if(share > 0.0) {
				link.getPlacement().propagateLoad(weight * share);
			}
		}
	}

	public void removeLastSupportee() {
		PlacementLoad supporteeLink = supportees.remove(supportees.size() - 1);
		propagateLoad(-supporteeLink.getWeight());
	}


}
