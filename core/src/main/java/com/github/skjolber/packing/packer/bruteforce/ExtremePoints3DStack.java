package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.points3d.ExtremePoints3D;
import com.github.skjolber.packing.points3d.Point3D;

public class ExtremePoints3DStack extends ExtremePoints3D<StackPlacement> {
	
	protected static class StackItem {
		protected List<Point3D> values = new ArrayList<>();
		protected List<StackPlacement> placements = new ArrayList<>();
		protected StackPlacement stackPlacement = new StackPlacement();
		protected Point3D point;
	}
	
	protected List<StackItem> stackItems = new ArrayList<>();
	protected int stackIndex = 0;
	
	public ExtremePoints3DStack(int dx, int dy, int dz, int maxStackDepth) {
		super(dx, dy, dz, true);
		
		for(int i = 0; i < maxStackDepth; i++) {
			stackItems.add(new StackItem());
		}
		
		stackItems.get(0).values.addAll(values);
		
		loadCurrent();
	}

	@Override
	public boolean add(int index, StackPlacement placement) {
		stackItems.get(this.stackIndex).point = values.get(index);

		return super.add(index, placement);
	}
	
	public StackPlacement push() {
		StackItem stackItem = stackItems.get(stackIndex);
		
		stackIndex++;
		
		StackItem nextStackItem = stackItems.get(stackIndex);
		nextStackItem.placements.addAll(stackItem.placements);
		nextStackItem.values.addAll(stackItem.values);
		
		loadCurrent();
		
		return nextStackItem.stackPlacement;
	}
	
	public void redo() {
		// clear current level
		placements.clear();
		values.clear();

		StackItem stackItem = stackItems.get(stackIndex);
		stackItem.point = null;

		// copy from previous level
		StackItem previousStackItem = stackItems.get(stackIndex - 1);

		placements.addAll(previousStackItem.placements);
		values.addAll(previousStackItem.values);
	}

	public void pop() {
		StackItem nextStackItem = stackItems.get(stackIndex);
		nextStackItem.placements.clear();
		nextStackItem.values.clear();

		nextStackItem.point = null;

		stackIndex--;
		loadCurrent();
	}
	
	private void loadCurrent() {
		StackItem stackItem = stackItems.get(stackIndex);
		
		this.values = stackItem.values;
		this.placements = stackItem.placements;
	}
	
	public List<Point3D> getPoints() {
		// item 0 is always empty
		List<Point3D> list = new ArrayList<>(stackIndex + 1);
		for (int i = 1; i < stackIndex + 1; i++) {
			StackItem stackItem = stackItems.get(i);
			list.add(stackItem.point);
		}
		return list;
	}
	
	public List<StackPlacement> getStackPlacement() {
		List<StackPlacement> list = new ArrayList<>(stackIndex);
		for (StackItem stackItem : stackItems) {
			list.add(stackItem.stackPlacement);
		}
		return list;
	}
	
	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy, dz);

		for (int i = 0; i < stackItems.size(); i++) {
			StackItem stackItem = stackItems.get(i);
			stackItem.point = null;
			
			stackItem.placements.clear();
			stackItem.values.clear();
		}

		stackIndex = 0;

		addFirstPoint();
		stackItems.get(0).values.addAll(values);
		
		loadCurrent();
	}
	
}