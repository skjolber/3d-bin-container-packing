package com.github.skjolber.packing.api;

import java.io.Closeable;

public interface Validator<B extends ValidatorResultBuilder> extends Closeable {

	B newResultBuilder();

}
