package com.github.skjolber.packing.visualizer.packaging;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultBuilder;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.validator.ContainerPriorityValidator;
import com.github.skjolber.packing.validator.ExtractionOrderValidator;
import com.github.skjolber.packing.validator.GroupInsertionValidator;
import com.github.skjolber.packing.validator.InsertionOrderValidator;
import com.github.skjolber.packing.validator.SupportGraph;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadBoxCountReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadPressureReason;
import com.github.skjolber.packing.validator.load.reasons.ExcessiveLoadWeightReason;
import com.github.skjolber.packing.validator.load.reasons.NonIdenticalLoadBoxReason;
import com.github.skjolber.packing.validator.reasons.BlockedExtractionReason;
import com.github.skjolber.packing.validator.reasons.BlockedInsertionReason;
import com.github.skjolber.packing.validator.reasons.ContainerPriorityReason;
import com.github.skjolber.packing.validator.reasons.InterleavedGroupReason;
import com.github.skjolber.packing.validator.reasons.InsertedBeforeSupporterReason;
import com.github.skjolber.packing.validator.stability.reasons.InsufficientSupportAreaReason;
import com.github.skjolber.packing.validator.stability.reasons.UnstableCenterOfGravityReason;
import com.github.skjolber.packing.validator.stability.reasons.UnstableStackCenterOfGravityReason;
import com.github.skjolber.packing.visualizer.api.packaging.BoxVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.ContainerVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultsVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PlacementReferenceVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PointVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackPlacementVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.ValidationReasonVisualizer;

public class DefaultPackagingResultVisualizerFactory extends AbstractPackagingResultVisualizerFactory<Container> {

	private static final Logger LOGGER = Logger.getLogger(DefaultPackagingResultVisualizerFactory.class.getName());

	protected final boolean calculatePoints;

	public DefaultPackagingResultVisualizerFactory(boolean calculatePoints) {
		this.calculatePoints = calculatePoints;
	}
	
	/**
	 * Visualize a packager result: its containers, and whether it succeeded, its duration and cost.
	 * The load limits of the boxes are validated, see {@link #visualize(List)}.
	 */
	public PackagingResultVisualizer visualize(PackagerResult result) {
		return visualize(result, (ValidatorResultBuilder)null);
	}

	/**
	 * Visualize and validate a packager result. Invalid results are visualized too: the reasons are logged,
	 * the result is marked as invalid, and the reasons are added to the placements they concern.
	 *
	 * @param result the result
	 * @param validation a validator result builder with the packager input, for example
	 *        {@code validator.newResultBuilder().withContainerItems(..).withBoxItems(..).withMaxContainerCount(..)},
	 *        or null to validate only the boxes' load limits
	 * @return the visualization
	 */
	public PackagingResultVisualizer visualize(PackagerResult result, ValidatorResultBuilder validation) {
		Map<Placement, PlacementReferenceVisualizer> references = new IdentityHashMap<>();
		PackagingResultVisualizer visualization = visualize(result.getContainers(), references);
		visualization.setSuccess(result.isSuccess());
		visualization.setTimeout(result.isTimeout());
		visualization.setDuration(result.getDuration());
		visualization.setCost(result.getCost());
		visualization.setInsertionOrder(result.isInsertionOrder());

		if(validation != null) {
			ValidatorResult validatorResult = validation.withPackagerResult(result).build();
			if(validatorResult.isTimeout()) {
				addReason(visualization, references, ValidatorResultReason.class.getSimpleName(), -1, "Validation timed out", List.of());
			} else if(!validatorResult.isValid()) {
				if(validatorResult.getReasons().isEmpty()) {
					addReason(visualization, references, ValidatorResultReason.class.getSimpleName(), -1, "Invalid, without reason", List.of());
				}
				addReasons(visualization, references, validatorResult.getReasons());
			}
		}
		return visualization;
	}

	/**
	 * Visualize several results of the same order, for example from different packagers, to compare them in the viewer.
	 *
	 * @param results results by name, in the order to show them (for example a {@code LinkedHashMap})
	 * @param validation a validator result builder with the packager input, used for every result, or null to validate
	 *        only the boxes' load limits
	 * @return the visualization
	 */
	public PackagingResultsVisualizer visualize(Map<String, PackagerResult> results, ValidatorResultBuilder validation) {
		PackagingResultsVisualizer visualization = new PackagingResultsVisualizer();
		for (Map.Entry<String, PackagerResult> entry : results.entrySet()) {
			PackagingResultVisualizer result = visualize(entry.getValue(), validation);
			result.setName(entry.getKey());
			visualization.add(result);
		}
		return visualization;
	}

