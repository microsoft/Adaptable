/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.primitives;

/**
 * A modification operation.
 */
public interface Update<T> {
	/**
	 * Modify the passed object.
	 * @param element object to modify.
	 * @return true if the object contents have changed, false otherwise.
	 */
	public boolean apply(T element);
}
