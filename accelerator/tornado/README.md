# tornado-accelerator

Experimental [TornadoVM](https://www.tornadovm.org/) accelerator for `FastBruteForcePackager` (see [accelerated brute force](../../docs/heuristics/accelerated-brute-force.md)):
the search of the box orders of a container runs on a GPU (or another OpenCL, CUDA or Metal device of TornadoVM), and the packager places the winning order itself, so
the placements are the same as without the accelerator.

It needs Java 22+, and is only built with the `tornado` profile; the default build does not contain it:

```sh
./mvnw -B -ntp -Ptornado,dev -pl accelerator/tornado -am test
```

```java
TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().build();
FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withAccelerator(accelerator).build();
...
packager.close();
accelerator.close(); // releases the device memory and the compiled kernel
```

The accelerator is only used if it is passed to the packager. Without a TornadoVM runtime and device it gives up, and the packager searches as it does without one; the same
when a search needs more free points than `withMaxPoints(..)` (the layout of the scratch is fixed up front), or when the device fails (`getLastFailure()`). One search runs on the
device at a time; searches from other threads wait.

## How it works

 * `DeviceSelector`, `DeviceInfo` and `TornadoDevices`: find the devices of the TornadoVM runtime and choose one, by default the OpenCL GPU with the most compute throughput
   (compute units times clock, then the most memory: an integrated GPU shares the memory of the system, so it can report more than the discrete card). Without a runtime the list is
   empty. Exclude a device with `withExcludedName(..)`, or choose by name with `withNameContains(..)`.
 * `SubtreeKernel` and `src/main/resources/.../subtree.cl`: the kernel. One work item runs `FlatSubtree.run(..)` for one slot of a window, with its own scratch, for at most
   `withNodeBudget(..)` search nodes, then pauses. It is a hand-written OpenCL C port of the flat package of the core module (`FlatGeometry`, `FlatPointOrder`, `FlatPoints`,
   `FlatPermutation`, `FlatSearch`, `FlatSubtree`), function by function with the same names and statements, run as a TornadoVM *prebuilt* task. See below for why it is not
   compiled by the TornadoVM JIT. A change to the flat package is made in the kernel too; `KernelParityTest` checks that the two agree.
 * `TornadoBruteForceAccelerator`: the host loop of `FlatEngine` on the device. The permutations are split in items by their first boxes (`withTargetItems(..)` items at least);
   a window of `withWindow(..)` items runs at a time, each item in a slot with its own scratch on the device (`withMaxPoints(..)` free points at each level). After each run
   the host reads the state of the slots, polls the interrupt, and runs the next round with the items which are not done; the winners of the items are reduced as in `FlatEngine`.
   The compiled kernel and the device memory are kept between searches of the same shape (number of boxes and sizes of the problem arrays), so that only the first search of a
   packing pays for compilation and allocation.
 * `ListDevices`: a `main` which prints the devices and the chosen one, as a smoke test of a TornadoVM installation.

The dependency on `tornado-api` is `provided`: the API jar has no native drivers. The runtime is the TornadoVM SDK, which is not on Maven Central.

## Running against a device

Download the SDK for your backend and JDK from the [TornadoVM releases](https://github.com/beehive-lab/TornadoVM/releases), for example `tornadovm-7.3.1-jdk22plus-opencl-linux-amd64.tar.gz`, and unpack it
(for example into `accelerator/tornado/target/tornadovm-sdk`, which `mvn clean` removes). The OpenCL backend has no native library of its own: it calls the system OpenCL loader
(`libOpenCL.so`) and so the OpenCL driver of the GPU (on AMD, the ROCm OpenCL runtime). The SDK contains `tornado-argfile.template`, the JVM options (JVMCI, module path, ...) in which `${TORNADOVM_HOME}`
is the unpacked directory:

```sh
SDK=$PWD/accelerator/tornado/target/tornadovm-sdk/tornadovm-7.3.1-opencl
T=$PWD/accelerator/tornado/target
sed "s|\${TORNADOVM_HOME}|$SDK|g" $SDK/tornado-argfile.template > $T/tornado.args
```

The tests which need a device (`KernelParityTest`, and the device part of `TornadoBruteForceAcceleratorTest`) are skipped unless the JVM options are given:

```sh
./mvnw -B -ntp -Ptornado,dev -pl accelerator/tornado -am test -Dtornado.argfile=$T/tornado.args
```

To run a class by hand:

```sh
CP=$T/classes:$T/test-classes:core/target/classes:api/target/classes
mkdir -p $T/run $T/home/.cache $T/tmp && cd $T/run   # the module path of the SDK contains '.', so run in a directory without jars
HOME=$T/home XDG_CACHE_HOME=$T/home/.cache TMPDIR=$T/tmp java @$T/tornado.args -Djava.io.tmpdir=$T/tmp -cp $CP com.github.skjolber.packing.tornado.ListDevices
```

The environment variables keep the caches of the GPU stack within the build directory (the AMD compiler cache, `comgr`, is the only thing it writes).

 * `GpuSmoke` (in the test sources): chooses the device, runs a vector add, and a kernel with data-dependent loops, `long` arithmetic and scratch memory per work item,
   and compares both with the same code run on the CPU.
 * `PrebuiltProbe`: checks the calling convention of a prebuilt kernel (the order of the parameters, the 16-byte header of a Java array on the device, the global work size).
   Run it after an upgrade of TornadoVM, before the kernel.
 * `KernelParity`: runs the accelerator on random problems and compares the winner with `FlatEngine`, printing the times. Arguments: `firstSeed seeds minBoxes maxBoxes window
   nodeBudget maxPoints targetItems cpuThreads repeat`, for example `KernelParity 1 20 8 10`. Give every run a `timeout`: a hung kernel never returns.
 * `StageProbe` and `ArrayProbe`: probes of the TornadoVM JIT path (below), kept for retrying it with a newer TornadoVM.

Verified: the 7.3.1 `jdk22plus` OpenCL SDK on JDK 25, with the ROCm 7.1 OpenCL runtime on a Radeon RX 7600 (`gfx1102`, OpenCL 2.0 device); the integrated GPU (`gfx1036`) is listed as a second device.

## Speed

Measured with `KernelParity` on a Radeon RX 7600 (16 compute units, 8 GB) against `FlatEngine` with 8 threads on 4 cores of a Ryzen 9 9950X (the CPU engine does the
same work per node as the packager's own loop, so this is the packager with 8 threads), with a warm JVM, on 42 random problems of 11 to 12 boxes in tight containers, every
problem different. The device time includes the host loop, the transfers and, for each problem, about 0.4 s to compile the kernel and allocate the scratch; the compiled kernel
and the memory are reused only for a problem of the same shape (number of boxes and sizes of the problem arrays).

| Problems | 8 CPU threads | Device |
|---|---|---|
| 35 easy ones (the CPU under 0.5 s each) | 1.3 s in all | 16.3 s in all, 0.46 s each |
| seed 112, 7 million nodes | 0.54 s | 1.03 s |
| seed 115, 6 million nodes | 0.52 s | 0.81 s |
| seed 122, 9 million nodes | 1.53 s | 2.01 s |
| seed 128, 6 million nodes | 0.63 s | 0.93 s |
| seed 140, 5 million nodes | 0.63 s | 0.74 s |
| seed 109, 55 million nodes | 12.0 s | 26.5 s |
| seed 138, 126 million nodes | 19.1 s | 10.7 s |
| all 42 | 36.3 s | 59.0 s |

So the device is 1.6 times slower than 8 CPU threads over the whole set: the compilation makes the easy problems 12 times slower, the medium ones lose by 1.2 to 1.9 times, and
only the largest problem wins, by 1.8 times (seed 109, whose items are very uneven, loses by 2.2 times). Against the 32 threads of the whole CPU the device loses everywhere.
A second search of a problem of the same shape saves the 0.4 s, which does not change the picture for the medium problems. The target of the design (3 times the whole CPU)
is not reached, and the expectation of 0.5 to 2 times only for the largest problems.

Note on measuring: the CPU times must be taken with a warm JVM. A first search in a fresh JVM includes the JIT compilation of the search and is 2 to 6 times slower, which made
an earlier version of this table claim the opposite.

The defaults come from tuning runs on seeds 112 to 115:

 * `withLocalWorkSize(1)`: one work item per work group. The search diverges so much that the lanes of a wavefront mostly wait for each other; a work group of one
   was 2.5 times faster than the choice of the runtime (a work group of 32 was no better than that).
 * `withNodeBudget(1000)`: after each round the load volumes found so far bound the next, so a small budget means fewer nodes in all (seed 112: 6.9 million nodes against 21 million
   with a budget of 5000, and 47 million with 20,000); 500 was slightly faster still, 2000 slower.
 * `withWindow(2048)`: 1024 and 4096 were the same; 512 was 2.5 times slower (too few work items in flight). `withMaxPoints(64)` and `withTargetItems(65536)` made no difference.

What could still change the picture: compiling the kernel once per JVM instead of once per shape (the kernel does not depend on the sizes, only the plan does), and a layout of the
scratch which coalesces the memory accesses of a wavefront; neither has been tried.

## Why the kernel is written by hand

The flat package was written so that the TornadoVM JIT could compile `FlatSubtree.run(..)` from the Java (a `@Parallel` loop over the slots, see `StageProbe` for what is left of it). With
TornadoVM 7.3.1 (OpenCL) that did not work: the JIT compiles the search (36,000 lines of OpenCL, once its inlining limits are raised with `-Dgraal.MaximumInliningSize=20000
-Dgraal.MaximumDesiredSize=2000000`), but the kernel does not run correctly. Found by bisecting on the device, each of which turned a wrong result or a hang into a match with the host:

 * `Integer.compare` and `Long.compare` produce a node which the backend cannot lower (a JIT error); boolean predicates compile.
 * A `return` inside a loop gave wrong results (`-1` or `0` from `FlatPermutation.next` and `FlatSearch.searchPermutation`); a `continue` in a loop gave wrong results too.
 * With those rewritten, every function matched the host when run in a straight line, and a search of one box matched, but the second iteration of the search loop of
   `FlatSearch.searchPermutation` hangs the kernel, whatever the loop does in it; the host thread then blocks in `clEnqueueReadBuffer` for ever.

The JIT does not reject code it cannot lower: it compiles it, and the kernel then returns wrong results or never finishes. The prebuilt kernel is compiled by the OpenCL compiler of the
driver, takes about 400 ms to compile, and runs the search as written.
