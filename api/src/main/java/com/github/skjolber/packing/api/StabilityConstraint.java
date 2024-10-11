package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.ep.Point3D;

/**
 * Builder scaffold.
 * 
 * @see <a href=
 *      "https://www.sitepoint.com/self-types-with-javas-generics/">https://www.sitepoint.com/self-types-with-javas-generics/</a>
 */

@SuppressWarnings("unchecked")
public interface StabilityConstraint {
	
	boolean supportsPlacement(Point3D point, List<StackPlacement> supports);
	
}