	public void visualize(Map<String, PackagerResult> results, ValidatorResultBuilder validation, File output) throws Exception {
		write(visualize(results, validation), output);
	}

	public void visualize(PackagerResult result, File output) throws Exception {
		write(visualize(result), output);
	}

	public void visualize(PackagerResult result, ValidatorResultBuilder validation, File output) throws Exception {
		write(visualize(result, validation), output);
	}

	/**
	 * Visualize containers, and validate their insertion order (see {@link com.github.skjolber.packing.api.ContainerAccess}),
	 * that the boxes of each group are inserted together, the extraction order and container priority of their boxes, and the load limits of their boxes (the load validators
	 * are chosen from the boxes' limits).
	 */
	public PackagingResultVisualizer visualize(List<Container> inputContainers) {
		return visualize(inputContainers, new IdentityHashMap<>());
	}

	protected PackagingResultVisualizer visualize(List<Container> inputContainers, Map<Placement, PlacementReferenceVisualizer> references) {
		boolean calculatePoints = this.calculatePoints;
		Map<Object, Integer> boxItemKeys = new IdentityHashMap<>();
		
		int step = 0;
		PackagingResultVisualizer visualization = new PackagingResultVisualizer();
		for (int containerIndex = 0; containerIndex < inputContainers.size(); containerIndex++) {
			Container inputContainer = inputContainers.get(containerIndex);
			ContainerVisualizer containerVisualization = new ContainerVisualizer();
			containerVisualization.setStep(step++);

			containerVisualization.setDx(inputContainer.getDx());
			containerVisualization.setDy(inputContainer.getDy());
			containerVisualization.setDz(inputContainer.getDz());

			containerVisualization.setLoadDx(inputContainer.getLoadDx());
			containerVisualization.setLoadDy(inputContainer.getLoadDy());
			containerVisualization.setLoadDz(inputContainer.getLoadDz());

			containerVisualization.setId(inputContainer.getId());
			containerVisualization.setName(inputContainer.getDescription());
			containerVisualization.setAccess(inputContainer.getAccess().name());
			for (Placement obstacle : inputContainer.getObstacles()) {
				PointVisualizer o = new PointVisualizer();
				o.setX(obstacle.getAbsoluteX());
				o.setY(obstacle.getAbsoluteY());
				o.setZ(obstacle.getAbsoluteZ());
				o.setDx(obstacle.getStackValue().getDx());
				o.setDy(obstacle.getStackValue().getDy());
				o.setDz(obstacle.getStackValue().getDz());
				containerVisualization.getObstacles().add(o);
			}
			containerVisualization.setEmptyWeight(inputContainer.getEmptyWeight());
			containerVisualization.setMaxLoadWeight(inputContainer.getMaxLoadWeight());
			containerVisualization.setLoadWeight(inputContainer.getLoadWeight());
			containerVisualization.setMaxLoadVolume(inputContainer.getMaxLoadVolume());
			containerVisualization.setLoadVolume(inputContainer.getLoadVolume());

			StackVisualizer stackVisualization = new StackVisualizer();
			stackVisualization.setStep(step++);
			containerVisualization.setStack(stackVisualization);

			Stack stack = inputContainer.getStack();
			SupportGraph supportGraph = new SupportGraph(stack.getPlacements());
			setCenterOfGravity(containerVisualization, stack.getPlacements());

			DefaultPointCalculator3D pointCalculator = new DefaultPointCalculator3D(true, stack.getPlacements().size());
			pointCalculator.clearToSize(inputContainer.getDx(), inputContainer.getDy(), inputContainer.getDz());
			
			List<Placement> placements = stack.getPlacements();
			for (int i = 0; i < placements.size(); i++) {
				Placement placement = placements.get(i);
				Box box = placement.getStackValue().getBox();
				BoxVisualizer boxVisualization = new BoxVisualizer();
				boxVisualization.setId(box.getId());
				boxVisualization.setName(box.getDescription());
				boxVisualization.setStep(step);

				BoxStackValue stackValue = placement.getStackValue();

				boxVisualization.setDx(stackValue.getDx());
				boxVisualization.setDy(stackValue.getDy());
				boxVisualization.setDz(stackValue.getDz());

				BoxItem boxItem = placement.getBoxItem();
				Object boxItemIdentity = boxItem != null ? boxItem : box;
				boxVisualization.setBoxItemKey(boxItemKeys.computeIfAbsent(boxItemIdentity, key -> boxItemKeys.size()));
				boxVisualization.setWeight(box.getWeight());
				if(boxItem != null && boxItem.getGroup() != null) {
					boxVisualization.setGroupId(boxItem.getGroup().getId());
				}
				if(boxItem != null && boxItem.getContainerPriority() != 0) {
					boxVisualization.setContainerPriority(boxItem.getContainerPriority());
				}
				if(boxItem != null && boxItem.getExtractionOrder() != 0) {
					boxVisualization.setExtractionOrder(boxItem.getExtractionOrder());
				}

				if(stackValue.isMaxLoadBoxCount()) {
					boxVisualization.setMaxLoadBoxCount(stackValue.getMaxLoadBoxCount());
				}
				if(stackValue.isMaxLoadWeight()) {
					boxVisualization.setMaxLoadWeight(stackValue.getMaxLoadWeight());
				}
				if(stackValue.isMaxLoadPressure()) {
					boxVisualization.setMaxLoadPressure(stackValue.getMaxLoadPressure());
				}
				if(stackValue.isLoadIdenticalBoxOnly()) {
					boxVisualization.setMaxLoadIdenticalOnly(stackValue.isLoadIdenticalBoxOnly());
				}
				
				StackPlacementVisualizer stackPlacement = new StackPlacementVisualizer();
				stackPlacement.setX(placement.getAbsoluteX());
				stackPlacement.setY(placement.getAbsoluteY());
				stackPlacement.setZ(placement.getAbsoluteZ());
				stackPlacement.setStackable(boxVisualization);
				stackPlacement.setStep(step);
				stackPlacement.setSupportedArea(supportGraph.getSupportedArea(placement));
				stackPlacement.setLoadWeight(supportGraph.getLoadWeight(placement));
				references.put(placement, new PlacementReferenceVisualizer(containerIndex, i));

				if(calculatePoints) {
					int pointIndex = pointCalculator.findPoint(placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ());
	
					if(pointIndex == -1) {
						LOGGER.info("Unable to find next point, disabling further calculation of points");
						
						calculatePoints = false;
					} else {
						pointCalculator.add(pointIndex, placement);
		
						for (Point point : pointCalculator.getAll()) {
							PointVisualizer p = new PointVisualizer();
		
							p.setX(point.getMinX());
							p.setY(point.getMinY());
							p.setZ(point.getMinZ());
		
							p.setDx(point.getMaxX() - point.getMinX() + 1);
							p.setDy(point.getMaxY() - point.getMinY() + 1);
							p.setDz(point.getMaxZ() - point.getMinZ() + 1);
		
							stackPlacement.add(p);
						}
					}
				}
				
				stackVisualization.add(stackPlacement);

				step++;
			}

			visualization.add(containerVisualization);
		}

		InsertionOrderValidator insertionOrderValidator = new InsertionOrderValidator();
		ExtractionOrderValidator extractionOrderValidator = new ExtractionOrderValidator();
		GroupInsertionValidator groupInsertionValidator = new GroupInsertionValidator();
		for (Container inputContainer : inputContainers) {
			List<Placement> placements = inputContainer.getStack().getPlacements();
			List<ValidatorResultReason> reasons = new ArrayList<>();
			// the placements must be in a possible insertion order, and extractable in their extraction order
			insertionOrderValidator.validate(inputContainer, reasons);
			extractionOrderValidator.validate(inputContainer, reasons);
			groupInsertionValidator.validate(inputContainer, reasons);
			LoadValidator loadValidator = new DefaultLoadValidatorBuilder().withContainer(inputContainer).withPlacements(placements).build();
			if(loadValidator != null) {
				loadValidator.isValid(placements, reasons);
			}
			addReasons(visualization, references, reasons);
		}
		// boxes in containers in order of their container priority
		List<ValidatorResultReason> reasons = new ArrayList<>();
		new ContainerPriorityValidator().validate(inputContainers, reasons);
		addReasons(visualization, references, reasons);
		return visualization;
	}

