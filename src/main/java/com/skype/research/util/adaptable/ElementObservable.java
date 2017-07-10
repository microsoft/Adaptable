/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

/**
 * Allows subscriptions for item updates.
 */
public interface ElementObservable<T> {
	public void addElementObserver(ElementObserver<? super T> observer);
	public void removeElementObserver(ElementObserver<? super T> observer);
}
