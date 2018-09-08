package com.github.skjolberg.packing.properties;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.assertj.core.api.*;

class PropertyBasedTests {
  @Provide
  Arbitrary<String> shortStrings() {
    return Arbitraries.strings().withCharRange('a', 'z')
      .ofMinLength(1).ofMaxLength(8);
  }

  @Property
  boolean absoluteValueOfAllNumbersIsPositive(@ForAll @IntRange(min = -1000, max = 1000) int anInteger) {
    return Math.abs(anInteger) >= 0;
  }


  @Property
  void lengthOfConcatenatedStringIsGreaterThanLengthOfEach(
    @ForAll("shortStrings") String string1, @ForAll("shortStrings") String string2
  ) {
    String conc = string1 + string2;
    Assertions.assertThat(conc.length()).isGreaterThan(string1.length());
    Assertions.assertThat(conc.length()).isGreaterThan(string2.length());
  }
}
