package com.github.skjolber.packing.packer.bruteforce.reference;

import static com.github.skjolber.packing.packer.bruteforce.reference.ReferenceSupport.NO_ROTATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.v4.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.v4.iterator.PermutationRotationState;
import com.github.skjolber.packing.v4.packer.PackagerInterruptedException;

/**
 * Pins the reference, the relocated 4.x code run through {@link Version4Reference}, to known results: the rows below were
 * produced by running the 4.x {@code DefaultBoxItemPermutationRotationIterator} and {@code BruteForcePackager} (master @ e79cf716,
 * the single container attempt of its box item adapter) on the scenarios, outside of this repository. They guard the relocation,
 * the adapter and a change of the 4.x version (the property {@code v4.version} of legacy/v4).
 * <p>
 * A row is a scenario, and then the checksum of its enumeration (the hash of every permutation and rotation state, in
 * order), the number of states, and the load volume, load weight and box count of the best packing.
 * <p>
 * Scenario: {@code name;dx;dy;dz;max load weight;box items} where a box item is {@code id,dx,dy,dz,rotation,weight,count} with
 * rotation 0 (none), 1 (2D) or 2 (3D).
 */
class ReferenceGoldenMasterTest {

	private static final String[] ROWS = {
		"s0;3;3;1;1000000;square,2,2,1,0,1,1|long,3,1,1,1,1,2|short,2,1,1,1,1,1|unit,1,1,1,0,1,1 # -8712864758780172304,480,9,4,4",
		"s1;7;1;1;7;heavy,1,1,1,0,3,2|middle,2,1,1,0,2,1|light,1,1,1,0,1,2 # 6725025032320827426,30,5,7,4",
		"s3;2;1;1;1000000;unit,1,1,1,0,1,1|double,2,1,1,0,1,1 # 29614176,2,2,1,1",
		"s4;5;6;1;1000000;box-0,4,4,3,2,3,3|box-1,5,6,3,2,2,3|box-2,3,2,1,2,3,1 # 1013949,2,6,3,1",
		"s5;5;3;3;1000000;box-0,2,1,1,1,1,1|box-1,1,2,2,1,3,1 # -2840533254980928384,8,6,4,2",
		"s6;5;5;3;1000000;box-0,4,1,1,2,3,1|box-1,2,2,2,2,1,1|box-2,5,4,4,0,1,2|box-3,3,4,2,1,3,1|box-4,3,3,3,2,3,1 # 5936941804674328576,96,39,7,3",
		"s7;6;5;4;1000000;box-0,3,1,3,1,1,2|box-1,3,1,2,0,2,3|box-2,3,1,3,2,1,2 # 713314062890394272,7560,54,10,7",
		"s8;6;4;4;1000000;box-0,5,2,2,2,2,3 # 953312,1,60,6,3",
		"s9;3;4;4;1000000;box-0,1,2,1,1,3,3|box-1,1,1,2,2,1,1 # 3021901564360244480,96,8,10,4",
		"s10;6;6;2;1000000;box-0,4,6,5,2,2,2 # 0,0,0,0,0",
		"s11;4;6;2;1000000;box-0,2,2,2,1,2,3|box-1,1,1,1,2,2,1 # 26255454555924672,4,25,8,4",
		"s12;3;4;2;1000000;box-0,1,3,3,2,3,3|box-1,1,4,4,0,1,1|box-2,4,3,2,2,2,1 # 26255482985595136,4,24,2,1",
		"s13;6;4;2;1000000;box-0,1,2,2,2,3,3|box-1,2,1,2,2,2,1|box-2,2,2,2,2,3,1|box-3,1,2,2,1,3,1 # 2187358430090316928,19440,28,17,6",
		"s14;6;6;2;1000000;box-0,1,2,5,0,3,2|box-1,5,3,5,0,3,1|box-2,3,1,1,2,1,1 # 1013949,2,3,1,1",
		"s15;3;3;1;1000000;box-0,1,1,1,1,1,2|box-1,1,1,1,0,1,1|box-2,1,1,1,0,3,1 # -7720394932169805632,12,4,6,4",
		"s16;3;4;4;1000000;box-0,2,4,2,0,3,1|box-1,3,1,4,1,3,3|box-2,3,1,4,1,1,1|box-3,3,4,4,1,2,2 # -299274371648449984,6720,48,10,4",
		"s17;4;4;1;2;box-0,1,1,1,2,2,1|box-1,1,1,1,2,1,2|box-2,1,1,1,2,1,1 # 3953706173925652096,12,2,2,2",
		"s18;3;6;1;1000000;box-0,5,1,5,0,1,1|box-1,5,3,1,0,2,1 # 0,0,0,0,0",
		"s19;5;6;1;1000000;box-0,1,1,1,2,2,1|box-1,1,1,1,1,3,2 # 882265560222,3,3,8,3",
		"s20;4;5;1;2;box-0,5,4,1,1,2,2|box-1,5,5,3,2,2,1|box-2,2,5,2,0,2,1|box-3,5,4,4,2,2,1 # 30752,1,20,2,1",
		"s21;3;4;2;2;box-0,2,1,1,0,2,1|box-1,2,1,1,1,2,1|box-2,1,1,2,2,3,2|box-3,2,1,1,0,1,2 # 1805289820821266496,24,4,2,2",
		"s22;3;6;3;1000000;box-0,4,5,5,1,1,1|box-1,1,5,2,2,2,3 # -1054536439512484484,8,30,6,3",
		"s23;4;6;3;1000000;box-0,2,1,2,0,2,2|box-1,2,1,2,0,1,3 # 4164050797969949370,10,20,7,5",
		"s24;4;5;3;2;box-0,5,5,5,1,3,2|box-1,5,1,4,1,3,2|box-2,5,2,1,1,1,2|box-3,5,4,1,0,3,1 # 32736,1,20,2,2",
		"s25;5;5;4;2;box-0,2,1,2,0,1,1|box-1,2,1,1,2,2,1 # 6810156233236008128,6,4,1,1",
		"s26;5;4;3;2;box-0,4,4,5,0,3,1|box-1,2,2,4,0,1,3|box-2,1,1,3,0,2,1|box-3,5,1,5,2,1,1|box-4,1,1,1,1,2,1 # 31553536,2,3,2,1",
		"s27;6;4;4;2;box-0,3,2,2,2,1,2|box-1,1,3,3,1,3,1 # 2253857769650018112,9,24,2,2",
		"s28;6;3;3;1000000;box-0,1,5,5,0,2,2|box-1,6,2,3,2,2,2|box-2,6,6,3,1,2,2 # 28202264591360,4,36,2,1",
		"s29;5;3;3;1000000;box-0,1,2,1,0,2,2|box-1,1,1,2,1,2,1|box-2,1,2,1,1,3,1 # 1198080590222935360,24,8,9,4",
		"s30;4;4;4;2;box-0,1,4,3,0,1,1|box-1,2,3,3,2,3,1|box-2,1,1,1,0,1,1 # 29644928,2,13,2,2",
		"s31;5;4;2;2;box-0,1,2,2,0,2,2|box-1,1,1,1,2,3,1|box-2,2,1,1,0,1,2 # -8353996708166747584,6,4,2,2",
		"s32;5;3;1;1000000;box-0,3,2,3,1,2,2|box-1,2,2,4,1,2,3|box-2,1,1,1,0,1,1 # 1054,1,1,1,1",
		"s33;6;3;2;1000000;box-0,2,1,2,0,3,1|box-1,1,1,1,1,2,1 # 29614176,2,5,5,2",
		"s34;6;5;2;2;box-0,3,2,4,1,2,3|box-1,2,3,3,2,3,3|box-2,5,2,3,1,1,1 # 0,0,0,0,0",
		"s35;3;5;3;1000000;box-0,2,2,2,1,3,2|box-1,1,2,2,0,1,1 # 881350320159,3,20,7,3",
		"s36;3;4;2;1000000;box-0,4,3,3,2,1,1|box-1,1,2,1,1,3,1|box-2,2,4,1,1,2,1|box-3,1,2,1,2,2,1 # 3860815641139881162,36,12,7,3",
		"s37;6;3;4;1000000;box-0,2,2,2,1,3,3|box-1,2,1,1,0,2,2 # 191238667880704956,10,28,13,5",
		"s38;5;4;4;2;box-0,5,1,5,2,3,3 # 0,0,0,0,0",
		"s39;6;5;1;1000000;box-0,1,1,1,1,3,2|box-1,1,1,1,2,2,3|box-2,1,1,1,1,1,1 # 827665037861983680,60,6,13,6",
		"s40;6;3;4;1000000;box-0,2,4,5,0,2,1|box-1,6,3,4,0,1,2|box-2,3,4,2,1,1,1|box-3,5,5,5,2,1,1|box-4,6,1,1,1,3,1 # -6635318499860317952,12,72,1,1",
		"s41;3;3;1;1000000;box-0,1,1,1,2,2,2 # 30752,1,2,4,2",
		"s42;3;5;3;1000000;box-0,4,3,1,2,1,3|box-1,1,3,2,0,2,3|box-2,5,2,5,2,1,1 # 6154640907882986496,160,36,3,3",
		"s43;4;5;3;1000000;box-0,2,2,2,1,2,1|box-1,1,1,1,1,1,3|box-2,2,1,2,1,1,2|box-3,2,1,2,2,2,1 # 1021501914828205184,5040,23,9,7"
	};

