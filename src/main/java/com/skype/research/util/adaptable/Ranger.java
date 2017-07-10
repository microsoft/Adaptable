/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.primitives.Filter;

/**
 * Aggregation delegate to accommodate custom client logic for {@link RangedAdaptable}. 
 */
public interface Ranger<G, T> extends Filter<T> {
    public T getRangeItem(G rangeKey);
    public G getRangeKey(T rangeItem);
    public Iterable<G> qualify(T element);
}
