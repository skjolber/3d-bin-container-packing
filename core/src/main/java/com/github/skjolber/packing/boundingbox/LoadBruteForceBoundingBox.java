package com.github.skjolber.packing.boundingbox;

/**
 * Bounding-box search respecting each orientation's load weight, contact
 * pressure, maximum stack depth and identical-item-only constraints.
 * Identical means the same input box item, not merely equal dimensions.
 *
 * <p>Constraints are evaluated on complete assemblies, including supports
 * inserted below previously placed boxes. Only valid assemblies participate in
 * objective comparison or goal evaluation. Results contain independent support
 * graphs with area-proportional load weights. As with the geometric variant,
 * groups and obstacles are unsupported; full support and stability are not
 * enforced. Close this reusable component after all operations finish.</p>
 */
public class LoadBruteForceBoundingBox extends BruteForceBoundingBox {

	@Override
	protected boolean supportsLoad() {
		return true;
	}
}