	/**
	 * Set the centre of gravity of the load: the boxes' centres of gravity (by default their centres), weighted by their weight.
	 */
	protected void setCenterOfGravity(ContainerVisualizer containerVisualization, List<Placement> placements) {
		double weight = 0;
		double x = 0;
		double y = 0;
		double z = 0;
		for (Placement placement : placements) {
			BoxStackValue stackValue = placement.getStackValue();
			double w = placement.getWeight();
			weight += w;
			x += w * (placement.getAbsoluteX() + getCenterOfGravity(stackValue.getCenterOfGravityX(), stackValue.getDx()));
			y += w * (placement.getAbsoluteY() + getCenterOfGravity(stackValue.getCenterOfGravityY(), stackValue.getDy()));
			z += w * (placement.getAbsoluteZ() + getCenterOfGravity(stackValue.getCenterOfGravityZ(), stackValue.getDz()));
		}
		if(weight > 0) {
			containerVisualization.setCenterOfGravityX(x / weight);
			containerVisualization.setCenterOfGravityY(y / weight);
			containerVisualization.setCenterOfGravityZ(z / weight);
		}
	}

	private static double getCenterOfGravity(int centerOfGravity, int size) {
		// -1: not configured, use the centre
		return centerOfGravity == -1 ? size / 2.0 : centerOfGravity;
	}

