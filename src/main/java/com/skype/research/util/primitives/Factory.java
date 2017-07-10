/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.primitives;

/**
 * Factory of anything.
 */
public interface Factory<T> {
    public T create();
}
