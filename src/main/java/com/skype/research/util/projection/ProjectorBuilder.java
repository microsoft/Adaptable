/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

import com.skype.research.util.primitives.Filter;

import java.util.BitSet;

/**
 * Projection builder.
 */
public interface ProjectorBuilder<T> {
    int addFilter(Filter<? super T> filter);
    int addNarrower(Filter<? super T> specialization, int... preconditions);
    int addNarrowerOf(Filter<? super T> specialization, Object... preconditions);
    int addDerivative(Derivative derivative, int... arguments);
    int addDerivativeOf(Derivative derivative, Object... arguments);
    BitSet setShouldComputeForGroup(int filterIndex, boolean shouldCompute);
	void freezeFilter(int filterIndex);
    int getFilterCount();
    int getHorizon();
}
