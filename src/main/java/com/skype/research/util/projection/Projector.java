/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.projection;

/**
 * A projection component computing block.
 * May apply a {@link com.skype.research.util.primitives.Filter}, use precomputed values (below filterIndex)
 * or query underlying aggregated data in aggregated container implementations.
 */
public interface Projector<T> {
	/**
	 * Evaluates a condition over an element value and/or conditions evaluated beforehand.
	 * The method must have no side effects. No object state or argument value may be modified.
	 * 
	 * @param element     element to apply the condition to
	 * @param filterIndex index of the condition to evaluate
	 * @param precomputed dependency conditions (index &lt; filterIndex) pre-evaluated on the same element.
	 *                    Used for filter chaining (e.g. index=1: "is a man"; index=2: "is tall"; index=3: "is a tall man").
	 *                    Elements of indices equal to or greater than filterIndex should NOT be used, to avoid cyclic dependencies.
	 *                    Integers rather than of boolean are used to allow further vector arithmetic (e.g. summarizing, differencing etc.).
	 * @return true if the element passes the specified condition, false otherwise.
	 */
	boolean accept(T element, int filterIndex, int[] precomputed);
}
