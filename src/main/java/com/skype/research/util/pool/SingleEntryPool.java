/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.pool;

import com.skype.research.util.primitives.Factory;
import com.skype.research.util.primitives.Update;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Conserve allocations when using a temporary object while still staying thread safe.
 * Expecting low contention, stores at most one entry.
 */
public class SingleEntryPool<T> extends AbstractPool<T> implements Pool<T> {

    final AtomicReference<T> container = new AtomicReference<T>();

    public SingleEntryPool(Factory<? extends T> factory) {
        this(factory, NO_UPDATE);
    }
    
    public SingleEntryPool(Factory<? extends T> factory, Update<? super T> laundry) {
        super(factory, laundry);
        container.set(factory.create());
    }
    
    @Override
    protected T tryGet() {
        return container.getAndSet(null);
    }

    @Override
    protected void offer(T temp) {
        container.compareAndSet(null, temp);
    }
}
