/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable.mocks;

import com.skype.research.util.primitives.Filter;

/**
 * Simplest filter to guarantee a desired percentage of positives.
 */
public class DivisionFilter implements Filter<IntValue> {
	final int divisor;
	
	public DivisionFilter(int divisor) {
		this.divisor = divisor;
	}
	
	@Override
	public boolean accept(IntValue item) {
		return (item.getValue() % divisor) == 0;
	}
}
