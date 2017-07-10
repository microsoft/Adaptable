/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptation;

import com.skype.research.util.adaptable.Adaptable;
import com.skype.research.util.adaptable.AdaptableFactory;
import com.skype.research.util.adaptable.Dump;
import com.skype.research.util.adaptable.ElementObserver;
import com.skype.research.util.adaptable.FlexibleAdaptable;
import com.skype.research.util.adaptable.mocks.DivisibleBy;
import com.skype.research.util.adaptable.mocks.IntValue;
import com.skype.research.util.model.DataSet;
import com.skype.research.util.model.RecyclerAdapter;
import com.skype.research.util.model.Subscriber;
import com.skype.research.util.primitives.Update;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Test identification and routing of incremental positional updates.
 */
public class TestUpdates extends TestCase {
	
	static class Representation<T> implements Subscriber<T>, RecyclerAdapter {
		final Adaptable<T> adaptable;
		final int filterIndex;
		final List<T> selection = new ArrayList<T>();
		
		Representation(Adaptable<T> source, int filterIndex) {
			this.adaptable = source;
			this.filterIndex = filterIndex;
		}
		
		T getItem(int position) {
			return adaptable.get(filterIndex, position);
		}
		
		@Override
		public void notifyItemChanged(int position) {
			selection.set(position, getItem(position));
		}
		
		@Override
		public void notifyItemInserted(int position) {
			selection.add(position, getItem(position));
		}
		
		@Override
		public void notifyItemMoved(int fromPosition, int toPosition) {
			throw new UnsupportedOperationException(); // should not be triggered
		}
		
		@Override
		public void notifyItemRangeInserted(int positionStart, int itemCount) {
			for (int position = 0; position < itemCount; ++position, ++positionStart) {
				selection.add(positionStart, getItem(positionStart));
			}
		}
		
		@Override
		public void notifyItemRangeChanged(int positionStart, int itemCount) {
			int positionEnd = positionStart + itemCount;
			for (int position = positionStart; position < positionEnd; ++position) {
				notifyItemChanged(position);
			}
		}
		
		@Override
		public void notifyItemRangeRemoved(int positionStart, int itemCount) {
			for (int position = 0; position < itemCount; ++position) {
				selection.remove(positionStart);
			}
		}
		
		@Override
		public void notifyItemRemoved(int position) {
			selection.remove(position);
		}
		
		void validate() {
			final int size = adaptable.size(filterIndex);
			Assert.assertEquals("Size[" + filterIndex + "] matches", size, selection.size());
			Iterator<T> refIterator = adaptable.iterator(filterIndex);
			Iterator<T> ourIterator = selection.iterator();
			int pos = 0;
			while (refIterator.hasNext()) {
				Assert.assertSame("Contents[" + filterIndex + "][" + (pos++) + "] match",
						refIterator.next(), ourIterator.next());
			}
		}
		
		void assertClear() {
			Assert.assertEquals("Clear[" + filterIndex + "]", 0, selection.size());
		}
		
		@Override
		public void setDataSet(DataSet<T> dataSet) {
			Assert.assertEquals("The original assigned data set is empty", 0, dataSet.getCount());
		}
		
		@Override
		public void notifyDataSetChanged() {
			throw new AssertionFailedError("No all-in-one update is expected");
		}
		
		@Override
		public List<? extends Subscriber<T>> getAncillarySubscribers() {
			return Collections.emptyList();
		}
	};
	
