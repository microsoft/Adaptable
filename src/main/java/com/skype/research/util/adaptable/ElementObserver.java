/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.primitives.Update;

/**
 * Incremental change observer.
 */
public interface ElementObserver<T> {
	/**
	 * Represents a single {@link Adaptable} state update.
	 * 
	 * The format is chosen to minimize the amount of computations within the {@link ElementObservable}.
	 * For instance, "deltaCount" may contain the same value in the cases of element addition or removal,
	 * however addition will result in deltaSign=1 and removal in deltaSign=-1. The actual selection size
	 * increment for selection <i>i</i> equals <i>deltaSign * deltaCount[i]</i>. No assumptions about the
	 * value or sign of "deltaSign" or "deltaCount[i]" alone should be made.
	 * 
	 * DeltaSign=0 indicates an update that does not, by definition, affect selection membership,
	 * such as one resulting from {@link ElementEditor#updateInPlace(Object, Update)}. 
	 * 
	 * All array parameters have their length equal to the filter count (aka selection count).
	 * 
	 * @param element element being updated, added or removed. null indicates a batch update ("all changed").
	 * @param position starting positions of the update within filtered selections.
	 * @param changeEstimate represents affected selections. if a selection is changed, the respective element is nonzero,
	 *               otherwise zero. No other assumptions about the returned values should be made.   
	 * @param deltaSign represents the "direction" of the update.
	 * @param deltaCount represents the "absolute value" of the update.
	 */
	public void onElementUpdated(T element, int[] position, int[] changeEstimate, int deltaSign, int[] deltaCount);
}
