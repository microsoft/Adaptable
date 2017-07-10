/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

import com.skype.research.util.pool.SingleEntryPool;
import com.skype.research.util.primitives.Factory;
import com.skype.research.util.primitives.Filter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamically built projector used internally by filterable structures.
 */
public class CompositeProjectorImpl<T> implements CompositeProjector<T> {
    
    final List<SocialProjector<T>> children = new ArrayList<SocialProjector<T>>();
    final BitSet forceComputeSpec = new BitSet();
    final BitSet immutableFilters = new BitSet();
    final Map<Object, Integer> reverseLookup = new IdentityHashMap<Object, Integer>();
    private boolean[] forceCompute;
    private boolean frozen;

    @Override
    public int addFilter(Filter<? super T> filter) {
        return doAdd(new FilterProjector<T>(filter), filter);
    }

    @Override
    public int addNarrowerOf(Filter<? super T> specialization, Object... preconditions) {
	    return addNarrower(specialization, lookup(preconditions));
    }
	
	public int addNarrower(Filter<? super T> specialization, int... preconditions) {
		return doAdd(new NarrowerProjector<T>(specialization, preconditions), null);
	}

	@Override
	public int addDerivativeOf(Derivative derivative, Object... arguments) {
		return addDerivative(derivative, lookup(arguments));
	}
    
	@Override
	public int addDerivative(Derivative derivative, int... arguments) {
        return doAdd(new DerivativeProjector<T>(derivative, arguments), derivative);
    }

    private int[] lookup(Object[] arguments) {
        final int[] positions = new int[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            positions[i] = argument instanceof Number
                    // allow direct specification of SOME positional parameters
                    ? ((Number) argument).intValue()
                    // when not found, will throw a NPE on unboxing
                    : reverseLookup.get(argument);
        }
        return positions;
    }

    private int doAdd(SocialProjector<T> projector, Object anchor) {
        checkFrozen();
        final int position = children.size();
        if (projector.getDependencies().nextSetBit(position) >= 0) {
            throw new IllegalArgumentException("Cyclic dependency in filter " + anchor);
        }
	    if (anchor != null) {
		    reverseLookup.put(anchor, position);
	    }
        children.add(projector);
        return position;
    }

    private void checkFrozen() {
        if (frozen) {
            throw new IllegalStateException("structural changes frozen");
        }
    }

    public int getFilterCount() {
        return children.size();
    }
	
	public int getHorizon() {
		return getFilterCount();
	}

    public void freeze() {
	    forceCompute = new boolean[getFilterCount()];
	    for (int i = 0; i < forceCompute.length; i++) {
		    forceCompute[i] = forceComputeSpec.get(i);
	    }
        frozen = true;
    }

    @Override
    public final boolean shouldComputeForGroup(int filterIndex) {
        return forceCompute[filterIndex];
    }
    
    @Override
    public final void freezeFilter(int filterIndex) {
        immutableFilters.set(filterIndex);
    }
    
    @Override
    public BitSet setShouldComputeForGroup(int filterIndex, boolean shouldCompute) {
        forceComputeSpec.set(filterIndex, shouldCompute);
        return getDependents(filterIndex);
    }
    
    public final BitSet getDependents(int filterIndex) {
        BitSet dependents = new BitSet();
        getDependents(filterIndex, dependents, true);
        return dependents;
    }
    
    public final void getDependents(int touchedIndex, BitSet out, boolean markValue) {
        out.set(touchedIndex);
        for (int filterIndex = touchedIndex; filterIndex < children.size(); filterIndex++) {
            if (children.get(filterIndex).getDependencies().intersects(out)) {
                out.set(filterIndex, markValue);
            }
        }
    }

	protected final Filter<? super T> getOldFilter(int filterIndex) {
		// WISDOM also works for NarrowerProjector (extends FilterProjector)
		return ((FilterProjector<T>) children.get(filterIndex)).filter;
	}
	
	@Override
    public BitSet setFilter(int filterIndex, Filter<? super T> filter) {
        clear(filterIndex);
        children.set(filterIndex, new FilterProjector<T>(filter));
        reverseLookup.put(filter, filterIndex);
        return getDependents(filterIndex);
    }

    @Override
    public BitSet setNarrower(int filterIndex, Filter<? super T> specialization) {
        // the following line throws a ClassCastException if the predecessor is not a Narrower
	    return setNarrower(filterIndex, specialization, getOldNarrowerArgs(filterIndex));
    }

	protected final int[] getOldNarrowerArgs(int filterIndex) {
		return ((NarrowerProjector) children.get(filterIndex)).positions;
	}

