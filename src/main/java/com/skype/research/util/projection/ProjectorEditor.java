/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

import com.skype.research.util.primitives.Filter;

import java.util.BitSet;

/**
 * Allows modifications to a composite {@link Projector}
 */
public interface ProjectorEditor<T> {
    BitSet setFilter(int filterIndex, Filter<? super T> filter);
    BitSet setNarrower(int filterIndex, Filter<? super T> specialization);
    BitSet setNarrowerOf(int filterIndex, Filter<? super T> specialization, Object... preconditions);
    BitSet setDerivative(int filterIndex, Derivative derivative);
    BitSet setDerivativeOf(int filterIndex, Derivative derivative, Object... arguments);
}
