package com.github.skjolber.packing.api.packager.control.manifest;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;

/**
 * Receives notifications of the packing attempt in some particular container. Implemented by {@link ManifestControls} and by
 * {@link com.github.skjolber.packing.api.packager.control.point.PointControls}. All methods are optional.
 * <p>
 * The notifications about box item groups ({@link #attempt(BoxItemGroup, int, int)}, {@link #attemptSuccess(BoxItemGroup)},
 * {@link #attemptFailure(BoxItemGroup)}, {@link #filteredGroups(List)} and {@link #undo(List)}) are sent when packing box item
 * groups only.
 * <p>
 * When a group cannot be fitted after some of its box items were placed, the packager first calls {@link #undo(List)} with the
 * placed box items on the manifest controls and then on the point controls, then
 * {@link com.github.skjolber.packing.api.packager.control.placement.PlacementControls#undo(List) PlacementControls.undo(..)}
 * with the placements (the undo calls are skipped if no box items were placed), and finally {@link #attemptFailure(BoxItemGroup)}
 * on the manifest controls and then on the point controls.
 * <p>
 * {@link #attempt(BoxItemGroup, int, int)} is sent to the manifest controls only, not to the point controls.
 */

public interface ManifestListener {

	/**
	 * 
	 * Notify box was loaded. 
	 * 
	 * @param boxItem {@linkplain BoxItem} to be added.
	 */
	
	default void accepted(BoxItem boxItem) {
	}
	
	/**
	 * 
	 * Notify some box items will not be attempted
	 * 
	 * @param boxItems list of box items
	 */
	
	default void declined(List<BoxItem> boxItems) {
		
	}

	/**
	 * 
	 * Notify box cannot be fitted, even it was previously accepted; usually because
	 * fitting the whole group was not possible.
	 * 
	 * @param boxItems {@linkplain BoxItem}
	 */
	
	default void undo(List<BoxItem> boxItems) {
	}


	/**
	 * 
	 * Notify attempting box group. 
	 * 
	 * @param group group
	 * @param offset box offset
	 * @param length box length from offset
	 */
	
	default void attempt(BoxItemGroup group, int offset, int length) {
	}
	
	/**
	 * 
	 * Notify box group was fitted.
	 * 
	 * @param group {@linkplain BoxItemGroup}
	 */
	
	default void attemptSuccess(BoxItemGroup group) {		
	}
	
	/**
	 * 
	 * Notify box group cannot be fitted.
	 * 
	 * @param group {@linkplain BoxItemGroup}
	 */
	
	default void attemptFailure(BoxItemGroup group) {
		declined(group.getItems());
	}


	/**
	 * 
	 * Notify box group cannot be fitted.
	 * 
	 * @param groups {@linkplain BoxItemGroup}
	 */
	
	default void filteredGroups(List<BoxItemGroup> groups) {
		for (BoxItemGroup boxItemGroup : groups) {
			declined(boxItemGroup.getItems());
		}
	}

}
