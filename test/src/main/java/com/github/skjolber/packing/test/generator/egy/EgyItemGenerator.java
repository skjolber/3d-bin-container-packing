package com.github.skjolber.packing.test.generator.egy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;

import com.github.skjolber.packing.test.generator.ItemGenerator;

/**
 * Item generator based on details found in the paper
 * "Three-Dimensional Bin Packing and Mixed-Case Palletization"
 * by Samir Elhedhli, Fatma Gzara and Burak Yildiz.
 *
 */

public class EgyItemGenerator implements ItemGenerator<EgyItem> {

	public static Clazz CLASS_1 = new Clazz.Builder()
			.withCategory(Category.K1, BigDecimal.valueOf(28.48))
			.withCategory(Category.K2, BigDecimal.valueOf(58.75))
			.withCategory(Category.K3, BigDecimal.valueOf(12.67))
			.withCategory(Category.K4, BigDecimal.valueOf(0.1))
			.withCategory(Category.K5, BigDecimal.valueOf(0))
			.withName("1")
			.build();

	public static Clazz CLASS_2 = new Clazz.Builder()
			.withCategory(Category.K1, BigDecimal.valueOf(33.08))
			.withCategory(Category.K2, BigDecimal.valueOf(32.36))
			.withCategory(Category.K3, BigDecimal.valueOf(23.34))
			.withCategory(Category.K4, BigDecimal.valueOf(7.94))
			.withCategory(Category.K5, BigDecimal.valueOf(3.28))
			.withName("2")
			.build();

	public static Clazz CLASS_3 = new Clazz.Builder()
			.withCategory(Category.K1, BigDecimal.valueOf(66.88))
			.withCategory(Category.K2, BigDecimal.valueOf(24.75))
			.withCategory(Category.K3, BigDecimal.valueOf(5.7))
			.withCategory(Category.K4, BigDecimal.valueOf(2.6))
			.withCategory(Category.K5, BigDecimal.valueOf(0.08))
			.withName("3")
			.build();

	public static Clazz CLASS_4 = new Clazz.Builder()
			.withCategory(Category.K1, BigDecimal.valueOf(78.58))
			.withCategory(Category.K2, BigDecimal.valueOf(13.16))
			.withCategory(Category.K3, BigDecimal.valueOf(6.33))
			.withCategory(Category.K4, BigDecimal.valueOf(1.78))
			.withCategory(Category.K5, BigDecimal.valueOf(0.15))
			.withName("4")
			.build();

	private final RandomDataGenerator randomDataGenerator;

	private NormalDistribution depthWidthRatio = new NormalDistribution(0.695d, 0.118d);
	private LogNormalDistribution heightWidthRatio = new LogNormalDistribution(-0.654d, 0.453d);
	private LogNormalDistribution repetition = new LogNormalDistribution(0.544d, 0.658d);

	// relative to dm^3. i.e. 1000 for cm, a million for mm
	private int volumeReference = 1000;
	private Clazz clazz;

	public EgyItemGenerator(Clazz clazz) {
		this(clazz, new RandomDataGenerator(), 1000);
	}

	public EgyItemGenerator(Clazz clazz, RandomDataGenerator randomDataGenerator, int volumeReference) {
		super();
		this.clazz = clazz;
		this.randomDataGenerator = randomDataGenerator;
		this.volumeReference = volumeReference;
	}

	@Override
	public List<EgyItem> getItems(int count) {
		CategoryCounts counts = clazz.getCounts(count);

		List<EgyItem> items = new ArrayList<>();

		for (Category category : Category.values()) {

			int c = counts.getCount(category);
			while (c > 0) {

				double volume = category.getVolume(randomDataGenerator);

				EgyItem item = getItem((volume * volumeReference), c, category);

				c -= item.getCount();

				items.add(item);
			}
		}

		return items;
	}

	private EgyItem getItem(double volume, int maxCount, Category category) {

		// dy / dx = depthWidth
		// dy      = dx * depthWidth
		double depthWidth = depthWidthRatio.sample();

		// dz / dx = heightWidth
		// dz      = heightWidth * dx
		double heightWidth = heightWidthRatio.sample();
		// v    = dx * dy * dz 
		// v    = dx * dx * depthWidth * dx * heightWidth
		// dx^3 = v / (depthWidth * heightWidth)
		// dx   = (v / (depthWidth * heightWidth))^(1/3)

		double referenceLength = Math.pow(volume / (depthWidth * heightWidth), 1.0 / 3.0);

		int dx = Math.max(1, (int)Math.round(referenceLength));

		// adjust for rounding error
		referenceLength *= referenceLength / dx;

		int dy = Math.max(1, (int)Math.round(depthWidth * referenceLength));

		// adjust for round errors by taking the full volume and dividing by actual other dimensions
		int dz = Math.max(1, (int)Math.round(volume / (dx * dy)));

		// adjust for rounding errors
		double sample = repetition.sample();
		if(sample < 1) {
			sample = 1;
		}
		int count = Math.min(maxCount, (int)Math.floor(sample));

		return new EgyItem(dx, dy, dz, count, category);
	}

}
