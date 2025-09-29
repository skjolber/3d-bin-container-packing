![Build Status](https://github.com/skjolber/3d-bin-container-packing/actions/workflows/maven.yml/badge.svg) 
[![Maven Central](https://img.shields.io/maven-central/v/com.github.skjolber.3d-bin-container-packing/parent.svg)](https://mvnrepository.com/artifact/com.github.skjolber.3d-bin-container-packing)

# 3d-bin-container-packing

This library does 3D rectangular bin packing; it attempts to match a set of 3D items to one or more in a set of 3D containers. The result can be constrained to a maximum number of containers.

Projects using this library will benefit from:
 * short and predictable calculation time,
 * fairly good use of container space, 
 * brute-force support for low number of boxes (ideal for small orders)
    
Bugs, feature suggestions and help requests can be filed with the [issue-tracker].

## Obtain
The project is implemented in Java and built using [Maven]. The project is available on the central Maven repository.

<details>
  <summary>Maven coordinates</summary>

Add
 
```xml
<3d-bin-container-packing.version>3.0.9</3d-bin-container-packing.version>
```

and

```xml
<dependency>
    <groupId>com.github.skjolber.3d-bin-container-packing</groupId>
    <artifactId>core</artifactId>
    <version>${3d-bin-container-packing.version}</version>
</dependency>
```

</details>

or

<details>
  <summary>Gradle coordinates</summary>

For

```groovy
ext {
  containerBinPackingVersion = '3.0.9'
}
```

add

```groovy
api("com.github.skjolber.3d-bin-container-packing:core:${containerBinPackingVersion}")
```

</details>

Java 11+ projects please use module `com.github.skjolber.packing.core`.

# Usage
The units of measure is out-of-scope, be they cm, mm or inches.

Obtain a `Packager` instance, then then compose your container and product list:

```java
List<BoxItem> products = new ArrayList<>();

products.add(new BoxItem(Box.newBuilder().withId("Foot").withSize(6, 10, 2).withRotate3D().withWeight(25).build(), 1));
products.add(new BoxItem(Box.newBuilder().withId("Leg").withSize(4, 10, 1).withRotate3D().withWeight(25).build(), 1));
products.add(new BoxItem(Box.newBuilder().withId("Arm").withSize(4, 10, 2).withRotate3D().withWeight(50).build(), 1));

// add a single container type
Container container = Container.newBuilder()
    .withDescription("1")
    .withSize(10, 10, 3)
    .withEmptyWeight(1)
    .withMaxLoadWeight(100)
    .build();
    
// with unlimited number of containers available
List<ContainerItem> containerItems = ContainerItem
    .newListBuilder()
    .withContainer(container)
    .build();
```

Pack all in a single container:

```java
PackagerResult result = packager
    .newResultBuilder()
    .withContainerItems(containerItems)
    .withBoxItems(products)
    .build();

if(result.isSuccess()) {
    Container match = result.get(0);
    
    // ...
}
```

Pack all in a maximum number of containers:

```java
int maxContainers = ...; // maximum number of containers which can be used

PackagerResult result = packager
    .newResultBuilder()
    .withContainers(containerItems)
    .withStackables(products)
    .withMaxContainerCount(maxContainers)
    .build();
```

Note that all `packager` instances are thread-safe.

### Plain packager
A simple packager

```java
PlainPackager packager = PlainPackager
    .newBuilder()
    .build();
```

### Largest Area Fit First (LAFF) packager
A packager using the LAFF algorithm

```java
LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager
    .newBuilder()
    .build();
```

### Brute-force packager
For a low number of packages (like <= 6) the brute force packager might be a good fit. 

```java
Packager packager = BruteForcePackager
    .newBuilder()
    .build();
```

Using a deadline is recommended whenever brute-forcing in a real-time application.

<details>
  <summary>Algorithm details</summary>
 
### Largest Area Fit First algorithm
The implementation is based on [this paper][2], and is not a traditional [bin packing problem][1] solver.

The box which covers the largest ground area of the container is placed first; its height becomes the level height. Boxes which fill the full remaining height take priority. Subsequent boxes are stacked in the remaining space in at the same level, the boxes with the greatest volume first. If box height is lower than level height, the algorithm attempts to place some there as well. 

When no more boxes fit in a level, the level is incremented and the process repeated. Boxes are rotated, containers not.

 * `LargestAreaFitFirstPackager` stacks in 3D within each level
 * `FastLargestAreaFitFirstPackager` stacks in 2D within each level

The algorithm runs reasonably fast, usually in milliseconds. Some customization is possible.

### Plain algorithm
This algorithm selects the box with the biggest volume, fitting it where it is best supported.

###  Brute-force algorithm
This algorithm has no logic for selecting the best box or rotation; running through all permutations, for each permutation all rotations:

 * `BruteForcePackager` attempts all box orders, rotations and placement positions.
 * `FastLargestAreaFitFirstPackager` selects all box orders and rotations, selecting the most appropriate placement position.

The complexity of this approach is [exponential], and thus there is a limit to the feasible number of boxes which can be packaged within a reasonable time. However, for real-life applications,  a healthy part of for example online shopping orders are within its grasp.

The worst case complexity can be estimated using the `DefaultPermutationRotationIterator` before packaging is attempted.

The algorithm tries to skip combinations which will obviously not yield a (better) result:

 * permutations
   * two or more boxes have the same dimensions
   * permutations which mutated at a previously unreachable index
 * fewer rotations
   * two or more sides have the same length
   * rotations which mutated at a previously unreachable index
 
There is also a parallel version `ParallelBruteForcePackager` of the brute-force packager, for those wishing to use it on a multi-core system.

Note that the algorithm is recursive on the number of boxes, so do not attempt this with many boxes (it will likely not complete in time anyhow).

</details> 
 
### Visualizer
There is a simple output [visualizer](visualization) included in this project, based of [three.js](https://threejs.org/). This visualizer is currently intended as a tool for developing better algorithms (not as stacking instructions).

![Alt text](visualizer/viewer/images/view.png?raw=true "Demo")

To use the visualizer during development, make your unit tests write directly to a file in the project (see `VisualizationTest` example). 

# Customization
The code has been structured so it is possible to extend and adapt to specialized needs. See `AbstractPackager` class, the `extreme-points` and `test` artifacts. 

# Get involved
If you have any questions, comments or improvement suggestions, please file an issue or submit a pull-request. 

Note on bugs: Please follow [shuairan's](https://github.com/shuairan) example and [file a test case with a visualization](https://github.com/skjolber/3d-bin-container-packing/issues/574).

Feel free to connect with me on [LinkedIn], see also my [Github page].

## License
[Apache 2.0]. Social media preview by [pch.vector on www.freepik.com](https://www.freepik.com/free-photos-vectors/people).

# Interesting links

 * [The Art of Stacking: Challenges Faced While Developing a Packing Algorithm](https://medium.com/@fayyazawais1412/the-art-of-stacking-challenges-faced-while-developing-a-packing-algorithm-64d869b924ab)

# History
 * 4.0.0: Major rewrite. 
     * Support for packaging groups
     * Various ways to control packaging:
        * Manifest controls (box vs box, box vs container)
        * Point controls (points per box)
        * Placement controls (best box+point)
 * 3.0.11: Use `BigInteger` to sanity-check max volume / max weight, calculate real remaining max volume.
 * 3.0.10: Fix module info, bump dependencies.
 * 3.0.9: Fix point support bug which resulted in invalid packaging result
 * 3.0.8: Visualization fix
 * 3.0.4-3.0.6: Fix issue #689
 * 3.0.3: Fix module info
 * 3.0.2: Make Plain Packager prefer low z coordinate over supported area.
 * 3.0.1: Various performance improvements.
 * 3.0.0: Support max number of containers (i.e. per container type). Use builders from now on. Various optimizations.
 * 2.1.4: Fix issue #574
 * 2.1.3: Fix null-pointer
 * 2.1.2: Tidy up, i.e. remove warnings, nuke some dependencies.
 * 2.1.1: Improve free space calculation performance
 * 2.1.0: Improve brute force iterators, respect deadlines in brute for packagers.

[1]: 				https://en.wikipedia.org/wiki/Bin_packing_problem
[2]: 				https://www.drupal.org/files/An%20Efficient%20Algorithm%20for%203D%20Rectangular%20Box%20Packing.pdf
[Apache 2.0]: 		http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:	https://github.com/skjolber/3d-bin-container-packing/issues
[Maven]:			http://maven.apache.org/
[LinkedIn]:			http://lnkd.in/r7PWDz
[Github page]:		https://skjolber.github.io
[NothinRandom]:		https://github.com/NothinRandom
[exponential]:		https://en.wikipedia.org/wiki/Exponential_function

