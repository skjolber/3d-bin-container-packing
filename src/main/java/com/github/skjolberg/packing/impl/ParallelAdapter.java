package com.github.skjolberg.packing.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

import com.github.skjolberg.packing.Container;

public class ParallelAdapter implements Adapter {

	private static class ContainerAdapter {
		
		private RunnableAdapter[] runnableAdapters;
	}

	private static class RunnableAdapter implements Runnable {

		private Adapter adapter;
		private int containerIndex;

		private volatile PackResult result;
		private Phaser phaser;
		@Override
		public void run() {
			try {
				result = adapter.attempt(containerIndex);
			} finally {
				phaser.arriveAndDeregister();
			}
		}
		
		public PackResult getResult() {
			return result;
		}
		
		public void reset() {
			result = null;
		}
	}
	
	private ExecutorService executorService;
	private Adapter[] adapters;
	
	@Override
	public Container accepted(PackResult result) {
		Container container = null;
		for(Adapter adapter : adapters) {
			container = adapter.accepted(result);
		}
		
		return container;
	}

	@Override
	public PackResult attempt(int containerIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasMore(PackResult result) {
		boolean hasMore = false;
		for(Adapter adapter : adapters) {
			if(adapter.hasMore(result)) {
				hasMore = true;
			}
		}
		
		return hasMore;
	}

}
