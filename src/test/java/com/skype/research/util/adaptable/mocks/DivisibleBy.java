/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable.mocks;

import com.skype.research.util.primitives.Filter;

/**
 * A simple "divides by" integer filter.
 */
public class DivisibleBy implements Filter<Object> {
	
	final int divisor;
	
	public DivisibleBy(int divisor) {
		this.divisor = divisor;
	}
	
	@Override
	public boolean accept(Object item) {
		int value;
		if (item instanceof Integer) {
			value = (Integer) item;
		} else if (item instanceof Sample) {
			value = ((Sample) item).getValue();
		} else if (item instanceof IntValue) {
			value = ((IntValue) item).getValue();
		} else {
			return false;
		}
		return value % divisor == 0;
	}
}
