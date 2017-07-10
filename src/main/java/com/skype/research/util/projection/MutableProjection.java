/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

import java.util.BitSet;

/**
 * Exposes a projection editor and a "commit" call to finalize modifications.
 */
public interface MutableProjection<T> {
	ProjectorEditor<T> getFilterEditor();
	BitSet refreshFilters(BitSet dirtyMask);
}
