# Deliveries: extraction order and container priority

Two settings on box items (and box item groups) say when boxes leave, as opposed to the box item order (`Order`, see
`withOrder(..)` on the result builder), which is the order in which boxes arrive for loading:

```java
// the stops of a delivery route: lower values are extracted first
BoxItem firstStop = new BoxItem(box, 2).withExtractionOrder(1);
BoxItem lastStop = new BoxItem(otherBox, 4).withExtractionOrder(3);

// urgent boxes in the first containers: lower values in earlier containers
BoxItem urgent = new BoxItem(box, 1).withContainerPriority(0);
BoxItem later = new BoxItem(otherBox, 1).withContainerPriority(1);

// for all the boxes of a group
BoxItemGroup group = new BoxItemGroup("order-1", items).withExtractionOrder(2);
```

 * **Extraction order**: within a container, no box rests on, or is in the path of (see `withAccess(..)`), a box
   which is extracted earlier, so the boxes of each stop can be taken out without moving the boxes for later stops.
   Boxes with the same extraction order (by default 0) are not constrained among themselves. The packagers place the
   boxes which are extracted last first, and the placements of each container are in insertion order: the boxes
   extracted last are inserted first.
 * **Container priority**: a hard constraint on which boxes go in earlier containers. A box with a lower value is never
   in a later container than a box with a higher value: the boxes of the next priority start in a container only after
   all the boxes of the priority before it are placed, in that container or an earlier one. With a box item order,
   the priorities must not decrease in that order.

## Packager support

The plain, LAFF and brute-force packagers support both, also for groups. Without a box item order, groups go into
containers lowest container priority first, then the groups which are extracted last, then the largest
(`withBoxItemGroupComparator(..)`). The brute-force packagers pack groups in an order, permuting the boxes within each
group, and a container holds the first groups which fit; they try every order of the remaining groups for each
container (groups of the same container priority change places), skipping the orders which cannot give a better result.
Each order is searched with the best result so far, so that the permutations which cannot load more are pruned. This is
exponential in the number of groups; use a deadline.
Virtual-box preprocessing packs boxes with either setting directly. `DefaultValidator` checks both
(`ExtractionOrderValidator`, `ContainerPriorityValidator`).

## See also

 * [Insertion order](insertion-order.md): the order in which the boxes of a container can be loaded, and the container's access.
 * [Brute-force packager heuristics](heuristics/brute-force.md): the search of box item groups.
 * [Virtual-box preprocessing](heuristics/virtual-box.md).
