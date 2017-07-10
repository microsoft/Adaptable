/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.projection.MutableProjection;

/**
 * Mix-in: {@link Adaptable} with filter mutation support.
 */
public interface FlexibleAdaptable<T> extends Adaptable<T>, MutableProjection<T> {
}