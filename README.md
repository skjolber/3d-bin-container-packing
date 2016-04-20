[![Build Status](https://travis-ci.org/skjolber/3d-bin-container-packing.svg)](https://travis-ci.org/skjolber/3d-bin-container-packing)

3d-bin-container-packing
==================================

This library does 3D rectangular bin packing; it attempts to match a set of 3D items to __one__ in a set of 3D containers. The result is the __single__ container which can hold all the items; no attempt is made to subdivide the items into several containers. 

Projects using this library will benefit from:
 * short and predictable calculation time,
 * fairly good use of container space, and
 * intuitive use for a human 
 
So while the algorithm will not produce the theoretically optimal result (which is NP-hard), its reasonable simplicity means that in many cases it would be possible to stack the resulting container for a human without instructions.

In short, the library provides a service which is __usually good enough, in time and reasonably user-friendly__ ;-)

Bugs, feature suggestions and help requests can be filed with the [issue-tracker].


## Obtain
The project is implemented in Java and built using [Maven]. The project is not yet available on the central Maven repository.

Example dependency config:

```xml
<dependency>
    <groupId>com.skjolberg</groupId>
    <artifactId>3d-bin-container-packing</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```


# Usage

### Initialization
Obtain a `Packager` instance:

    // initialization
    List<Dimension> containers = new ArrayList<Dimension>();
    containers.add(Dimension.newInstance(10, 10, 3)); // your container dimensions here
    Packager packager = new Packager(containers);

The `packager` instance is thread-safe.
### Packing
Then compose your item list and perform packing:

	List<Box> products = new ArrayList<Box>();
	products.add(new Box("Foot", 6, 10, 2));
	products.add(new Box("Leg", 4, 10, 1));
	products.add(new Box("Arm", 4, 10, 2));
	
    // match to container
	Container match = packager.pack(products);

The resulting `match` variable returning the resulting packaging details or null if no match. 

The above example would return a match (Foot and Arm would be packaged at the height 0, Leg at height 2).

## Details - Largest Area Fit First (LAFF)
----------
The implementation is based on [this paper][2], and is not a traditional [Bin packing problem][1] solver.

The box which covers the largest ground area of the container is placed first. Subsequent boxes are stacked in the remaining space in at the same level, the boxes with the greatest volume first. Then level is increased and the process repeated. Boxes are rotated, containers not.

The units of measure is out-of-scope, be they cm, mm or inches.

# Contact
If you have any questions or comments, please email me at thomas.skjolberg@gmail.com.

Feel free to connect with me on [LinkedIn], see also my [Github page].

## License
[Apache 2.0]

# History
 - [1.0.0]: Initial release.

[1]: https://en.wikipedia.org/wiki/Bin_packing_problem
[2]: http://www.zahidgurbuz.com/yayinlar/An%20Efficient%20Algorithm%20for%203D%20Rectangular%20Box%20Packing.pdf
[Apache 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:       https://github.com/skjolber/3d-bin-container-packing/issues
[Maven]:               	http://maven.apache.org/
[LinkedIn]: http://lnkd.in/r7PWDz
[Github page]: https://skjolber.github.io
