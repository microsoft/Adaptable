/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.projection.CompositeProjector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * A sectioned skip list, i.e. one capable of on-the-fly item classification and ranging.
 */
public class RangedAdaptableSkipList<G, T> extends AdaptableSkipList<T> implements RangedAdaptable<T> {

    final Map<G, int[]> aggregates = new HashMap<G, int[]>(); 
    final Ranger<G, T> ranger;
    final int headItemFilter;

	// optimizations
	boolean delayAggregation;

    public RangedAdaptableSkipList(Ranger<G, T> ranger,
                                   int levelCount, int denominator,
                                   int universeFilter,
                                   int headItemFilter,
                                   CompositeProjector<T> projector) {
        this(ranger, levelCount, denominator, Trivial.naturalOrder(), universeFilter, headItemFilter, projector);
    }

    public RangedAdaptableSkipList(Ranger<G, T> ranger,
                                   int levelCount, int denominator, 
                                   Comparator<? super T> comparator,
                                   int universeFilter,
                                   int headItemFilter,
                                   CompositeProjector<T> projector) {
        super(levelCount, denominator, comparator, universeFilter, projector);
        this.ranger = ranger;
        this.headItemFilter = headItemFilter;
        setBroadcastOldValue(true);
	    addElementObserver(new ElementObserver<T>() {
		    @Override
		    public final void onElementUpdated(T element, 
		                                       int[] position, 
		                                       int[] changeEstimate, 
		                                       int deltaSign, 
		                                       int[] deltaCount) {
			    adjustAggregation(element, deltaSign, deltaCount);
		    }
	    });
    }
   
    final void adjustAggregation(T element, int deltaSign, int[] deltaCount) {
        if (element != null && deltaSign != 0 && !ranger.accept(element) && !Distance.isZero(deltaCount, horizon)) {
            Iterable<G> sectionKeys = ranger.qualify(element);
            for (G sectionKey : sectionKeys) {
                int[] aggregate = aggregates.get(sectionKey);
                boolean isNewSection = aggregate == null;
                if (isNewSection) {
                    // create if not exists
                    aggregate = newDistance();
                    aggregates.put(sectionKey, aggregate);
                }
                Distance.add(aggregate, deltaSign, deltaCount, horizon);
	            if (!delayAggregation) {
		            final T sectionItem = ranger.getRangeItem(sectionKey);
		            if (isNewSection) {
			            add(sectionItem);
		            } else {
			            updateFilters(sectionItem, Trivial.refresh());
		            }
	            }
            }
        }
    }

    @Override
    public boolean accept(T element, int filterIndex, int[] precomputed) {
        if (filterIndex == headItemFilter) {
            return ranger.accept(element);
        } else if (projector.shouldComputeForGroup(filterIndex) || !ranger.accept(element)) {
            return super.accept(element, filterIndex, precomputed);
        }
	    final int[] distance = aggregates.get(ranger.getRangeKey(element));
        return distance != null && distance[filterIndex] > 0;
    }

	@Override
    public int getHeadItemFilterIndex() {
        return headItemFilter;
    }

    @Override
    public void updateRangeClassification() {
        // we assume fewer ranges, so it's easier to wipe them out
        while (size(headItemFilter) != 0) {
	        remove(headItemFilter, 0);
        }
        aggregates.clear();
        // full rebuild - wipe all existing sections
        Node node = absMinNode;
        Node prev = node;
	    while ((node = node.nodes[0]) != null) { // this is safe against any upcoming insertions
            adjustAggregation(node.element, 1, prev.distances[0]);
            prev = node;
        }
    }

	@Override
	public void clear() {
		aggregates.clear();
		super.clear();
	}

	@Override
	public void hintBulkOpBegin() {
		delayAggregation = true;
	}

    @Override
	public void hintBulkOpCompleted() {
	    if (delayAggregation) {
		    for (G sectionKey : aggregates.keySet()) {
			    T sectionItem = ranger.getRangeItem(sectionKey);
			    if (!updateFilters(sectionItem, Trivial.refresh())) {
				    add(sectionItem);
			    }
		    }
	    }
	    delayAggregation = false;
	}

    @Override
	public void updateRangePopulation() {
		int aggregates = size(headItemFilter);
		for (int index = 0; index < aggregates; ++index) {
			// FIXME implement update*() by index and filter
			updateFilters(get(headItemFilter, index), Trivial.refresh());
		}
	}

    @Override
    public int getChildCount(int sourceElementIndex, int sourceFilterIndex, int targetFilterIndex) {
        return getChildCount(get(sourceFilterIndex, sourceElementIndex), targetFilterIndex);
    }

    @Override
    public int getChildCount(T item, int targetFilterIndex) {
        if (ranger.accept(item)) {
	        final int[] aggregate = aggregates.get(ranger.getRangeKey(item));
	        if (aggregate == null) {
		        return 0;
	        }
	        return aggregate[targetFilterIndex];
        }
	    return 0;
    }
}
