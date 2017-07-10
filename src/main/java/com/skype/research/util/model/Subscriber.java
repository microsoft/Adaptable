/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.model;

import java.util.List;

/**
 * DataSet consumer.
 */
public interface Subscriber<T> {
    /**
     * Callback provided to {@link Publisher#subscribe}
     * @param dataSet data set instance to display and browse.
     */
    public void setDataSet(DataSet<T> dataSet);

    /**
     * Tell the subscriber that the data set contents have been updated,
     * without replacing the data set instance.
     */
    public void notifyDataSetChanged();

    /**
     * Ancillary subscribers to subscribe for ancillary data sets.
     * @return dependent subscribers, or an empty list if N/A.
     * @see DataSet#getAncillaryDataSet(int)
     */
    List<? extends Subscriber<T>> getAncillarySubscribers();
}
