/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable.mocks;

import java.util.Random;

/**
 * A sample on a relative (base-adjustable) scale. 
 */
public class Sample {
    static final Random random = new Random(924);
    
    static int baseLine = 0;
    
    public static void setBaseLine(int baseLine) {
        Sample.baseLine = baseLine;
    }
    
    public boolean boundary;
    public int value;
    
    public Sample() {
        boundary = false;
        value = random.nextInt(16384);
    }
    
    public Sample(int literal) {
        boundary = true;
        value = literal;
    }
    
    public int getRange() {
        // ensure top header placement
        int remainder = getValue() % 1000;
        if (remainder < 0) {
            remainder += 1000;
        }
        return getValue() - remainder;
    }
    
    public int getValue() {
        return boundary ? value : value - baseLine;
    }

    @Override
    public String toString() {
        return (boundary ? "!" + (value + baseLine) : "," + value);
    }
}
