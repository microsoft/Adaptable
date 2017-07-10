/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.model;

import java.util.List;
import java.util.RandomAccess;

/**
 * Random-accessible, read-only list contract.
 * See android.widget.Adapter for explanation of methods.
 */
@SuppressWarnings("JavadocReference")
public interface DataSet<T> extends DataSource<T>, RandomAccess, Iterable<T> {
    // common
    public int getCount();
    public T getItem(int index);
    // extra logic shared with {@link SlotAllocator}
    public DataSet<T> getAncillaryDataSet(int ancillaryIndex);
    // reverse lookup
    public int indexOf(T element);
    // index conversion
    public int toAncillary(int ancillaryIndex, int elementIndex, boolean ceiling);
    public int fromAncillary(int ancillaryIndex, int elementIndex, boolean ceiling);
	// child count
	public int getChildCount(T item);
    // facility
    public List<T> asList();
}
