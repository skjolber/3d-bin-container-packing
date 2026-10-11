# Visualizer

There is a simple output [visualizer](../visualizer) included in this project, based of [three.js](https://threejs.org/).

This visualizer is currently intended as a tool for developing better algorithms; not as stacking instructions.

## Setup
```
cd visualizer/viewer
npm install
```

## Run
```
npm start
```

The viewer shows `visualizer/viewer/public/assets/containers.json`, and reloads it when it changes. Write it with
`DefaultPackagingResultVisualizerFactory` (module `visualizer/packaging`), for one result or several results of the same order:

```java
Map<String, PackagerResult> results = new LinkedHashMap<>();
results.put("plain", plainResult);
results.put("composite", compositeResult);
new DefaultPackagingResultVisualizerFactory(true) // true: calculate the free points after each placement
    .visualize(results, validator.newResultBuilder().withContainerItems(containers).withBoxItems(boxItems), file);
```

The viewer shows:

 * a summary of the result: whether it was packed, the time and cost, and the volume and weight used per container
 * a comparison table when there are several results (`r` or click a row to switch)
 * whether the result is valid: the factory validates the boxes' load limits (and the whole result, given the input), logs the reasons, and the viewer outlines the boxes of invalid placements in red
 * colour modes (`c`): box item, group, support, load relative to the max load weight, and extraction order
 * the container's opening (orange), and boxes which are not in a possible insertion order
 * each container's centre of gravity, and for each box its supported area and load
 * the packing steps (`a` / `d`) and the free points after each placement (`p`, `w` / `s`)

## Hot reload and examples

To "hot reload" the visualizer during development, make your unit tests write that file. The `*VisualizationTest`
classes in `visualizer/packaging` are examples (load limits, insertion order, deliveries, groups and support, container
costs, virtual boxes, and comparing packagers); they are run by hand, for example from the IDE or with
`./mvnw -B -ntp -Pdev -pl visualizer/packaging -am -Dtest=PackagerComparisonVisualizationTest -Dsurefire.failIfNoSpecifiedTests=false test`.

For a tour of the features, run `ShowcaseVisualizationTest` while the viewer is open: it writes one scenario after
another, with a delay between them (`-Dshowcase.delay=20` seconds, `-Dshowcase.rounds=3`), and prints what to look
at. The viewer fits the camera when the containers change size.

![Alt text](../visualizer/viewer/images/view.png?raw=true "Demo")
