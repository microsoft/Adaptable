/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

/**
 * Represents a boolean expression on an int[] argument list.
 */
public interface Derivative {
    public boolean accept(int... args);
}
