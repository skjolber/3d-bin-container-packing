package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;

/**
 * Combines multiple packagers into a tiered strategy.
 * <br>
 * Cheaper packagers are used to establish a baseline result, then more expensive packagers
 * are used to try to improve upon that baseline.
 */

public class CompositePackager extends AbstractCompositePackager<AbstractCompositePackager.CompositeResultBuilder> {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		protected Comparator<IntermediatePackagerResult> packagerResultComparator;
		protected final List<PackagerTier> tiers = new ArrayList<>();

		public Builder withPackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.packagerResultComparator = comparator;
			return this;
		}

		public Builder withTier(String name, PackagerAdapterBuilderFactory... factories) {
			tiers.add(new PackagerTier(name, Arrays.asList(factories)));
			return this;
		}

		public Builder withTier(String name, List<PackagerAdapterBuilderFactory> factories) {
			tiers.add(new PackagerTier(name, factories));
			return this;
		}

		public CompositePackager build() {
			if(packagerResultComparator == null) {
				packagerResultComparator = new DefaultIntermediatePackagerResultComparator();
			}
			return new CompositePackager(packagerResultComparator, tiers);
		}
	}

	public CompositePackager(Comparator<IntermediatePackagerResult> comparator, List<PackagerTier> packagers) {
		super(comparator, packagers);
	}

	@Override
	public CompositeResultBuilder newResultBuilder() {
		return new CompositeResultBuilder();
	}

}
