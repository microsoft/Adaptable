/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import junit.framework.Assert;

import java.util.Iterator;

/**
 * Through access to package methods.
 */
public class Validation {
	public static <T> void validateIntegrity(FlexibleAdaptable<T> asl) {
	    ((AdaptableSkipList<T>) asl).validateIntegrity();
	}
	
	public static <T> void validateIterators(FlexibleAdaptable<T> adaptable) {
		for (int filterIndex = 0; filterIndex < adaptable.getFilterCount(); ++filterIndex) {
			Iterator<T> iterator = adaptable.iterator(filterIndex);
			for (int elementIndex = 0; elementIndex < adaptable.size(filterIndex); ++ elementIndex) {
				Assert.assertSame(adaptable.get(filterIndex, elementIndex), iterator.next());
			}
		}
	}
	
	public static <T> void validateIterators(AdaptableSkipList<T> adaptable) {
		for (int filterIndex = 0; filterIndex < adaptable.getFilterCount(); ++filterIndex) {
			Iterator<T> cherryIterator = adaptable.cherryIterator(filterIndex);
			Iterator<T> ladderIterator = adaptable.ladderIterator(filterIndex);
			Iterator<T> walkerIterator = adaptable.walkerIterator(filterIndex);
			for (int elementIndex = 0; elementIndex < adaptable.size(filterIndex); ++ elementIndex) {
				final T expected = adaptable.get(filterIndex, elementIndex);
				Assert.assertSame("cherryIterator", expected, cherryIterator.next());
				Assert.assertSame("walkerIterator", expected, walkerIterator.next());
				Assert.assertSame("ladderIterator", expected, ladderIterator.next());
			}
		}
	}
}
