/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.unique;

/**
 * Through integer index.
 */
public interface Registry<T> {
	public T get(int id);
}
