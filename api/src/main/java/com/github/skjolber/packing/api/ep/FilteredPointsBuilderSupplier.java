package com.github.skjolber.packing.api.ep;

import java.util.function.Supplier;

@FunctionalInterface
public interface FilteredPointsBuilderSupplier<B extends FilteredPointsBuilder<B>> extends Supplier<FilteredPointsBuilder<B>> {

}
