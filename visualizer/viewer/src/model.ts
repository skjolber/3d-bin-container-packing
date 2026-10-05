// Data model of the viewer, parsed from the JSON written by the Java DefaultPackagingResultVisualizerFactory.
// Kept free of three.js imports, so that it can be unit tested.

export class Point {
    
    x : number;
    y : number;
    z : number;
    
    dx : number;
    dy : number;
    dz : number;

    constructor(x : number, y : number, z: number, dx : number, dy : number, dz: number) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }
}

export class Stackable {

    dx : number;
    dy : number;
    dz : number;

    name: string;
    id: string;

    step : number;
    
    constructor(name : string, id : string, step: number, dx : number, dy : number, dz: number) {
        this.name = name;
        this.id = id;
        this.step = step;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

}

export class Box extends Stackable {

    boxItemKey?: number;
    /** Id of the box item group, if any. */
    groupId?: string;
    weight: number;
    maxLoadWeight?: number;
    maxLoadPressure?: number;
    maxLoadBoxCount?: number;
    maxLoadIdenticalOnly?: boolean;

    constructor(name : string, id : string, step: number, dx : number, dy : number, dz: number,
                weight: number,
                maxLoadWeight?: number, maxLoadPressure?: number,
                maxLoadBoxCount?: number, maxLoadIdenticalOnly?: boolean) {
        super(name, id, step, dx, dy, dz);
        this.weight = weight;
        this.maxLoadWeight = maxLoadWeight;
        this.maxLoadPressure = maxLoadPressure;
        this.maxLoadBoxCount = maxLoadBoxCount;
        this.maxLoadIdenticalOnly = maxLoadIdenticalOnly;
    }
    
}

export class Container extends Stackable {

    loadDx : number;
    loadDy : number;
    loadDz : number;
    
    stack : Stack;

    /** How boxes get into the container: ANY, TOP or FRONT (a door at x = dx). */
    access : string = "ANY";

    /** Centre of gravity of the load, if it weighs anything. */
    centerOfGravity? : { x : number, y : number, z : number };

    emptyWeight : number = 0;
    maxLoadWeight : number = 0;
    loadWeight : number = 0;
    maxLoadVolume : number = 0;
    loadVolume : number = 0;

    constructor(name : string, id : string, step: number, dx : number, dy : number, dz: number, loadDx : number, loadDy : number, loadDz: number) {
        super(name, id, step, dx, dy, dz);

        this.loadDx = loadDx;
        this.loadDy = loadDy;
        this.loadDz = loadDz;

        this.stack = new Stack(step);
    }
    
    add(stackPlacement : StackPlacement) {
        this.stack.add(stackPlacement);
    }
}

export class StackPlacement {

    stackable : Stackable;
    x : number;
    y : number;
    z : number;

    step : number;

    points : Array<Point>;

    /** Area resting on boxes which count as support (the floor is not counted). */
    supportedArea : number = 0;
    /** Total weight resting on the box, as calculated by the validators. */
    loadWeight : number = 0;

    /** The validation reasons which concern this placement; empty if valid. */
    reasons : Array<ValidationReason> = [];

    constructor(stackable : Stackable, step : number, x : number, y : number, z: number, points: Array<Point>) {
        this.stackable = stackable;
        this.step = step;
        this.x = x;
        this.y = y;
        this.z = z;
        this.points = points;
    }

}

export class Stack {

    placements : Array<StackPlacement>;

    step : number;

    constructor(step : number) {
        this.step = step;
        this.placements = new Array();
    }

