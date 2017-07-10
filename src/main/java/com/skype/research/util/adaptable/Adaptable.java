/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

/** Container with the following properties:
 * - ordered at model level (the universe)
 * - filtered at view level, preserving order
 * - random access by element position within any view, including the universe, is O(log N)
 * - per-element modification operations are as granular as possible and come at O(log N) too
 * 
 * Methods with "filterIndex" argument treat element index and element count 
 * in the context of the specified filtered selection ("view").
 */
@SuppressWarnings("JavadocReference")
public interface Adaptable<T> extends Iterable<Map.Entry<T, int[]>>,
		ElementEditor<T>,
		ElementObservable<T>,
		BulkUpdatable {
	/**
	 * Return the total filter count, aka the count of selections instantly maintained by this container.
	 * Cannot change during the lifetime of the container.
	 * @return the number of filters specified at construction/build time.
	 */
	int getFilterCount();
	
	/**
	 * Return the index of the special filter to return *all* elements,
	 * as opposed to other filters that may return incomplete selections.
	 * The container implementation need not expose such a filter, in general,
	 * and does not use it internally. However, it might be easier
	 * from the design standpoint to ensure that such filter exists (and is number 0).
	 * @return the index of the trivial return-all filter.
	 */
	int getUniverseFilterIndex();
	
	/**
	 * The size of the universe represented by this container, i.e. the total number of elements stored.
	 * @return the total number of elements stored.
	 */
	int size();
	
	/**
	 * The size of a filtered selection represented by this container.
	 * @param filterIndex number of the filter (and, respectively, the selection)
	 * @return the current number of elements passing a given filter.
	 */
	int size(int filterIndex);
	
	/**
	 * Add an item to the container, if there is no such item yet or duplicates are allowed.
	 * The item will be placed according to the current comparison order 
	 * and filtered according to the current filters.
	 * @param element item to add
	 * @return whether the operation resulted in a modification of the container.
	 */
	boolean add(T element);
	
	/**
	 * Remove an item from the container.
	 * Elements are matched by comparison equality, NOT by identity.
	 * @param element item to remove.
	 * @return true if the item was found and has been removed, false otherwise.
	 */
	boolean remove(T element);
	
	/**
	 * Remove an item from the container by selection index and element index within the selection
	 * Use {@link #getUniverseFilterIndex()} as filterIndex to remove from the universe.
	 * (i.e. "remove the second red ball"). 
	 * @param filterIndex index of the selection in which the element will be looked up.
	 * @param elementIndex index of the element within the selection.
	 * @return true if the item was found and has been removed, false otherwise.
	 */
	boolean remove(int filterIndex, int elementIndex);
	
	/**
	 * Return iterator over a given selection.
	 * Use {@link #getUniverseFilterIndex()} as filterIndex to iterate over all stored elements.
	 * @param filterIndex index of the selection to iterate over.
	 * @return iterator that returns elements matching a specific filter.
	 */
	Iterator<T> iterator(int filterIndex);
	
	/**
	 * Convenience method: get element by index from the universe.
	 * Equivalent to #get(#getUniverseFilterIndex(), elementIndex).
	 * @param elementIndex index of the element to return.
	 * @return element to find.
	 */
	T get(int elementIndex);
	
	/**
	 * Get element by selection index and element index within the selection.
	 * @param filterIndex index of the selection in which the element will be looked up.
	 * @param elementIndex index of the element within the selection.
	 * @return the element at the specified position, or null if bounds are not satisfied.
	 */
	T get(int filterIndex, int elementIndex);
	int indexOf(T item);
	int indexOf(int filterIndex, T item);
	
	/**
	 * Overload of {@link #convertIndex(int, int, int, boolean)} with default policy ("floor").
	 * @param sourceElementIndex zero-based position number within the source view
	 * @param sourceFilterIndex index of the original filtered view (may be the universe)
	 * @param targetFilterIndex index of the target filtered view (may be the universe)
	 * @return converted index, as required by research.widget.SectionIndexer#getSectionForPosition
	 */
	int convertIndex(int sourceElementIndex, int sourceFilterIndex, int targetFilterIndex);
	
	/**
	 * Convert element index in a view to the index of the same element in another view.
	 * If the element does not pass the target filter, the behavior depends on the policy.
	 *
	 * @param sourceElementIndex zero-based position number within the source view
	 * @param sourceFilterIndex index of the original filtered view (may be the universe)
	 * @param targetFilterIndex index of the target filtered view (may be the universe)
	 * @param roundToCeiling if exact match is not found, return the next available element (otherwise return the previous one)
	 * @return such a convertedIndex that 
	 * {@link Adaptable#get(int sourceFilterIndex, int sourceElementIndex) == 
	 * {@link Adaptable#get(int targetFilterIndex, int convertedIndex)}}, at best effort.
	 * and the sourceElementIndex is outside the [0; {@link Adaptable#size(int sourceFilterIndex)}) range.
	 */
	int convertIndex(int sourceElementIndex, int sourceFilterIndex, int targetFilterIndex, boolean roundToCeiling);
	/**
	 * Delete all contents without broadcasting removal of any individual element.
	 * That this method breaks the usual {@link ElementObserver} contract in its 
	 * strict sense: <i>only one</i>
	 * {@link ElementObserver#onElementUpdated(Object, int[], int[], int, int[])} is
	 * broadcast, with null as element value, the total size as the estimate vector
	 * and -1 the total size as the deltaCount vector.
	 * Internal state: if there is a pending comparator, it is immediately applied.
	 */
	void clear();
	/**
	 * Replace all contents in bulk, without broadcasting update of any individual element.
	 * That this method breaks the usual {@link ElementObserver} contract in its 
	 * strict sense: <i>only one</i>
	 * {@link ElementObserver#onElementUpdated(Object, int[], int[], int, int[])} is
	 * broadcast, with null as element value, the total size as the estimate vector
	 * and -1 the total size as the deltaCount vector.
	 * Internal state: if there is a pending comparator, it is immediately applied.
	 * @param adaptable data source to copy from
	 */
	void setAll(Adaptable<T> adaptable);
	
	/**
	 * Add contents in bulk, without broadcasting update of any individual element.
	 * @param adaptable source of data to add
	 */
	void addAll(Adaptable<T> adaptable);
	
	/**
	 * Set the <b>next</b> comparator to apply when the data are explicitly cleared or reordered.
	 * @param pendingComparator comparator to be used upon a subsequent call to 
	 * {@link #setAll(Adaptable)} or {@link #clear()}.
	 */
	void setComparator(Comparator<? super T> pendingComparator);
	
	/**
	 * Get the <b>current</b> comparator (the one items are currently sorted according to).
	 * @return the active comparator.
	 */
	Comparator<? super T> getComparator();
	
	/**
	 * Controls auto-add mode: when a locator is not found, a modification is applied to old value
	 *  and the modified value is inserted in the container.
	 * @param autoAdd true to set auto-add on, false to turn off.
	 */
	void setAutoAdd(boolean autoAdd);
}
