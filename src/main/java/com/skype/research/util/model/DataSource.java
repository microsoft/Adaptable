/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.model;

/**
 * Simplest possible common random-data access interface.
 */
public interface DataSource<T> {
	int getCount();
	T getItem(int index);
}
