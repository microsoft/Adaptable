/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.primitives.Filter;
import com.skype.research.util.primitives.Update;

import java.util.Comparator;

/**
 * Simple Adaptable-related constructs.
 */
public class Trivial {
	public static final int[] NO_ANCILLARY_SLOTS = new int[0];
	
	static final Comparator<Object> NATURAL_ORDER = new Comparator<Object>() {
        @Override
        public int compare(Object lhs, Object rhs) {
            if (lhs instanceof Comparable) {
                //noinspection unchecked
                return ((Comparable) lhs).compareTo(rhs);
            }
            throw new ClassCastException(lhs.getClass().getName());
        }
    };
    
    static final Filter<Object> ALL = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return true;
        }
    };
    
    static final Filter<Object> NONE = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return false;
        }
    };
	
	static final Update<Object> SUGGEST = new Update<Object>() {
		@Override
		public boolean apply(Object element) {
			return false;
		}
	};

	static final Update<Object> REFRESH = new Update<Object>() {
		@Override
		public boolean apply(Object element) {
			return true;
		}
	};

	public static <T> Comparator<? super T> naturalOrder() {
        return NATURAL_ORDER;
    }

    public static <T> Filter<? super T> universeFilter() {
        return ALL;
    }
    
    public static <T> Filter<? super T> placeholder() {
        return NONE;
    }

	public static <T> Update<? super T> suggest() {
		return SUGGEST;
	}

	public static <T> Update<? super T> refresh() {
		return REFRESH;
	}

}