	@Override
    public BitSet setNarrowerOf(int filterIndex, Filter<? super T> specialization, Object... arguments) {
        return setNarrower(filterIndex, specialization, lookup(arguments));
    }

	public BitSet setNarrower(int filterIndex, Filter<? super T> specialization, int... positions) {
        clear(filterIndex);
        children.set(filterIndex, new NarrowerProjector<T>(specialization, positions));
        return getDependents(filterIndex);
    }

    @Override
    public BitSet setDerivative(int filterIndex, Derivative derivative) {
        // the following line throws a ClassCastException if the predecessor is not a Derivative
	    return setDerivative(filterIndex, derivative, getOldDerivativeArgs(filterIndex));
    }

	protected final int[] getOldDerivativeArgs(int filterIndex) {
		return ((DerivativeProjector) children.get(filterIndex)).positions;
	}

	@Override
    public BitSet setDerivativeOf(int filterIndex, Derivative derivative, Object... arguments) {
        return setDerivative(filterIndex, derivative, lookup(arguments));
    }

    public BitSet setDerivative(int filterIndex, Derivative derivative, int... positions) {
        clear(filterIndex);
        children.set(filterIndex, new DerivativeProjector<T>(derivative, positions));
        reverseLookup.put(derivative, filterIndex);
        return getDependents(filterIndex);
    }

    private void clear(int filterIndex) {
        if (immutableFilters.get(filterIndex)) {
            throw new IllegalArgumentException(String.format("Attempt to alter immutable filter %d", filterIndex));
        }
        reverseLookup.remove(children.get(filterIndex).getAnchor());
    }

    @Override
    public final boolean accept(T element, int filterIndex, int[] precomputed) {
        return children.get(filterIndex).accept(element, filterIndex, precomputed);
    }

    final void ensureDirect(int filterIndex) {
        if (!isDirect(filterIndex)) {
            throw new IllegalStateException(String.format("Filter %d must be directly computable", filterIndex));
        }
    }

    final boolean isDirect(int filterIndex) {
        return children.get(filterIndex) instanceof FilterProjector;
    }

    static interface SocialProjector<T> extends Projector<T> {
        BitSet getDependencies();
        Object getAnchor();
    }
    
    static class FilterProjector<T> implements SocialProjector<T> {
        
        static final BitSet noDependencies = new BitSet();
        
        final Filter<? super T> filter;
        public FilterProjector(Filter<? super T> filter) {
            this.filter = filter;
        }

        @Override
        public boolean accept(T element, int filterIndex, int[] precomputed) {
            return filter.accept(element);
        }

        @Override
        public BitSet getDependencies() {
            return noDependencies;
        }

        @Override
        public final Object getAnchor() {
            return filter;
        }
    }
    
    static class NarrowerProjector<T> extends FilterProjector<T> {
        final int[] positions;
        final BitSet dependencies = new BitSet();

        public NarrowerProjector(Filter<? super T> filter, int[] positions) {
            super(filter);
            this.positions = positions;
            for (int dependency : positions) {
                dependencies.set(dependency);
            }
        }

        @Override
        public final BitSet getDependencies() {
            return dependencies;
        }

        @Override
        public final boolean accept(T element, int filterIndex, int[] precomputed) {
            for (int position : positions) {
                if (precomputed[position] == 0) {
                    return false;
                }
            }
            return super.accept(element, filterIndex, precomputed);
        }
    }

    static class DerivativeProjector<T> implements SocialProjector<T> {
        final Derivative derivative;
        final int[] positions;
        final int argListSize;
        final SingleEntryPool<int[]> tmpArgPool;
        final BitSet dependencies = new BitSet();

        public DerivativeProjector(Derivative derivative, int[] positions) {
            this.derivative = derivative;
            this.positions = positions;
            this.argListSize = positions.length;
            tmpArgPool = new SingleEntryPool<int[]>(new Factory<int[]>() {
                @Override
                public final int[] create() {
                    return new int[argListSize];
                }
            });
            for (int dependency : positions) {
                dependencies.set(dependency);
            }
        }

        @Override
        public final boolean accept(T element, int filterIndex, int[] precomputed) {
            int[] arguments = tmpArgPool.allocate();
            try {
                for (int i = 0; i < argListSize; i++) {
                    arguments[i] = precomputed[positions[i]];
                }
                return derivative.accept(arguments);
            } finally {
                tmpArgPool.recycle(arguments);
            }
        }

        @Override
        public final BitSet getDependencies() {
            return dependencies;
        }

        @Override
        public final Object getAnchor() {
            return derivative;
        }
    }
}
