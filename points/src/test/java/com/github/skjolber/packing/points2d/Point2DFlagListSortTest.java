package com.github.skjolber.packing.points2d;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.ep.points2d.DefaultPoint2D;
import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.ep.points2d.Point2DFlagList;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;

/**
 * The custom sort must order points exactly like a stable sort (the JDK's {@link Arrays#sort}),
 * including ties, for random input and for the shape produced by add(..): new points in front
 * of an already sorted tail.
 */
public class Point2DFlagListSortTest {

	@Test
	public void matchesStableSortForRandomInput() {
		Random random = new Random(1);
		for(int run = 0; run < 500; run++) {
			int size = 1 + random.nextInt(120);
			SimplePoint2D[] points = randomPoints(random, size);
			check(points, size);
		}
	}

	@Test
	public void matchesStableSortForUnsortedFrontAndSortedTail() {
		Random random = new Random(2);
		for(int run = 0; run < 500; run++) {
			int size = 1 + random.nextInt(120);
			SimplePoint2D[] points = randomPoints(random, size);
			int front = random.nextInt(Math.min(size, 40) + 1);
			Arrays.sort(points, front, size, (a, b) -> Point2D.COMPARATOR_X_THEN_Y.compare(a, b));
			check(points, size);
		}
	}

	private static void check(SimplePoint2D[] points, int size) {
		Point2DFlagList list = new Point2DFlagList();
		list.ensureCapacity(size);
		for(SimplePoint2D point : points) {
			list.add(point);
		}
		SimplePoint2D[] expected = points.clone();
		Arrays.sort(expected, (a, b) -> Point2D.COMPARATOR_X_THEN_Y.compare(a, b));

		list.sort(Point2D.COMPARATOR_X_THEN_Y, size);

		for(int i = 0; i < size; i++) {
			// identity: ties must keep their original order
			assertThat(list.get(i)).isSameAs(expected[i]);
		}
	}

	private static SimplePoint2D[] randomPoints(Random random, int size) {
		SimplePoint2D[] points = new SimplePoint2D[size];
		for(int i = 0; i < size; i++) {
			// small ranges give many ties
			int minX = random.nextInt(5);
			int minY = random.nextInt(5);
			points[i] = new DefaultPoint2D(minX, minY, 0, minX + random.nextInt(3), minY + random.nextInt(3), 0);
		}
		return points;
	}
}