	private static ReferenceScenario parse(String line) {
		String[] parts = line.split(";", -1);
		ReferenceScenario scenario = new ReferenceScenario(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
		for (String spec : parts[5].split("\\|")) {
			String[] fields = spec.split(",");
			Rotation rotation = switch (Integer.parseInt(fields[4])) {
				case 0 -> NO_ROTATION;
				case 1 -> Rotation.TWO_D;
				default -> Rotation.THREE_D;
			};
			scenario.add(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), rotation, Integer.parseInt(fields[5]), Integer.parseInt(fields[6]));
		}
		return scenario;
	}

	@Test
	void matchesTheResultsOfTheFourXCode() throws PackagerInterruptedException {
		assertThat(ROWS).isNotEmpty();
		for (String row : ROWS) {
			String[] parts = row.split(" # ");
			ReferenceScenario scenario = parse(parts[0]);
			String[] expected = parts[1].split(",");
			String message = row;

			// the enumeration
			DefaultBoxItemPermutationRotationIterator iterator = scenario.newVersion4Iterator();
			long hash = 0;
			long states = 0;
			if(iterator.length() > 0) {
				do {
					do {
						PermutationRotationState state = iterator.getState();
						hash = hash * 31 + Arrays.hashCode(state.getPermutations());
						hash = hash * 31 + Arrays.hashCode(state.getRotations());
						states++;
					} while (iterator.nextRotation() != -1);
				} while (iterator.nextPermutation() != -1);
			}
			assertThat(hash).as(message).isEqualTo(Long.parseLong(expected[0]));
			assertThat(states).as(message).isEqualTo(Long.parseLong(expected[1]));

			// the search
			Version4Reference.Result result = Version4Reference.pack(scenario.newVersion4Container(), scenario.newVersion4BoxItems(), Version4Reference.interruptAfter(20_000));
			assertThat(result.loadVolume()).as(message).isEqualTo(Long.parseLong(expected[2]));
			assertThat(result.loadWeight()).as(message).isEqualTo(Long.parseLong(expected[3]));
			assertThat(result.boxCount()).as(message).isEqualTo(Integer.parseInt(expected[4]));
		}
	}

}
