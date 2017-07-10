/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.primitives.Factory;
import com.skype.research.util.primitives.Filter;
import com.skype.research.util.projection.CompositeProjectorImpl;
import com.skype.research.util.projection.Derivative;
import com.skype.research.util.projection.ProjectorBuilder;
import com.skype.research.util.projection.HopefulProjectorImpl;

import java.util.BitSet;
import java.util.Comparator;

/**
 * Encapsulates pre-instantiation Adaptable configuration.
 */
public class AdaptableFactory<T> implements ProjectorBuilder<T>, Factory<FlexibleAdaptable<T>> {
    
    // skeletal dependencies
    final CompositeProjectorImpl<T> projector = new HopefulProjectorImpl<T>();
	final boolean withRanging;

	// replaceable dependencies
    Comparator<? super T> comparator = Trivial.naturalOrder();
    final int universeFilter = 0;
    final int headItemFilter = 1;
    
    int denominator = 3;
    int levelCount = 15;
    
	private boolean allowDuplicates;
    private boolean broadcastOldValue;

    public AdaptableFactory() {
        this(false);
    }
    
    public AdaptableFactory(boolean withRanging) {
        this(Trivial.universeFilter(), withRanging);
    }
    
    public AdaptableFactory(Filter<? super T> customUniverseFilter, boolean withRanging) {
        projector.addFilter(customUniverseFilter);
        projector.freezeFilter(universeFilter);
	    this.withRanging = withRanging;
        if (withRanging) {
            projector.addFilter(Trivial.placeholder());
        }
    }

    public void setDenominator(int denominator) {
        this.denominator = denominator;
    }

    public void setLevelCount(int levelCount) {
        this.levelCount = levelCount;
    }

    public void setComparator(Comparator<? super T> comparator) {
        this.comparator = comparator;
    }

	public void setAllowDuplicates(boolean allowDuplicates) {
		this.allowDuplicates = allowDuplicates;
	}

	public void setBroadcastOldValue(boolean broadcastOldValue) {
		this.broadcastOldValue = broadcastOldValue;
	}

    @Override
    public int addFilter(Filter<? super T> filter) {
        return projector.addFilter(filter);
    }

    @Override
    public int addNarrower(Filter<? super T> specialization, int... preconditions) {
        return projector.addNarrower(specialization, preconditions);
    }

    @Override
    public int addNarrowerOf(Filter<? super T> specialization, Object... preconditions) {
        return projector.addNarrowerOf(specialization, preconditions);
    }

    @Override
    public int addDerivative(Derivative derivative, int... arguments) {
        return projector.addDerivative(derivative, arguments);
    }

    @Override
    public int addDerivativeOf(Derivative derivative, Object... arguments) {
        return projector.addDerivativeOf(derivative, arguments);
    }

    @Override
    public BitSet setShouldComputeForGroup(int filterIndex, boolean shouldCompute) {
        return projector.setShouldComputeForGroup(filterIndex, shouldCompute);
    }
	
	@Override
	public void freezeFilter(int filterIndex) {
		projector.freezeFilter(filterIndex);
	}

    @Override
    public int getFilterCount() {
        return projector.getFilterCount();
    }

    @Override
    public int getHorizon() {
        return projector.getHorizon();
    }

    public FlexibleAdaptable<T> create() {
		if (withRanging) {
			throw new IllegalStateException("Ranger expected");
		}
        projector.freeze();
		AdaptableSkipList<T> adaptable = new AdaptableSkipList<T>(levelCount, denominator, comparator, universeFilter, projector);
		adaptable.setBroadcastOldValue(broadcastOldValue);
	    adaptable.setAllowDuplicates(allowDuplicates);
		return adaptable;
    }

    public <G> RangedAdaptable<T> create(Ranger<G, T> ranger) {
	    if (!withRanging) {
		    throw new IllegalStateException("Ranger not expected");
	    }
        projector.setFilter(headItemFilter, ranger);
        projector.freezeFilter(headItemFilter);
        projector.freeze();
	    RangedAdaptableSkipList<G, T> adaptable = new RangedAdaptableSkipList<G, T>(ranger, levelCount, denominator, comparator, universeFilter, headItemFilter, projector);
	    // adaptable.setBroadcastOldValue(broadcastOldValue) is not needed here as it's automatic for RASL
	    adaptable.setAllowDuplicates(allowDuplicates);
	    return adaptable;
    }
}
