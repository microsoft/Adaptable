/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.model;

/**
 * Common data source. Converts filters to data sets.
 */
public interface Publisher<F, T> {
    
    /**
     * Request a given representation of underlying data.
     * @param filterSpec representation specifier.
     * @param callback subscriber instance to receive updates.
     */
    public void subscribe(F filterSpec, Subscriber<T> callback);
    
    /**
     * Detaches a subscriber. After a subscriber is detached, 
     * its {@link DataSet} integrity guarantees are no longer held.
     * @param callback subscriber instance to stop being notified of updates.
     */
    public void unsubscribe(Subscriber<T> callback);
}
