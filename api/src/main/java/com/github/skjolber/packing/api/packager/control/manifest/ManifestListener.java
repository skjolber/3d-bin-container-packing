package com.github.skjolber.packing.api.packager.control.manifest;

import java.util.List;

import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * 
 * Listener for items which are available for load into some particular container.
 * 
 * The filter is expected to maintain underlying {@linkplain BoxItemSource} and {@linkplain PointSource} instances.
 * 
 */

public interface ManifestListener {

	/**
	 * 
	 * Notify box was loaded. 
	 * 
	 * @param boxItem {@linkplain RemainingBoxItem} to be added.
	 */
	
	default void accepted(RemainingBoxItem boxItem) {
	}
	
	/**
	 * 
	 * Notify some box items will not be attempted
	 * 
	 * @param boxItems list of box items
	 */
	
	default void declined(List<RemainingBoxItem> boxItems) {
		
	}

	/**
	 * 
	 * Notify box cannot be fitted, even it was previously accepted; usually because
	 * fitting the whole group was not possible.
	 * 
	 * @param boxItems {@linkplain RemainingBoxItem}
	 */
	
	default void undo(List<RemainingBoxItem> boxItems) {
	}


	/**
	 * 
	 * 
	 * @param group {@linkplain RemainingBoxItemGroup} to be added.
	 */
	
	/**
	 * 
	 * Notify attempting box group. 
	 * 
	 * @param group group
	 * @param offset box offset
	 * @param length box length from offset
	 */
	
	default void attempt(RemainingBoxItemGroup group, int offset, int length) {
	}
	
	/**
	 * 
	 * Notify box group was fitted.
	 * 
	 * @param group {@linkplain RemainingBoxItemGroup}
	 */
	
	default void attemptSuccess(RemainingBoxItemGroup group) {		
	}
	
	/**
	 * 
	 * Notify box group cannot be fitted.
	 * 
	 * @param group {@linkplain RemainingBoxItemGroup}
	 */
	
	default void attemptFailure(RemainingBoxItemGroup group) {
		declined(group.getItems());
	}


	/**
	 * 
	 * Notify box group cannot be fitted.
	 * 
	 * @param groups {@linkplain RemainingBoxItemGroup}
	 */
	
	default void filteredGroups(List<RemainingBoxItemGroup> groups) {
		for (RemainingBoxItemGroup boxItemGroup : groups) {
			declined(boxItemGroup.getItems());
		}
	}

}
