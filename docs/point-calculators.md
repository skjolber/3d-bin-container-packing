# Point calculators

`PointCalculator` tracks placements and the free points available for the next box. Ordinary `add(..)`
updates both. When placing the final box, use either terminal overload:

```java
Point point = calculator.get(0);
calculator.addLast(point, placement);
// Or: calculator.addLast(0, placement);
List<Placement> result = new ArrayList<>(calculator.getPlacements());
long volume = calculator.calculateUsedVolume();
long weight = calculator.calculateUsedWeight();
calculator.clear();
```

The placement must fit within the selected point, as with `add(..)`. Terminal insertion includes the placement
in history and used volume/weight. The built-in 1D, 2D and 3D calculators skip free-space calculation.
Free-point queries are unspecified after `addLast(..)`; clear the calculator or restore a saved search state
before using those points or adding another placement. Mark/reset calculators can restore a state saved
before terminal insertion.

The interface provides default methods which delegate to ordinary `add(..)`, so existing custom calculators
remain compatible. They can override the methods to skip free-space calculation under the same contract.