	protected void addReasons(PackagingResultVisualizer visualization, Map<Placement, PlacementReferenceVisualizer> references, List<ValidatorResultReason> reasons) {
		for (ValidatorResultReason reason : reasons) {
			addReason(visualization, references, reason.getClass().getSimpleName(), reason.getCode(), reason.getMessage(), getPlacements(reason));
		}
	}

	protected void addReason(PackagingResultVisualizer visualization, Map<Placement, PlacementReferenceVisualizer> references, String type, int code, String message, List<Placement> placements) {
		for (ValidationReasonVisualizer existing : visualization.getValidationReasons()) {
			if(existing.getType().equals(type) && existing.getMessage().equals(message)) {
				// also found by the visualizer's own validation, see visualize(List)
				return;
			}
		}
		LOGGER.warning("Invalid result: " + type + ": " + message);

		ValidationReasonVisualizer reasonVisualization = new ValidationReasonVisualizer();
		reasonVisualization.setType(type);
		reasonVisualization.setCode(code);
		reasonVisualization.setMessage(message);

		int reasonIndex = visualization.getValidationReasons().size();
		for (Placement placement : placements) {
			PlacementReferenceVisualizer reference = references.get(placement);
			if(reference != null) {
				reasonVisualization.getPlacements().add(reference);
				visualization.getContainers().get(reference.getContainer()).getStack().getPlacements().get(reference.getPlacement()).getReasons().add(reasonIndex);
			}
		}
		visualization.getValidationReasons().add(reasonVisualization);
		visualization.setValid(false);
	}

	/**
	 * @return the placements a reason concerns, if known
	 */
	protected List<Placement> getPlacements(ValidatorResultReason reason) {
		if(reason instanceof ExcessiveLoadWeightReason r) {
			return List.of(r.getPlacement());
		}
		if(reason instanceof InsertedBeforeSupporterReason r) {
			return List.of(r.getPlacement(), r.getSupporter());
		}
		if(reason instanceof BlockedInsertionReason r) {
			return List.of(r.getPlacement(), r.getBlocking());
		}
		if(reason instanceof BlockedExtractionReason r) {
			return List.of(r.getPlacement(), r.getBlocking());
		}
		if(reason instanceof InterleavedGroupReason r) {
			return List.of(r.getPlacement(), r.getOther());
		}
		if(reason instanceof ContainerPriorityReason r) {
			return List.of(r.getPlacement(), r.getEarlier());
		}
		if(reason instanceof ExcessiveLoadPressureReason r) {
			return List.of(r.getPlacement());
		}
		if(reason instanceof ExcessiveLoadBoxCountReason r) {
			return List.of(r.getPlacement());
		}
		if(reason instanceof NonIdenticalLoadBoxReason r) {
			return List.of(r.getConstrainedPlacement(), r.getOffendingPlacement());
		}
		if(reason instanceof InsufficientSupportAreaReason r) {
			return List.of(r.getPlacement());
		}
		if(reason instanceof UnstableCenterOfGravityReason r) {
			return List.of(r.getPlacement());
		}
		if(reason instanceof UnstableStackCenterOfGravityReason r) {
			return List.of(r.getPlacement());
		}
		return List.of();
	}
}
