/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptation;

import com.skype.research.util.adaptable.Adaptable;
import com.skype.research.util.adaptable.BulkUpdatable;
import com.skype.research.util.adaptable.ElementObserver;
import com.skype.research.util.datasets.FilteredDataSet;
import com.skype.research.util.model.Publisher;
import com.skype.research.util.model.RecyclerAdapter;
import com.skype.research.util.model.Subscriber;
import com.skype.research.util.primitives.Update;
import com.skype.research.util.unique.Registry;
import com.skype.research.util.unique.UniqueSequential;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dispatcher of Adaptable changes into [RecyclerView.Adapter]s and [ListAdapter]s.
 * Not yet a {@link Publisher} because knows nothing about filter specifications.
 * Single-thread.
 */
public class Adaptation<T> implements ElementObserver<T>, BulkUpdatable {
	
	final Adaptable<T> adaptable;
	final int filterCount;
	final BitSet dirtyMask;
	/**
	 * Position-unaware subscribers that only receive updates of the whole dataset
	 * ({@link Subscriber#notifyDataSetChanged()}).
	 */
	final Map<Subscriber<T>, Integer> coarseRoutingMap;
	/**
	 * Position-aware subscribers stored here to receive wrap-up updates at the end
	 * of large bulk operations, instead of a long series of small updates.
	 * (RecyclerView processes long series of small updates at O(N^2) complexity.)
	 * {@link #deferDispatch} flag is used to postpone small updates in this case.
	 */
	final Map<Subscriber<T>, Integer> wrapUpRoutingMap;
	/**
	 * Position-aware subscribers to deliver incremental updates to.
	 */
	final Map<RecyclerAdapter, Integer> fineRoutingMap;
	final boolean autoDetach;
	
	boolean deferDispatch = false;
	
	final Registry<FilteredDataSet<T>> selections = new UniqueSequential<FilteredDataSet<T>>(new Registry<FilteredDataSet<T>>() {
		@Override
		public FilteredDataSet<T> get(int id) {
			return new FilteredDataSet<T>(adaptable, id);
		}
	});
	
	public Adaptation(Adaptable<T> adaptable, boolean autoDetach) {
		this.adaptable = adaptable;
		filterCount = adaptable.getFilterCount();
		dirtyMask = new BitSet(filterCount);
		dirtyMask.set(0, filterCount);
		coarseRoutingMap = createMap(autoDetach);
		wrapUpRoutingMap = createMap(autoDetach);
		fineRoutingMap = createMap(autoDetach);
		this.adaptable.addElementObserver(this);
		this.autoDetach = autoDetach;
	}
	
	@Override
	public void onElementUpdated(T element, int[] position, int[] changeEstimate, int deltaSign, int[] deltaCount) {
		if (!deferDispatch) {
			for (Map.Entry<RecyclerAdapter, Integer> subscription : fineRoutingMap.entrySet()) {
				final int filterIndex = subscription.getValue();
				if (changeEstimate[filterIndex] != 0) {
					RecyclerAdapter adapter = subscription.getKey();
					final int realDelta = deltaSign * deltaCount[filterIndex];
					if (element == null) {
						if (realDelta > 0) {
							adapter.notifyItemRangeInserted(position[filterIndex], Math.abs(realDelta));
						} else if (realDelta < 0) {
							adapter.notifyItemRangeRemoved(position[filterIndex], Math.abs(realDelta));
						}
						// MOREINFO consider sending #notifyItemRangeChanged() (not the case now, but for completeness) 
					} else {
						if (realDelta > 0) {
							adapter.notifyItemInserted(position[filterIndex]);
						} else if (realDelta < 0) {
							adapter.notifyItemRemoved(position[filterIndex]);
						} else {
							adapter.notifyItemChanged(position[filterIndex]);
						}
					}
				}
			}
		}
		for (int i = 0; i < changeEstimate.length; i++) {
			if(changeEstimate[i]!= 0) {
				dirtyMask.set(i);
			}
		}
	}
	
	static <K, V> Map<K, V> createMap(boolean autoDetach) {
		return autoDetach ? new WeakHashMap<K, V>() : new IdentityHashMap<K, V>();
	}
	
	protected Adaptable<T> getAdaptable() {
		return adaptable;
	}
	
	public void subscribe(int filterIndex, Subscriber<T> subscriber) {
		subscriber.setDataSet(selections.get(filterIndex));
		if (subscriber instanceof RecyclerAdapter) {
			fineRoutingMap.put((RecyclerAdapter) subscriber, filterIndex);
			wrapUpRoutingMap.put(subscriber, filterIndex);
		} else {
			coarseRoutingMap.put(subscriber, filterIndex);
		}
	}
	
	public void unsubscribe(Subscriber<T> subscriber) {
		coarseRoutingMap.remove(subscriber);
		wrapUpRoutingMap.remove(subscriber);
		// instanceof on ART mb costly here, so remove always and suppress the warning instead:
		//noinspection SuspiciousMethodCalls
		fineRoutingMap.remove(subscriber);
	}
	
	public boolean hasChanged() {
		return dirtyMask.cardinality() > 0;
	}
	
	public void apply(Update<? super Adaptable<T>> modification) {
		if (modification.apply(adaptable)) {
			if (!deferDispatch) {
				sendBulkUpdates(coarseRoutingMap);
				dirtyMask.clear();
			}
		}
	}

	/**
	 * Helper method. Use it when the modification is large enough to be better processed as a whole.
	 * @param bulkModification a knowingly large modification.
	 */
	public void applyBulk(Update<? super Adaptable<T>> bulkModification) {
		try {
			hintBulkOpBegin();
			bulkModification.apply(adaptable);
		} finally {
			hintBulkOpCompleted();
		}
	}

	private void sendBulkUpdates(Map<Subscriber<T>, Integer> routingMap) {
		for (Map.Entry<Subscriber<T>, Integer> subscription : routingMap.entrySet()) {
			final int filterIndex = subscription.getValue();
			if (dirtyMask.get(filterIndex)) {
				subscription.getKey().notifyDataSetChanged();
			}
		}
	}

	protected void markExposed(BitSet target, boolean markValue) {
		for (Integer filterIndex : coarseRoutingMap.values()) {
			target.set(filterIndex, markValue);
		}
		for (Integer filterIndex : fineRoutingMap.values()) {
			target.set(filterIndex, markValue);
		}
	}

	@Override
	public void hintBulkOpBegin() {
		deferDispatch = true;
		adaptable.hintBulkOpBegin();
	}

	@Override
	public void hintBulkOpCompleted() {
		deferDispatch = false;
		adaptable.hintBulkOpCompleted();
		sendBulkUpdates(coarseRoutingMap);
		sendBulkUpdates(wrapUpRoutingMap);
		dirtyMask.clear();
	}
}
