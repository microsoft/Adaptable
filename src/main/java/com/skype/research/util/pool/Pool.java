/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.pool;

/**
 * Generic pool abstraction. 
 */
public interface Pool<T> {
    T allocate();
    void recycle(T temp);
}
