package com.github.skjolber.packing.points;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

/**
 * Golden master for the point calculators: seeded random placement sequences, with the free-point
 * geometry and support flags folded into a checksum after every placement. The expected checksums were
 * recorded from the implementation before performance work; optimizations must preserve them exactly.
 *
 * <pre>
 *   +-----------------------+        each step: pick a free point, place a box at its corner
 *   |  +---+                |        or offset inside it (floating), sometimes change the
 *   |  | B |   +--+         |        minimum area/volume limits, then checksum all points
 *   |  +---+   |B |         |
 *   |          +--+         |
 *   +-----------------------+
 * </pre>
 *
 * To re-record after an intentional behavior change, run with {@code -Dgolden.record=true}.
 */
public class PointCalculatorGoldenMasterTest {

	private static final int SEEDS = 24;
	private static final int STEPS = 200;

	private static final long[] EXPECTED_3D_MUTABLE = {
		-5091955106269232822L, -9075537510941027718L, -1446335560864807970L, -7060589060891959669L,
		2268993211140725798L, -2606865701560117283L, 1111862289235288376L, -3421424455031371834L,
		8002882743117567759L, 2735615249543178125L, -1680514528705940648L, 2288536373508989728L,
		969014741794828526L, 5646762956130876109L, -292302895029578590L, -7852511976673704913L,
		-3532465099854914122L, 6614992129290841881L, 6635267996967180732L, -2935806678993017604L,
		7976888051881208228L, -1572426230001127204L, 5049605832295452238L, 5262720378191622198L,
	};
	private static final long[] EXPECTED_3D_IMMUTABLE = {
		4139592842144258060L, 8277922522430812166L, -1815660599616930327L, 4740330908093859835L,
		3375018131030552866L, -8504845018327524076L, -6208411202264400511L, 6311551245043936981L,
		6359288708667618993L, 1974906733453362578L, -8261988966407273560L, -8230711104866404394L,
		5956924450454977348L, -7502988105662392187L, 7567254309997882539L, -3780101190158387449L,
		-1589917649292432214L, 8057576493806682361L, -2585266645793161947L, 5187316566187774399L,
		7644715946901533471L, -5642909004867392625L, 2842397440989686168L, -7684562711459624737L,
	};
	private static final long[] EXPECTED_2D_MUTABLE = {
		-1322936976661508749L, 190855659021914729L, -3258986275347276741L, 369101785702569237L,
		8100404957236031956L, -8593132933210028506L, 4699579175474342898L, -3000586306322219256L,
		-6619327306655791935L, 3599571999483713988L, -5319057628804655396L, -6165426722917010803L,
		537269486782452460L, -4581077419601733641L, 3758402415483185620L, -1080255120972536994L,
		-3037465317908368103L, -8486137078818799844L, -8503376472533255581L, 7807306852238332788L,
		8212348766512851478L, -4039683516882337234L, 8498895073245071869L, -7251712054584162658L,
	};
	private static final long[] EXPECTED_2D_IMMUTABLE = {
		5400140453997381053L, 6518580451192971286L, -4091428061526465044L, 8151442911684805321L,
		3598034177619272712L, 510675097974690725L, -1201060531409869174L, -1635275387096419665L,
		8226623571685856535L, -9189861780580928709L, 8097846019759335518L, 242662592723197006L,
		6871037548362121882L, -3988822388121629211L, 4911018785416659144L, 6781667666690919232L,
		7280740764397059383L, -1396954978587258679L, -7080499368477635481L, 7807306852238332788L,
		790960887361254188L, -6241564227597200807L, -4060761575220765607L, -4407809559709546367L,
	};

	@Test
	public void calculator3DMutable() {
		check("EXPECTED_3D_MUTABLE", EXPECTED_3D_MUTABLE, seed -> run3D(seed, false));
	}

	@Test
	public void calculator3DImmutable() {
		check("EXPECTED_3D_IMMUTABLE", EXPECTED_3D_IMMUTABLE, seed -> run3D(seed, true));
	}

	@Test
	public void calculator2DMutable() {
		check("EXPECTED_2D_MUTABLE", EXPECTED_2D_MUTABLE, seed -> run2D(seed, false));
	}

	@Test
	public void calculator2DImmutable() {
		check("EXPECTED_2D_IMMUTABLE", EXPECTED_2D_IMMUTABLE, seed -> run2D(seed, true));
	}

	private interface Run {
		long run(long seed);
	}

