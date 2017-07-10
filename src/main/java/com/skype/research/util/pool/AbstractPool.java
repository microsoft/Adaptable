/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.pool;

import com.skype.research.util.primitives.Factory;
import com.skype.research.util.primitives.Update;

import java.util.Collection;

/**
 * Abstract pool implementation.
 */
public abstract class AbstractPool<T> implements Pool<T> {
    static final Update<Object> NO_UPDATE = new Update<Object>() {
        @Override
        public boolean apply(Object element) {
            return false;
        }
    };
    
    final Factory<? extends T> factory;
    final Update<? super T> laundry;

    public AbstractPool(Factory<? extends T> factory) {
        this(factory, NO_UPDATE);
    }
    
    public AbstractPool(Factory<? extends T> factory, Update<? super T> laundry) {
        this.factory = factory;
        this.laundry = laundry;
    }

    @Override
    public final T allocate() {
        T candidate = tryGet();
        return candidate != null ? candidate : factory.create();
    }

    protected abstract T tryGet();

    @Override
    public final void recycle(T temp) {
        laundry.apply(temp);
        offer(temp);
    }

    protected abstract void offer(T temp);

    public static <T> void drain(Pool<T> pool, Collection<T> container) {
        for (T element : container) {
            pool.recycle(element);
        }
        container.clear();
    }
}
