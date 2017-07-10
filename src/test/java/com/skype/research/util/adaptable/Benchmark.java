/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.adaptable.mocks.DivisionFilter;
import com.skype.research.util.adaptable.mocks.IntValue;
import com.skype.research.util.projection.CompositeProjectorImpl;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Random;

/**
 * Benchmark {@link AdaptableSkipList} and {@link RangedAdaptableSkipList}.
 */
public class Benchmark {
	
	public static void main(String[] args) {
		final PrintStream out = System.err;
		AdaptableSkipList<IntValue> integers = createDichotomy();
		final int filterCount = integers.getFilterCount();
		
		final int VALUE_COUNT = 1 << 16;
		
		Random random = new Random(0);
		for (int i = 0; i < VALUE_COUNT; ++i) {
			integers.add(new IntValue(random.nextInt()));
		}
		int fi = filterCount;
		while (fi > 0) {
			--fi;
			out.println("Divisor number " + fi + " value " + (1 << fi));
			runFullBanchmark(out, integers, fi, false);
			runFullBanchmark(out, integers, fi, true);
		}
	}
	
	protected static void runFullBanchmark(PrintStream out, AdaptableSkipList<IntValue> integers, int fi, boolean smart) {
		long time = System.currentTimeMillis();
		final int CYCLE_COUNT = 1 << 10;
		for (int i = 0; i < CYCLE_COUNT; ++i) {
			Iterator<IntValue> itr = smart ? integers.ladderIterator(fi) : integers.walkerIterator(fi);
			while (itr.hasNext()) {
				itr.next().getValue();
			}
		}
		long done = System.currentTimeMillis();
		out.println((smart ? "Ladder" : "Walker") + ": done in " + (done - time) + " ms");
	}
	
	protected static AdaptableSkipList<IntValue> createDichotomy() {
		CompositeProjectorImpl<IntValue> projector = new CompositeProjectorImpl<IntValue>();
		projector.addFilter(Trivial.universeFilter());
		int fi = 1;
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		projector.addFilter(new DivisionFilter(1 << (fi++)));
		return new AdaptableSkipList<IntValue>(4, 8, 0, projector);
	}

	/*
Divisor number 8 value 256
Walker: done in 2813 ms
Ladder: done in 208 ms
Divisor number 7 value 128
Walker: done in 2630 ms
Ladder: done in 518 ms
Divisor number 6 value 64
Walker: done in 2645 ms
Ladder: done in 845 ms
Divisor number 5 value 32
Walker: done in 2643 ms
Ladder: done in 1232 ms
Divisor number 4 value 16
Walker: done in 2623 ms
Ladder: done in 1705 ms
Divisor number 3 value 8
Walker: done in 2626 ms
Ladder: done in 2183 ms
Divisor number 2 value 4
Walker: done in 2606 ms
Ladder: done in 2451 ms
Divisor number 1 value 2
Walker: done in 2565 ms
Ladder: done in 2678 ms
Divisor number 0 value 1
Walker: done in 2453 ms
Ladder: done in 2663 ms

Process finished with exit code 0
	 */
	
}
