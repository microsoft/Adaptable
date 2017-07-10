/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.projection.Projector;

import java.util.BitSet;

/**
 * Operations on vectors in the filtered space.
 */
class Distance {
	private Distance() {}
	
	static int[] toArray(BitSet mask) {
		int[] array = new int[mask.cardinality()];
		for (int ai = 0, fi = mask.nextSetBit(0); fi >= 0; fi = mask.nextSetBit(fi + 1)) {
			array[ai++] = fi;
		}
		return array;
	}

	static <T> int[] project(int[] target, T element, Projector<? super T> projector) {
		return project(target, element, target.length, projector);
	}

	static <T> int[] project(int[] target, T element, int horizon, Projector<? super T> projector) {
		for (int filterIndex = 0; filterIndex < horizon; ++filterIndex) {
			target[filterIndex] = projector.accept(element, filterIndex, target) ? 1 : 0;
		}
		return target;
	}
	
	static <T> int[] project(int[] target, T element, int[] indices, Projector<? super T> projector) {
		for (int fi : indices) {
			target[fi] = projector.accept(element, fi, target) ? 1 : 0;
		}
		return target;
	}
	
	static int[] add(int[] target, int[] source) {
		return add(target, source, target.length);
	}
	
	static int[] add(int[] target, int[] source, int horizon) {
		for (int filterIndex = 0; filterIndex < horizon; ++filterIndex) {
			target[filterIndex] += source[filterIndex];
		}
		return target;
	}
	
	static int[] sub(int[] target, int[] source) {
		return sub(target, source, target.length);
	}

	static int[] sub(int[] target, int[] source, int horizon) {
		for (int filterIndex = 0; filterIndex < horizon; ++filterIndex) {
			target[filterIndex] -= source[filterIndex];
		}
		return target;
	}

	static int[] add(int[] target, int sourceMultiplier, int[] source) {
		return add(target, sourceMultiplier, source, target.length);
	}

	static int[] add(int[] target, int sourceMultiplier, int[] source, int horizon) {
		if (sourceMultiplier != 0) {
			for (int filterIndex = 0; filterIndex < horizon; ++filterIndex) {
				target[filterIndex] += sourceMultiplier * source[filterIndex];
			}
		}
		return target;
	}

	static int[] add(int[] target, int[] source, int[] indices) {
		for (int fi : indices) {
			target[fi] += source[fi];
		}
		return target;
	}
	
	static int[] sub(int[] target, int[] source, int[] indices) {
		for (int fi : indices) {
			target[fi] -= source[fi];
		}
		return target;
	}
	
	static int[] add(int[] target, int sourceMultiplier, int[] source, int[] indices) {
		if (sourceMultiplier != 0) {
			for (int fi : indices) {
				target[fi] += sourceMultiplier * source[fi];
			}
		}
		return target;
	}

	static int[] set(int[] target, int[] source) {
		return set(target, source, target.length);
	}

	static int[] set(int[] target, int[] source, int horizon) {
		System.arraycopy(source, 0, target, 0, horizon);
		return target;
	}
	
	static int[] set(int[] target, int[] source, int[] indices) {
		for (int fi : indices) {
			target[fi] = source[fi];
		}
		return target;
	}
	
	static int[] bis(int[] target, BitSet indices) {
		return fill(target, 1, indices);
	}

	static int[] bic(int[] target, BitSet indices) {
		return fill(target, 0, indices);
	}
	
	static int[] fill(int[] target, int value, BitSet indices) {
		for (int fi = indices.nextSetBit(0); fi >= 0; fi = indices.nextSetBit(fi + 1)) {
			target[fi] = value;
		}
		return target;
	}
	
	static boolean isZero(int[] vector, int horizon) {
		for (int i = 0; i < horizon; i++) {
			if (vector[i] != 0)
				return false;
		}
		return true;
	}
	
	static boolean isZero(int[] vector) {
		return isZero(vector, vector.length);
	}
}
