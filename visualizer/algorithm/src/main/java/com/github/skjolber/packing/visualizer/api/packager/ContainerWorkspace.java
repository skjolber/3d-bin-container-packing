package com.github.skjolber.packing.visualizer.api.packager;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;

public class ContainerWorkspace {

	private Container container;
	
	private List<Room> rooms = new ArrayList<>();
	
	public Container getContainer() {
		return container;
	}
	
	public void setContainer(Container container) {
		this.container = container;
	}
	
	public List<Room> getRooms() {
		return rooms;
	}
	
	public void setRooms(List<Room> rooms) {
		this.rooms = rooms;
	}
	
}