	/**
	 * Get ready for RecyclerView$Adapter
	 * @throws Exception
	 */
	public void testPositionalUpdates() throws Exception {
		AdaptableFactory<IntValue> builder = new AdaptableFactory<IntValue>();
		builder.setComparator(new Comparator<IntValue>() {
			@Override
			public int compare(IntValue lhs, IntValue rhs) {
				int lhs1 = lhs.getValue();
				int rhs1 = rhs.getValue();
				return lhs1 < rhs1 ? -1 : (lhs1 == rhs1 ? 0 : 1);
			}
		});
		for (int i = 1, divisor = 1; i <= 8; ++i) {
			divisor<<=1;
			divisor++;
			builder.addFilter(new DivisibleBy(divisor));
		}
		FlexibleAdaptable<IntValue> adaptable = builder.create();
		final int filterCount = adaptable.getFilterCount();
		final Adaptation<IntValue> adaptation = new Adaptation<IntValue>(adaptable, false);
		final List<Representation<IntValue>> reflected = new ArrayList<Representation<IntValue>>();
		for (int filterIndex = 0; filterIndex < filterCount; ++filterIndex) {
			Representation<IntValue> selectionRepresentation = new Representation<IntValue>(adaptable, filterIndex);
			adaptation.subscribe(filterIndex, selectionRepresentation);
			reflected.add(selectionRepresentation);
		}
		adaptable.addElementObserver(new ElementObserver<IntValue>() {
			@Override
			public void onElementUpdated(IntValue element, int[] position, int[] changeEstimate, int deltaSign, int[] deltaCount) {
				validateReflection(filterCount, reflected);
			}
		});
		final Random random = new Random(1 << 11);
		for (int i = 0; i < 1 << 11; ++i) {
			adaptable.add(new IntValue(random.nextInt(1 << 15)));
		}
		for (int i = 0; i < 1 << 14; ++i) {
			validateReflection(filterCount, reflected);
			final int randomValue = random.nextInt(1 << 15);
			final IntValue randomLocator = new IntValue(randomValue);
			switch (random.nextInt(4)) {
				case 0:
					adaptable.add(randomLocator);
					if (random.nextInt(1 << 6) == 0) {
						validateReflection(filterCount, reflected);
					}
					break;
				case 1:
					adaptable.updateReorder(randomLocator, new Update<IntValue>() {
						@Override
						public boolean apply(IntValue element) {
							final int updatedValue = random.nextInt(4) == 0 ? randomValue : random.nextInt(1 << 15);
							boolean changed = element.getValue() != updatedValue;
							element.setValue(updatedValue);
							return changed;
						}
					});
					if (random.nextInt(1 << 6) == 0) {
						validateReflection(filterCount, reflected);
					}
					break;
				case 2:
					adaptable.remove(randomLocator);
					if (random.nextInt(1 << 6) == 0) {
						validateReflection(filterCount, reflected);
					}
					break;
				case 3:
					final int randomFilter = random.nextInt(filterCount);
					final int size = adaptable.size(randomFilter);
					if (size > 0) {
						adaptable.remove(randomFilter, random.nextInt(size));
					}
					if (random.nextInt(1 << 6) == 0) {
						validateReflection(filterCount, reflected);
					}
					break;
			}
			if (random.nextInt(1 << 12) == 0) {
				adaptable.clear();
				for (int filterIndex = 0; filterIndex < filterCount; ++filterIndex) {
					reflected.get(filterIndex).assertClear();
				}
			}
			if (random.nextInt(1 << 12) == 0) {
				adaptable.setComparator(new Comparator<IntValue>() {
					final int mask = random.nextInt();
					
					@Override
					public int compare(IntValue lhs, IntValue rhs) {
						int lhs1 = lhs.getValue() ^ mask;
						int rhs1 = rhs.getValue() ^ mask;
						return lhs1 < rhs1 ? -1 : (lhs1 == rhs1 ? 0 : 1);
					}
				});
				adaptable.setAll(adaptable);
			}
			if (random.nextInt(1 << 8) == 0) {
				Dump.validateIntegrity(adaptable);
				validateReflection(filterCount, reflected);
			}
		}
	}
	
	protected static void validateReflection(int filterCount, List<Representation<IntValue>> reflected) {
		for (int filterIndex = 0; filterIndex < filterCount; ++filterIndex) {
			reflected.get(filterIndex).validate();
		}
	}
}
