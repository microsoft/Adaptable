/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.pool;

import com.skype.research.util.primitives.Factory;
import com.skype.research.util.primitives.Update;

import java.util.Queue;

/**
 * Pool backed by a (bounded or unbounded) queue.
 */
public class MultipleEntryPool<T> extends AbstractPool<T> {
    final Queue<T> queue;

    public MultipleEntryPool(Queue<T> queue, Factory<? extends T> factory) {
        this(queue, factory, NO_UPDATE);
    }
    
    public MultipleEntryPool(Queue<T> queue, Factory<? extends T> factory, Update<? super T> laundry) {
        super(factory, laundry);
        this.queue = queue;
    }

    @Override
    protected T tryGet() {
        return queue.poll();
    }

    @Override
    protected void offer(T temp) {
        queue.offer(temp);
    }
}
