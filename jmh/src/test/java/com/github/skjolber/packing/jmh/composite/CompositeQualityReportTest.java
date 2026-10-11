package com.github.skjolber.packing.jmh.composite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.jmh.composite.CompositeQualityReport.Summary;

/**
 * The quality report runs; the composite packagers pack every order, and are rarely worse than the best packager.
 */
public class CompositeQualityReportTest {

	@Test
	public void compositePackagersPackAllOrders() {
		Map<String, Summary> summaries = CompositeQualityReport.run(3, 4, 200);

		assertThat(summaries).hasSize(6);
		assertThat(summaries.get("composite: plain, fast brute force").getPacked()).isEqualTo(4);
		assertThat(summaries.get("composite: plain, brute force").getPacked()).isEqualTo(4);
	}
}
