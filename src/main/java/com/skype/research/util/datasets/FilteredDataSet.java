/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.datasets;

import com.skype.research.util.adaptable.Adaptable;
import com.skype.research.util.adaptable.RangedAdaptable;
import com.skype.research.util.model.DataSet;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * A data set connected to an Adaptable, with optional ancillaries connected to the same Adaptable.
 */
public class FilteredDataSet<T> extends AbstractList<T> implements DataSet<T> {
    private int filterIndex;
    private int[] ancillaries;
    private Adaptable<T> adaptable;
    private RangedAdaptable<T> ranged;
    
    public FilteredDataSet(Adaptable<T> adaptable, int filterIndex) {
        this.adaptable = adaptable;
        this.ranged = adaptable instanceof RangedAdaptable ? (RangedAdaptable<T>) adaptable : null;
        this.filterIndex = filterIndex;
    }

    public void setAncillaries(int... ancillaries) {
        this.ancillaries = ancillaries;
    }
    
    @Override
    public int getCount() {
        return adaptable.size(getFilterIndex());
    }

    @Override
    public T getItem(int index) {
        return adaptable.get(getFilterIndex(), index);
    }

    @Override
    public DataSet<T> getAncillaryDataSet(int ancillaryIndex) {
        return hasAncillaries() ? null : dataSet(getAncillaryFilterIndex(ancillaryIndex));
    }

    @Override
    public int indexOf(Object element) {
        //noinspection unchecked
        return adaptable.indexOf(getFilterIndex(), (T) element);
    }

    @Override
    public int toAncillary(int ancillaryIndex, int elementIndex, boolean ceiling) {
        return adaptable.convertIndex(elementIndex, getFilterIndex(), getAncillaryFilterIndex(ancillaryIndex), ceiling);
    }

    @Override
    public int fromAncillary(int ancillaryIndex, int elementIndex, boolean ceiling) {
        return adaptable.convertIndex(elementIndex, getAncillaryFilterIndex(ancillaryIndex), getFilterIndex(), ceiling);
    }

	@Override
	public int getChildCount(T item) {
		return ranged == null ? 0 : ranged.getChildCount(item, getFilterIndex());
	}

    @Override
    public List<T> asList() {
        return this;
    }

    protected int getFilterIndex() {
        return filterIndex;
    }

    protected boolean hasAncillaries() {
        return ancillaries == null;
    }

    protected int getAncillaryFilterIndex(int ancillaryIndex) {
        return ancillaries[ancillaryIndex];
    }

    protected DataSet<T> dataSet(int filterIndex) {
        return new FilteredDataSet<T>(adaptable, filterIndex);
    }

    @Override
    public T get(int location) {
        return getItem(location);
    }

    @Override
    public Iterator<T> iterator() {
        return adaptable.iterator(getFilterIndex());
    }

    @Override
    public int size() {
        return getCount();
    }
}
