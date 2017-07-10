/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.primitives.Filter;
import com.skype.research.util.projection.CompositeProjectorImpl;
import com.skype.research.util.projection.Derivative;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.BitSet;

/**
 * Composite filter tests.
 */
public class TestCompositeProjector extends TestCase {

    Filter<Integer> isEven = new Filter<Integer>() {
        @Override
        public boolean accept(Integer item) {
            return item % 2 == 0;
        }
    };
    Filter<Integer> isMultipleOf3 = new Filter<Integer>() {
        @Override
        public boolean accept(Integer item) {
            return item % 3 == 0;
        }
    };
    Filter<Integer> isPowerOf2 = new Filter<Integer>() {
        @Override
        public boolean accept(Integer item) {
            return Long.bitCount(item) == 1;
        }
    };
    Derivative binaryIntersection = new Derivative() {
        @Override
        public boolean accept(int... args) {
            return args[0] * args[1] != 0;
        }
    };
    Derivative binaryDifference = new Derivative() {
        @Override
        public boolean accept(int... args) {
            return args[0] == 0 ^ args[1] == 0;
        }
    };
    Derivative binaryUnion = new Derivative() {
        @Override
        public boolean accept(int... args) {
            return (args[0] | args[1]) != 0;
        }
    };
    Derivative ternaryIntersection = new Derivative() {
        @Override
        public boolean accept(int... args) {
            return args[0] * args[1] * args[2] != 0;
        }
    };
    CompositeProjectorImpl<Integer> projector;
    
    int isEvenIndex;
    int isMul3Index;
    int binDifIndex;
    int binIntIndex;
    int binUniIndex;
    int isPow2Index;
    int triIntIndex;
    
    public void setUp() throws Exception {
        super.setUp();
        projector = new CompositeProjectorImpl<Integer>();
        isEvenIndex = projector.addFilter(isEven);
        isMul3Index = projector.addFilter(isMultipleOf3);
        binDifIndex = projector.addDerivativeOf(binaryDifference, isEven, isMultipleOf3);
        binIntIndex = projector.addDerivativeOf(binaryIntersection, isEven, 1);
        binUniIndex = projector.addDerivativeOf(binaryUnion, 0, 1);
        isPow2Index = projector.addFilter(isPowerOf2);
        triIntIndex = projector.addDerivativeOf(ternaryIntersection, 0, isPowerOf2, binaryUnion);
    }

    public void testAmalgam() throws Exception {
        Assert.assertEquals("Total filter count", 7, projector.getFilterCount());
        // we pass null to make sure that the precomputed array is not accessed by direct filters 
        Assert.assertFalse(projector.accept(7, isEvenIndex, null));
        Assert.assertFalse(projector.accept(7, isMul3Index, null));
        Assert.assertFalse(projector.accept(7, isPow2Index, null));
        Assert.assertTrue(projector.accept(8, isPow2Index, null));
        int[] precomputed = new int[] { 1, 1, 0, 0, 1, 1, 0 }; // even, mul3, NOT dif, NOT int, uni, pow2, NOT 3-int
        // now we pass null as element value
        Assert.assertFalse(projector.accept(null, binDifIndex, precomputed)); // 1 ^ 1
        Assert.assertTrue(projector.accept(null, binIntIndex, precomputed)); // 1 & 1
        Assert.assertTrue(projector.accept(null, binUniIndex, precomputed)); // 1 | 1
        Assert.assertTrue(projector.accept(null, triIntIndex, precomputed)); // 1 & 1 & 1
    }
    
    private static BitSet createBitSet(int... args) {
        final BitSet out = new BitSet();
        for (int arg : args) {
            out.set(arg);
        }
        return out;
    }
    
    public void testDependencies() throws Exception {
        Assert.assertEquals(createBitSet(isEvenIndex, binUniIndex, binDifIndex, binIntIndex, triIntIndex),
                projector.getDependents(isEvenIndex));
        Assert.assertEquals(createBitSet(isMul3Index, binUniIndex, binDifIndex, binIntIndex, triIntIndex),
                projector.getDependents(isMul3Index));
        Assert.assertEquals(createBitSet(binDifIndex),
                projector.getDependents(binDifIndex));
        Assert.assertEquals(createBitSet(binIntIndex),
                projector.getDependents(binIntIndex));
        Assert.assertEquals(createBitSet(binUniIndex, triIntIndex),
                projector.getDependents(binUniIndex));
        Assert.assertEquals(createBitSet(isPow2Index, triIntIndex),
                projector.getDependents(isPow2Index));
        Assert.assertEquals(createBitSet(triIntIndex),
                projector.getDependents(triIntIndex));
    }

    public void tearDown() throws Exception {
        ;;;
        super.tearDown();
    }
}