	private static void check(String name, long[] expected, Run run) {
		long[] actual = new long[SEEDS];
		for(int i = 0; i < SEEDS; i++) {
			actual[i] = run.run(i);
		}
		if(Boolean.getBoolean("golden.record")) {
			StringBuilder builder = new StringBuilder("private static final long[] " + name + " = {");
			for(int i = 0; i < actual.length; i++) {
				builder.append(i % 4 == 0 ? "\n\t\t" : " ").append(actual[i]).append("L,");
			}
			System.out.println(builder.append("\n\t};"));
			return;
		}
		assertThat(actual).containsExactly(expected);
	}

	protected static long run3D(long seed, boolean immutable) {
		Random random = new Random(seed);
		DefaultPointCalculator3D calculator = new DefaultPointCalculator3D(immutable, STEPS);
		calculator.clearToSize(10 + random.nextInt(40), 10 + random.nextInt(40), 10 + random.nextInt(40));
		long hash = 17;
		for(int step = 0; step < STEPS && !calculator.isEmpty(); step++) {
			if(random.nextInt(10) == 0) {
				calculator.setMinimumAreaAndVolumeLimit(random.nextInt(4), random.nextInt(8));
			}
			int index = random.nextInt(calculator.size());
			SimplePoint3D point = calculator.get(index);
			int dx = 1 + random.nextInt(Math.min(point.getDx(), 8));
			int dy = 1 + random.nextInt(Math.min(point.getDy(), 8));
			int dz = 1 + random.nextInt(Math.min(point.getDz(), 8));
			int x = point.getMinX();
			int y = point.getMinY();
			int z = point.getMinZ();
			if(random.nextInt(4) == 0) {
				// floating: not at the point's corner
				x += random.nextInt(point.getDx() - dx + 1);
				y += random.nextInt(point.getDy() - dy + 1);
				z += random.nextInt(point.getDz() - dz + 1);
			}
			calculator.add(index, placement(dx, dy, dz, x, y, z));
			hash = hash * 31 + step;
			for(int i = 0; i < calculator.size(); i++) {
				SimplePoint3D p = calculator.get(i);
				hash = fold(hash, p.getMinX(), p.getMinY(), p.getMinZ(), p.getMaxX(), p.getMaxY(), p.getMaxZ());
				hash = fold(hash, p.isSupportedXYPlane() ? 1 : 0, p.isSupportedXZPlane() ? 1 : 0, p.isSupportedYZPlane() ? 1 : 0,
						p.isSupportedXYPlane(p.getMaxX(), p.getMaxY()) ? 1 : 0, p.isSupportedXZPlane(p.getMaxX(), p.getMaxZ()) ? 1 : 0,
						p.isSupportedYZPlane(p.getMaxY(), p.getMaxZ()) ? 1 : 0);
			}
		}
		return hash;
	}

	protected static long run2D(long seed, boolean immutable) {
		Random random = new Random(seed);
		DefaultPointCalculator2D calculator = new DefaultPointCalculator2D(immutable, STEPS);
		calculator.clearToSize(10 + random.nextInt(40), 10 + random.nextInt(40), 1);
		long hash = 17;
		for(int step = 0; step < STEPS && !calculator.isEmpty(); step++) {
			if(random.nextInt(10) == 0) {
				calculator.setMinimumAreaLimit(random.nextInt(4));
			}
			int index = random.nextInt(calculator.size());
			SimplePoint2D point = calculator.get(index);
			int dx = 1 + random.nextInt(Math.min(point.getDx(), 8));
			int dy = 1 + random.nextInt(Math.min(point.getDy(), 8));
			int x = point.getMinX();
			int y = point.getMinY();
			if(random.nextInt(4) == 0) {
				x += random.nextInt(point.getDx() - dx + 1);
				y += random.nextInt(point.getDy() - dy + 1);
			}
			calculator.add(index, placement(dx, dy, 1, x, y, 0));
			hash = hash * 31 + step;
			for(int i = 0; i < calculator.size(); i++) {
				SimplePoint2D p = calculator.get(i);
				hash = fold(hash, p.getMinX(), p.getMinY(), p.getMaxX(), p.getMaxY(), p.isXSupport(p.getMaxX()) ? 1 : 0, p.isYSupport(p.getMaxY()) ? 1 : 0);
			}
		}
		return hash;
	}

	private static long fold(long hash, int a, int b, int c, int d, int e, int f) {
		hash = hash * 31 + a;
		hash = hash * 31 + b;
		hash = hash * 31 + c;
		hash = hash * 31 + d;
		hash = hash * 31 + e;
		return hash * 31 + f;
	}

	private static Placement placement(int dx, int dy, int dz, int x, int y, int z) {
		return new Placement(Box.newBuilder()
				.withSize(dx, dy, dz)
				.withWeight(1)
				.build().getStackValue(0), -1, x, y, z, false);
	}
}
