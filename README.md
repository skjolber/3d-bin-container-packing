[![Build Status](https://travis-ci.org/skjolber/3d-bin-container-packing.svg)](https://travis-ci.org/skjolber/3d-bin-container-packing)

# 3d-bin-container-packing

This library does 3D rectangular bin packing; it attempts to match a set of 3D items to __one__ in a set of 3D containers. The result is the __single__ container which can hold all the items; no attempt is made to subdivide the items into several containers. 

Projects using this library will benefit from:
 * short and predictable calculation time,
 * fairly good use of container space, and
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
    <version>1.0.6</version>
</dependency>
```

# Usage
The units of measure is out-of-scope, be they cm, mm or inches.

### Largest Area Fit First (LAFF) packager
Obtain a `Packager` instance:

```java
// initialization
List<Dimension> containers = new ArrayList<Dimension>();
containers.add(Dimension.newInstance(10, 10, 3)); // your container dimensions here
Packager packager = new LargestAreaFitFirstPackager(containers);
```

The `packager` instance is thread-safe.

### Packing
Then compose your item list and perform packing:

```java
List<BoxItem> products = new ArrayList<BoxItem>();
products.add(new BoxItem(new Box("Foot", 6, 10, 2), 1));
products.add(new BoxItem(new Box("Leg", 4, 10, 1), 1));
products.add(new BoxItem(new Box("Arm", 4, 10, 2), 1));
	
// match to container
Container match = packager.pack(products);
```

The resulting `match` variable returning the resulting packaging details or null if no match. 

The above example would return a match (Foot and Arm would be packaged at the height 0, Leg at height 2).

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

The maximum complexity this approach is [exponential] at __n! * 6^n__. The algorithm runs for under a second for small number of products (<= 6), to seconds or minutes (<= 8) or hours for larger numbers.

However accounting for container vs box size plus boxes with equal size might reduce this bound considerably, and the resulting complexity can be calculated using [PermutationRotationIterator](src/main/java/com/github/skjolberg/packing/PermutationRotationIterator.java) before packaging is attempted. See [example] in test sources.

# Contact
If you have any questions or comments, please email me at thomas.skjolberg@gmail.com.

Feel free to connect with me on [LinkedIn], see also my [Github page].

## License
[Apache 2.0]

# History
 - [1.0.6]: Better support for multiple instances of the same box
 - 1.0.5: Binary search approach for packaging with deadline
 - 1.0.4: Add deadline and brute force packager. 
 - 1.0.3: Fix for issue #5, minor cleanup. 
 - 1.0.2: Fix for issue #4, minor improvements. 
 - 1.0.1: Add option to toggle 2D and 3D rotation and box placement coordinates, compliments of [NothinRandom]. 
 - 1.0.0: Initial release.

[1]: 					https://en.wikipedia.org/wiki/Bin_packing_problem
[2]: 					http://www.zahidgurbuz.com/yayinlar/An%20Efficient%20Algorithm%20for%203D%20Rectangular%20Box%20Packing.pdf
[Apache 2.0]: 			http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:		https://github.com/skjolber/3d-bin-container-packing/issues
[Maven]:				http://maven.apache.org/
[LinkedIn]:				http://lnkd.in/r7PWDz
[Github page]:			https://skjolber.github.io
[1.0.6]:				https://github.com/skjolber/3d-bin-container-packing/releases
[NothinRandom]:		https://github.com/NothinRandom
[exponential]:			https://en.wikipedia.org/wiki/Exponential_function
[example]:				src/test/java/com/github/skjolberg/packing/BruteForcePackagerRuntimeEstimator.java
