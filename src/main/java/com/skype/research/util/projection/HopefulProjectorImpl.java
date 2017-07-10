/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

import com.skype.research.util.adaptable.Trivial;
import com.skype.research.util.primitives.Filter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Distinguishes between hopeful and hopeless filters.
 * Also allows restoration of the original filter set.
 */
public class HopefulProjectorImpl<T> extends CompositeProjectorImpl<T> {

	// optimization: filters that pass nothing
	final BitSet hopefulFilterSet = new BitSet();
	private int lastHopefulFilter = -1;

	public boolean isFilterHopeful(Filter<? super T> specialization) {
		return specialization != Trivial.placeholder();
	}

	private boolean areAllPreconditionsHopeful(int[] preconditionPositions) {
		for (int preconditionPos : preconditionPositions) {
			if (!hopefulFilterSet.get(preconditionPos)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isNarrowerHopeful(Filter<? super T> specialization, int[] arguments) {
		return isFilterHopeful(specialization) && areAllPreconditionsHopeful(arguments);
	}

	private void markLandOfHope() {
		lastHopefulFilter = children.size();
		hopefulFilterSet.set(lastHopefulFilter);
	}

	@Override
	public int addFilter(Filter<? super T> filter) {
		if (isFilterHopeful(filter)) {
			markLandOfHope();
		}
		return super.addFilter(filter);
	}

	@Override
	public int addNarrower(Filter<? super T> specialization, int... preconditions) {
		if (isNarrowerHopeful(specialization, preconditions)) {
			markLandOfHope();
		}
		return super.addNarrower(specialization, preconditions);
	}

	@Override
	public int addDerivativeOf(Derivative derivative, Object... arguments) {
		markLandOfHope();
		return super.addDerivativeOf(derivative, arguments);
	}

	@Override
	public int getHorizon() {
		return lastHopefulFilter + 1;
	}

	@Override
	public BitSet setFilter(int filterIndex, Filter<? super T> filter) {
		revisitLandOfHope(filterIndex, isFilterHopeful(filter));
		return super.setFilter(filterIndex, filter);
	}

	private void revisitLandOfHope(int filterIndex, boolean filterHopeful) {
		hopefulFilterSet.set(filterIndex, filterHopeful);
		if (filterHopeful) {
			BitSet dependents = getDependents(filterIndex);
			dependents.andNot(hopefulFilterSet);
			int dependentIndex = filterIndex;
			while ((dependentIndex = dependents.nextSetBit(dependentIndex + 1)) >= 0) {
				// derivatives are all hopeful. single filters are independent.
				// so any successor found here is a specialization.
				final Filter<? super T> oldFilter = getOldFilter(dependentIndex);
				final int[] preconditions = getOldNarrowerArgs(dependentIndex);
				final boolean newHope = isNarrowerHopeful(oldFilter, preconditions);
				hopefulFilterSet.set(dependentIndex, newHope);
			}
		}
		lastHopefulFilter = Math.max(lastHopefulFilter, hopefulFilterSet.length() - 1);
	}

	class State {
		final List<SocialProjector<T>> children;
		final Map<Object, Integer> reverseLookup;
		final BitSet hopefulFilterSet;

		State(List<SocialProjector<T>> children, 
		      Map<Object, Integer> reverseLookup, 
		      BitSet hopefulFilterSet) {
			this.children = new ArrayList<SocialProjector<T>>(children);
			this.reverseLookup = new IdentityHashMap<Object, Integer>(reverseLookup);
			this.hopefulFilterSet = (BitSet) hopefulFilterSet.clone();
		}

		public void copyOut(List<SocialProjector<T>> children,
		                    Map<Object, Integer> reverseLookup,
		                    BitSet hopefulFilterSet) {
			Collections.copy(this.children, children);
			reverseLookup.clear();
			reverseLookup.putAll(this.reverseLookup);
			hopefulFilterSet.clear();
			hopefulFilterSet.or(this.hopefulFilterSet);
		}
	}
	
	State state;

	@Override
	public void freeze() {
		state = new State(children, reverseLookup, hopefulFilterSet);
		super.freeze();
	}
	
	public void factoryReset() {
		state.copyOut(children, reverseLookup, hopefulFilterSet);
		lastHopefulFilter = hopefulFilterSet.length() - 1;
	}

	@Override
	public BitSet setNarrower(int filterIndex, Filter<? super T> specialization, int... arguments) {
		revisitLandOfHope(filterIndex, isNarrowerHopeful(specialization, arguments));
		return super.setNarrower(filterIndex, specialization, arguments);
	}

	@Override
	public BitSet setDerivative(int filterIndex, Derivative derivative) {
		revisitLandOfHope(filterIndex, true);
		return super.setDerivative(filterIndex, derivative);
	}
}
