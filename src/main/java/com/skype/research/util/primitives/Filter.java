/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.primitives;

/**
 * Generic filter interface.
 */
public interface Filter<T> {
    boolean accept(T item);
}