    add(placement : StackPlacement) {
        this.placements.push(placement);
    }

}

/** Placement within the result: index of the container, and of the placement in the container's stack. */
export interface PlacementReference {
    container : number;
    placement : number;
}

/** A reason why the result is invalid. */
export interface ValidationReason {
    /** Reason class name, for example ExcessiveLoadWeightReason */
    type : string;
    code : number;
    message : string;
    placements : Array<PlacementReference>;
}

/** A parsed result: the containers, and the step and point ranges for navigation. */
export interface Packaging {
    /** Name, for example the packager, when comparing results. */
    name? : string;
    containers : Array<Container>;
    /** Lowest step of any container or box, or -1 if none. */
    minStep : number;
    /** Highest step of any container or box, or -1 if none. */
    maxStep : number;
    /** Highest number of points after a box, by the box's step. */
    maxPointNumbers : Array<number>;
    /** Whether packaging succeeded, or undefined if only containers were visualized. */
    success? : boolean;
    timeout? : boolean;
    /** Packaging duration in milliseconds. */
    duration? : number;
    /** Total container cost, or -1 if not calculated. */
    cost? : number;
    /** Whether the result passed validation; invalid results are still shown. */
    valid : boolean;
    validationReasons : Array<ValidationReason>;
}

/**
 * Parse the JSON written by the Java DefaultPackagingResultVisualizerFactory: one or more results.
 */
export function parsePackagings(json : any) : Array<Packaging> {
    var results = new Array<Packaging>();
    for (const result of json.results ?? []) {
        results.push(parsePackaging(result));
    }
    return results;
}

/**
 * Parse one result.
 */
export function parsePackaging(json : any) : Packaging {
    var minStep = -1;
    var maxStep = -1;
    var maxPointNumbers = new Array<number>();
    var containers = new Array<Container>();
    var validationReasons : Array<ValidationReason> = json.validationReasons ?? [];

    for (const containerJson of json.containers) {
        var container = new Container(containerJson.name, containerJson.id, containerJson.step,
            containerJson.dx, containerJson.dy, containerJson.dz,
            containerJson.loadDx, containerJson.loadDy, containerJson.loadDz);
        container.emptyWeight = containerJson.emptyWeight;
        container.maxLoadWeight = containerJson.maxLoadWeight;
        container.loadWeight = containerJson.loadWeight;
        container.maxLoadVolume = containerJson.maxLoadVolume;
        container.loadVolume = containerJson.loadVolume;
        container.access = containerJson.access ?? "ANY";
        if(containerJson.centerOfGravityX != null) {
            container.centerOfGravity = { x: containerJson.centerOfGravityX, y: containerJson.centerOfGravityY, z: containerJson.centerOfGravityZ };
        }

        if(container.step < minStep || minStep == -1) {
            minStep = container.step;
        }
        if(container.step > maxStep || maxStep == -1) {
            maxStep = container.step;
        }

        for (const placement of containerJson.stack.placements) {
            var stackable = placement.stackable;

            if(stackable.step < minStep || minStep == -1) {
                minStep = stackable.step;
            }
            if(stackable.step > maxStep || maxStep == -1) {
                maxStep = stackable.step;
            }

            var points = new Array<Point>();
            for (const point of placement.points) {
                points.push(new Point(point.x, point.y, point.z, point.dx, point.dy, point.dz));
            }

            if(maxPointNumbers[stackable.step] == null || maxPointNumbers[stackable.step] < points.length) {
                maxPointNumbers[stackable.step] = points.length;
            }

            if(stackable.type == "box") {
                var box = new Box(
                    stackable.name, stackable.id, stackable.step,
                    stackable.dx, stackable.dy, stackable.dz,
                    stackable.weight || 0,
                    stackable.maxLoadWeight, stackable.maxLoadPressure,
                    stackable.maxLoadBoxCount, stackable.maxLoadIdenticalOnly
                );
                box.boxItemKey = stackable.boxItemKey;
                box.groupId = stackable.groupId ?? undefined;

                var stackPlacement = new StackPlacement(box, placement.step, placement.x, placement.y, placement.z, points);
                stackPlacement.supportedArea = placement.supportedArea ?? 0;
                stackPlacement.loadWeight = placement.loadWeight ?? 0;
                for (const reasonIndex of placement.reasons ?? []) {
                    stackPlacement.reasons.push(validationReasons[reasonIndex]);
                }
                container.add(stackPlacement);
            }
        }
        containers.push(container);
    }
    return {
        name: json.name ?? undefined,
        containers, minStep, maxStep, maxPointNumbers,
        success: json.success ?? undefined,
        timeout: json.timeout ?? undefined,
        duration: json.duration ?? undefined,
        cost: json.cost ?? undefined,
        valid: json.valid ?? true,
        validationReasons
    };
}
