/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

/**
 * Mix-in composite {@link Projector} interface.
 */
public interface CompositeProjector<T> extends Projector<T>, ProjectorBuilder<T>, ProjectorEditor<T> {
	boolean shouldComputeForGroup(int filterIndex);
}
