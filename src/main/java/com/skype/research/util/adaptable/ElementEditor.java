/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.primitives.Update;

/**
 * Add-on interface to {@link Adaptable} to define incremental, "green" update operations.
 * 
 * The "oldValue" parameters below should only be used for navigation within the ordered storage.
 * Neither reference identity nor strict value equality are required. The only requirement is that 
 * the locator value must be neither greater nor less than the one to apply the {@link Update} to.
 */
public interface ElementEditor<T> {
	
	// Note: generally speaking, we can use Comparable<? super T> as object locator.
	// However, it's not completely clear how to ensure consistency with {@link Adaptable} comparator.
	// One possible solution could involve wrapping the Comparator together with a sample value into a
	// single Comparable closure. To allow sample types wider than T, we can add the exact comparator 
	// type parameter ("locator") as a type parameter of the {@link AdaptableSkipList} itself, instead 
	// of using a wildcard (? super T).
	//
	// Note on Note: please do not delete this comment until the team thinks twice on the idea.

	/**
	 * Update an element in place, assuming element ordering and filtered "views" are unchanged.
	 * @param oldValue existing element value. 
	 * @param modification modification operation to apply.
	 * @return true if the modification operation has actually changed anything, false otherwise.   
	 */
	public boolean updateInPlace(T oldValue, Update<? super T> modification);

	/**
	 * Update an element in place, assuming element ordering unchanged but filtering potentially affected.
	 * @param oldValue existing element value. 
	 * @param modification modification operation to apply.
	 * @return true if the modification operation has actually changed anything, false otherwise.   
	 */
	public boolean updateFilters(T oldValue, Update<? super T> modification);

	/**
	 * Update an element in place, assuming both element ordering and filtering potentially affected.
	 * @param oldValue existing element value. 
	 * @param modification modification operation to apply.
	 * @return true if the modification operation has actually changed anything, false otherwise.   
	 */
	public boolean updateReorder(T oldValue, Update<? super T> modification);
}
