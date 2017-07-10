/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.datasets;

import com.skype.research.util.model.DataSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Returns no data at any circumstances, and returns data at no circumstances.
 */
public class EmptyDataSet<T> implements DataSet<T> {
	private static final DataSet instance = new EmptyDataSet();
	
	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public T getItem(int index) {
		throw new IndexOutOfBoundsException();
	}

	@Override
	public DataSet<T> getAncillaryDataSet(int ancillaryIndex) {
		return this;
	}

	@Override
	public int indexOf(T element) {
		return -1; // not found
	}

	@Override
	public int toAncillary(int ancillaryIndex, int elementIndex, boolean ceiling) {
		return -1; // not found
	}

	@Override
	public int fromAncillary(int ancillaryIndex, int elementIndex, boolean ceiling) {
		return -1; // not found
	}

	@Override
	public int getChildCount(T item) {
		return 0;
	}

	@Override
	public List<T> asList() {
		return Collections.emptyList();
	}

	@Override
	public Iterator<T> iterator() {
		return Collections.<T>emptySet().iterator();
	}
	
	public static <T> DataSet<T> emptyDataSet() {
		//noinspection unchecked
		return instance;
	}
}
