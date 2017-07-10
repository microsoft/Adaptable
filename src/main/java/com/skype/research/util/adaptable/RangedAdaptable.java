/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

/**
 * Represents an aggregated adaptable.
 */
public interface RangedAdaptable<T> extends FlexibleAdaptable<T> {
    /**
     * Get the index of the filter that distinguishes range headings from individual items. 
     * @return "head" item filter index
     */
    public int getHeadItemFilterIndex();
    
    /**
     * Recomputes group membership throughout the container.
     */
    public void updateRangeClassification();
    
    /**
     * Recomputes group population throughout the container (assuming that group membership has not changed).
     */
    public void updateRangePopulation();

	/**
	 * Locate an aggregate item and return its child count.
	 * @param sourceElementIndex index of the item within the lookup selection
	 * @param sourceFilterIndex selection index to look up the item in
	 * @param targetFilterIndex selection index to count children in
	 * @return child count of the item, or 0 if the item is not found or not aggregate
	 */
    public int getChildCount(int sourceElementIndex, int sourceFilterIndex, int targetFilterIndex);

	/**
	 * Get the child count of an item.
	 * @param item item to look up for
	 * @param targetFilterIndex selection index to count children in
	 * @return child count of the item, or 0 if the item is not found or not aggregate
	 */
    public int getChildCount(T item, int targetFilterIndex);
}
