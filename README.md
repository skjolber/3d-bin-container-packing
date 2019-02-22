[![Build Status](https://travis-ci.org/skjolber/3d-bin-container-packing.svg)](https://travis-ci.org/skjolber/3d-bin-container-packing)

# 3d-bin-container-packing

This library does 3D rectangular bin packing; it attempts to match a set of 3D items to one or more in a set of 3D containers. The result can be constrained to a maximum number of containers.

Projects using this library will benefit from:
 * short and predictable calculation time,
 * fairly good use of container space, 
 * brute-force support for low number of boxes, and
 * intuitive use for a human
 
So while the algorithm will not produce the theoretically optimal result (which is NP-hard), its reasonable simplicity means that in many cases it would be possible to stack the resulting container for a human without instructions.

In short, the library provides a service which is __usually good enough, in time and reasonably user-friendly__ ;-)

Bugs, feature suggestions and help requests can be filed with the [issue-tracker].
 
## Obtain
The project is implemented in Java and built using [Maven]. The project is available on the central Maven repository.

Example dependency config:

```xml
<dependency>
    <groupId>com.github.skjolber</groupId>
    <artifactId>3d-bin-container-packing</artifactId>
    <version>1.2.3</version>
</dependency>
```

# Usage
The units of measure is out-of-scope, be they cm, mm or inches.

### Largest Area Fit First (LAFF) packager
Obtain a `Packager` instance:

```java
// initialization
List<Container> containers = new ArrayList<Container>();
containers.add(new Container(10, 10, 3, 100)); // x y z and weight
Packager packager = new LargestAreaFitFirstPackager(containers);
```

The `packager` instance is thread-safe.

### Packing
Then compose your item list and perform packing:

```java
List<BoxItem> products = new ArrayList<BoxItem>();
products.add(new BoxItem(new Box("Foot", 6, 10, 2, 25), 1));
products.add(new BoxItem(new Box("Leg", 4, 10, 1, 25), 1));
products.add(new BoxItem(new Box("Arm", 4, 10, 2, 50), 1));
	
// match a single container
Container match = packager.pack(products);
```

The resulting `match` variable returning the resulting packaging details or null if no match. 

The above example would return a match (Foot and Arm would be packaged at the height 0, Leg at height 2). 

For matching against multiple containers use

```java
int maxContainers = ...; // maximum number of containers which can be used

// match multiple containers
List<Container> fits = packager.packList(products, maxContainers);
```

### Rotation
By adding an additional argument to the constructor, 2D or 3D rotation of boxes can be toggled:

```java
boolean rotate3d = ...;
Packager packager = new LargestAreaFitFirstPackager(containers, rotate3d, true, true);
```

### Brute-force packager
For a low number of packages (like <= 6) the brute force packager might be a good fit. 

```java
Packager packager = new BruteForcePackager(containers);
```

Using a deadline is recommended whenever brute-forcing in a real-time application.

```
// limit search using 5 seconds deadline
long deadline = System.currentTimeMillis() + 5000;

Container match = packager.pack(products, deadline);
```

## Details

### Largest Area Fit First algorithm
The implementation is based on [this paper][2], and is not a traditional [Bin packing problem][1] solver.

The box which covers the largest ground area of the container is placed first. Boxes which fill the full remaining height take priority. Subsequent boxes are stacked in the remaining space in at the same level, the boxes with the greatest volume first. Then level is increased and the process repeated. Boxes are rotated, containers not.

The algorithm runs reasonably fast, usually in milliseconds. 

###  Brute-force algorithm
This algorithm places the boxes in the same way as the LAFF algorithm, but has no logic for selecting the best box or rotation; running through all permutations, for each permutation all rotations. 

The maximum complexity of this approach is [exponential] at __n! * 6^n__. The algorithm runs for under a second for small number of products (<= 6), to seconds or minutes (<= 8) or hours for larger numbers.

However accounting for container vs box size plus boxes with equal size might reduce this bound considerably, and the resulting complexity can be calculated using [PermutationRotationIterator](src/main/java/com/github/skjolberg/packing/PermutationRotationIterator.java) before packaging is attempted. See [example] in test sources.

# Get involved
If you have any questions, comments or improvement suggestions, please file an issue or submit a pull-request.

Feel free to connect with me on [LinkedIn], see also my [Github page].

## License
[Apache 2.0]

# History
 - 1.2.3: Fix weight constraint (issue #83). Thanks to [BALACHANDAR S](https://github.com/balachandarsv) for bug reports and unit tests.
 - 1.2.2: ~~Fix~~ Improve weight constraint in LargestAreaFitFirstPackager (issue #83)
 - 1.2.1: Fix z coordinate propagation in free space (issue #78)
 - 1.2.0: Support for stopping / interrupting packaging, for use in multithreaded scenarios.
 - 1.1.0: Support for multi-container results. Thanks to [Michel Daviot](https://github.com/tyrcho) for QA.
 - 1.0.10: Fix isEmpty method.
 - 1.0.9: Support for calculating the used space bounding box within a container.
 - 1.0.8: Fix for issue #28 (Brute force packager)

[1]: 					https://en.wikipedia.org/wiki/Bin_packing_problem
[2]: 					http://www.zahidgurbuz.com/yayinlar/An%20Efficient%20Algorithm%20for%203D%20Rectangular%20Box%20Packing.pdf
[Apache 2.0]: 			http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:		https://github.com/skjolber/3d-bin-container-packing/issues
[Maven]:				http://maven.apache.org/
[LinkedIn]:				http://lnkd.in/r7PWDz
[Github page]:			https://skjolber.github.io
[1.1.0]:				https://github.com/skjolber/3d-bin-container-packing/releases
[NothinRandom]:			https://github.com/NothinRandom
[exponential]:			https://en.wikipedia.org/wiki/Exponential_function
[example]:				src/test/java/com/github/skjolberg/packing/BruteForcePackagerRuntimeEstimator.java
