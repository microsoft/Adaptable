/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable.mocks;

/**
* A mutable integer value holder.
*/
public class IntValue implements Comparable<IntValue> {
    private int value;

    public IntValue(int value) {
        this.setValue(value);
    }

    @Override
    public int compareTo(IntValue intValue) {
	    return value == intValue.value ? 0 : value > intValue.value ? 1 : -1;
    }
    
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof IntValue && value == ((IntValue) o).value;
    
    }
    
    @Override
    public int hashCode() {
        return value;
    }
    
    public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

    @Override
    public String toString() {
        return "{" + value + '}';
    }
}
