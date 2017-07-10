/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.unique;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * No hint needed to create elements.
 */
public class UniqueSequential<T> implements Registry<T> {
	final Lock lock = new ReentrantLock();
	final List<T> list = new ArrayList<T>();
	final Registry<T> source;

	public UniqueSequential(final Registry<T> source) {
		this.source = source;
	}

	@Override
	public T get(int id) {
		try {
			lock.lock();
			T element;
			if (growLocked(id)) {
				element = list.get(id);
				if (element != null) {
					return element;
				}
			}
			element = source.get(id);
			list.set(id, element);
			return element;
		} finally {
			lock.unlock();
		}
	}

	private boolean growLocked(int id) {
		boolean withinExisting = true;
		while (id >= list.size()) {
			list.add(null);
			withinExisting = false;
		}
		return withinExisting;
	}
}
