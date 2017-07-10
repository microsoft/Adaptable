/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.primitives.Update;
import com.skype.research.util.projection.CompositeProjector;
import com.skype.research.util.projection.CompositeProjectorImpl;
import com.skype.research.util.projection.Projector;
import com.skype.research.util.projection.ProjectorEditor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.skype.research.util.adaptable.Distance.isZero;
import static com.skype.research.util.adaptable.Distance.set;
import static com.skype.research.util.adaptable.Distance.sub;

/**
 * {@link Adaptable} implementation with a skip list container with tracked edge lengths.
 */
public class AdaptableSkipList<T> implements FlexibleAdaptable<T>, Projector<T> {

	final int levelCount;
	final int orbitLevel; // top, guaranteed to have only one link on it
	final int cloudLevel; // highest level allowed for real nodes
	//
	final int denominator;

	static final BitSet EMPTY = new BitSet();
	final Tracker doNotTrack = new Tracker();
	final Meter doNotMeasure = new Meter();
	
	// no use counting indices beyond it
	/* package */ int horizon;

	abstract class Locator {
		abstract boolean hasNext(Node node, int level);
		abstract Node next(int level);
		abstract boolean exactMatch();
	}
	
	final class ExactLocator extends Locator {
		final Node boundary;
		Node nextNode;
		
		ExactLocator(Node boundary) {
			this.boundary = boundary;
		}
		
		@Override
		final boolean hasNext(Node node, int level) {
			nextNode = node.nodes[level];
			return boundary != nextNode;
		}
		
		@Override
		final Node next(int level) {
			return nextNode;
		}
		
		final boolean exactMatch() {
			return true;
		}
	}
	
	abstract class AimingLocator extends Locator {
		int comparison;
		
		final boolean hasNext(Node node, int level) {
			comparison = evaluateNextStep(node, level);
			return comparison > 0;
		}
		
		abstract int evaluateNextStep(Node node, int level);
		
		final boolean exactMatch() {
			return comparison == 0;
		}
	}
	
	class ValueLocator extends AimingLocator {
		final T value;
		Node nextNode;
		public ValueLocator(T value) {
			this.value = value;
		}
		
		@Override
		final int evaluateNextStep(Node node, int level) {
			nextNode = node.nodes[level];
			return nextNode == null 
					? -1 // next is absMax
					: comparator.compare(value, nextNode.element);
		}
		
		@Override
		final Node next(int level) {
			return nextNode;
		}
	}
	
	class IndexLocator extends AimingLocator {
		final int filterIndex, elementIndex;
		int lookupIndex = -1, nextIndex;
		Node currentNode;
		
		IndexLocator(int filterIndex, int elementIndex) {
			this.filterIndex = filterIndex;
			this.elementIndex = elementIndex;
		}
		
		int evaluateNextStep(Node node, int level) {
			currentNode = node;
			nextIndex = lookupIndex + node.distances[level][filterIndex];
			return elementIndex - nextIndex;
		}
		
		@Override
		public Node next(int level) {
			lookupIndex = nextIndex;
			return currentNode.nodes[level];
		}
	}
	
	final class Navigator {
		Node node = absMinNode;
		int level = orbitLevel;
		
		final Tracker tracker;
		final Meter meter;
		
		public Navigator(Tracker tracker, Meter meter) {
			this.tracker = tracker;
			this.meter = meter;
		}
		
		private boolean navForward(Locator locator) {
			while (locator.hasNext(node, level)) {
				meter.addDistance(node, level);
				node = locator.next(level);
			}
			return locator.exactMatch();
		}
		
		private void markLevel() {
			tracker.setNextNode(level, node);
			meter.mark(this.tracker, level);
		}
		
		final boolean descend(Locator locator, boolean stopOnExactMatch) {
			markLevel();
			while (level > 0) {
				--level;
				if (navForward(locator) && stopOnExactMatch) {
					return true;
				}
				markLevel();
			}
			return false;
		}
		
		/**
		 * Descend to the node that contains a specific value recording predecessor nodes on each level.
		 * @param element value to find
		 * @return container node, or {@link #absMinNode} if the value is not found.
		 */
		final Node descendTo(T element) {
			// alternatively, we could inject Locator every time and decouple the two hierarchies
			final ValueLocator locator = new ValueLocator(element);
			if (descend(locator, true)) {
				// post-descend
				final Node found = locator.nextNode;
				descend(new ExactLocator(found), false);
				return found;
			}
			return absMinNode;
		}
		
		/**
		 * 
		 * @param filterIndex
		 * @param elementIndex
		 * @return
		 */
		final Node descendTo(int filterIndex, int elementIndex) {
			if (elementIndex >= 0 && elementIndex < size(filterIndex)) {
				final IndexLocator locator = new IndexLocator(filterIndex, elementIndex);
				descend(locator, false);
				return locator.currentNode.nodes[0];
			}
			return absMinNode;
		}
	}
	
	class Meter {
		int[] getPosition() { return zero; }
		void addDistance(Node node, int level) {}
		void mark(Tracker tracker, int level) {}
	}
	
	class ScalarMeter extends Meter {
		final int filterIndex;
		int position = -1;
		
		ScalarMeter(int filterIndex) {
			this.filterIndex = filterIndex;
		}
		
		@Override
		final void addDistance(Node node, int level) {
			position += node.distances[level][filterIndex];
		}
	}
	
	class VectorMeter extends Meter {
		final int[] position = newDistance();
		
		@Override
		final void addDistance(Node node, int level) {
			Distance.add(position, node.distances[level], horizon);
		}
		
		@Override
		final void mark(Tracker tracker, int level) {
			tracker.setDistance(level, position);
		}
		
		@Override
		final int[] getPosition() {
			return position;
		}
	}
	
	class Tracker {
		void setNextNode(int level, Node next) {}
		void setDistance(int level, int[] value) {}
	}
	
	class Section extends Tracker {
		// http://stackoverflow.com/questions/529085/how-to-create-a-generic-array-in-java
		final Node[] nodes = Node[].class.cast(Array.newInstance(Node.class, levelCount));
		
		@Override
		final void setNextNode(int level, Node next) {
			nodes[level] = next;
		}
	}

	class Gap extends Section {

		final int[][] distances = new int[levelCount][filterCount];

		@Override
		final void setDistance(int level, int[] value) {
			set(distances[level], value, horizon);
		}
	}
	
	protected int[] newDistance() {
		return new int[filterCount];
	}
	
	class Node extends Gap implements Map.Entry<T, int[]> {
		final T element;
		final int level;

		Node(T element, int nodeLevel) {
			this.element = element;
			this.level = nodeLevel;
		}

		@Override
		public final T getKey() {
			return element;
		}

		@Override
		public final int[] getValue() {
			return distances[0];
		}

		@Override
		public final int[] setValue(int[] object) {
			throw new UnsupportedOperationException(); // client should not overwrite filters
		}
	}
	
	// raw materials
	final Random random = new Random();

	// sub-products
	Comparator<? super T> comparator, pendingComparator;
	final int[] zero;

	// structural
	Node absMinNode;
	final int universeFilter;
	boolean allowDuplicates;
	boolean broadcastOldValue;
	boolean positionUnaware;
	boolean autoAdd;

	// multiple representations
	final CompositeProjector<T> projector;
	final int filterCount; // cached, uninitialized

	// observation
	final List<ElementObserver<? super T>> observers = new LinkedList<ElementObserver<? super T>>();

	public AdaptableSkipList(int levelCount, int denominator) {
		//noinspection unchecked
		this(levelCount, denominator, Trivial.naturalOrder(), new CompositeProjectorImpl<T>());
	}

	public AdaptableSkipList(int levelCount, int denominator, Comparator<? super T> comparator, CompositeProjectorImpl<T> projector) {
		//noinspection unchecked
		this(levelCount, denominator, comparator, 0, projector);
	}

	public AdaptableSkipList(int levelCount, int denominator, int universeFilter, CompositeProjectorImpl<T> projector) {
		//noinspection unchecked
		this(levelCount, denominator, Trivial.naturalOrder(), universeFilter, projector);
	}

	public AdaptableSkipList(int levelCount, int denominator, Comparator<? super T> comparator, int universeFilter, CompositeProjector<T> projector) {
		this.levelCount = levelCount;
		orbitLevel = levelCount - 1;
		cloudLevel = orbitLevel - 1;
		this.denominator = denominator;
		this.comparator = comparator;
		this.pendingComparator = comparator;
		this.universeFilter = universeFilter;
		this.projector = projector;
		this.filterCount = projector.getFilterCount();
		this.horizon = projector.getHorizon();
		zero = newDistance();
		absMinNode = new Node(null, orbitLevel);
	}

	/**
	 * Ensure determinism. Good for unit testing and other repeatable scenarios.
	 * @param seed internal random number generator seed
	 */
	public void setSeed(long seed) {
		random.setSeed(seed);
	}

	public void setAllowDuplicates(boolean allowDuplicates) {
		this.allowDuplicates = allowDuplicates;
	}

	/**
	 * If set to true, rank-preserving updates in {@link #updateReorder(Object, Update)}
	 * broadcast a full remove-insert in order to deliver both the old and the new value
	 * of the item being edited.
	 * 
	 * If set to false, a single "updated" is broadcast in this case, with the new value.
	 * 
	 * @param broadcastOldValue true to guarantee old value broadcasting,
	 *                        false to send a single "changed" update.
	 */
	public void setBroadcastOldValue(boolean broadcastOldValue) {
		this.broadcastOldValue = broadcastOldValue;
	}
	
	/**
	 * If set to true, add/update/delete positions are not computed and are instead set to 0.
	 * Useful if clients can only update their datasets in whole (as opposed to per-element).
	 * 
	 * @param positionUnaware suppress delivery of incremental updates and only deliver bulk updates
	 */
	public void setPositionUnaware(boolean positionUnaware) {
		this.positionUnaware = positionUnaware;
	}
	
	protected void onElementUpdated(T element, int[] position, int[] estimate, int deltaSign, int[] deltaCount) {
		if (!observers.isEmpty()){
			for (ElementObserver<? super T> observer : observers) {
				observer.onElementUpdated(element, position, estimate, deltaSign, deltaCount);
			}
		}
	}

	/**
	 * Implements container-specific projection logic.
	 * By default, condition evaluation is delegated to the {@link Projector} provided upon construction
	 * or assembled part by part with {@link AdaptableFactory}.
	 * However, specializations of the container may introduce their own state-dependent logic,
	 * such as "failing" or "passing" a group item based on whether the group is empty or non-empty.
	 * 
	 * (Example: allow the alphabet caption "A" in a selection 
	 * if, and only if, names starting on "A" are present in the same selection.)
	 * 
	 * Direct use by the client is allowed but only makes sense in complicated scenarios
	 * (for instance, to evaluate the impact of element addition 
	 * or removal before actually adding or removing an element). 
	 * 
	 * @param element element to evaluate.
	 * @param filterIndex condition index to evaluate.
	 * @param precomputed pre-evaluated conditions (potential dependencies of the current one)
	 * @return true if the element passes the condition, false otherwise.
	 */
	@Override
	public boolean accept(T element, int filterIndex, int[] precomputed) {
		return projector.accept(element, filterIndex, precomputed);
	}
	
	@Override
	public int size() {
		return size(universeFilter);
	}

	@Override
	public int size(int filterIndex) {
		return absMinNode.distances[orbitLevel][filterIndex];
	}

	@Override
	public int getFilterCount() {
		return filterCount;
	}

	final int compareWithNextNode(T prev, Node next) {
		return prev == null || next == null 
				? -1 // prev is absMin or next is absMax
				: comparator.compare(prev, next.element);
	}

	private int randomLevel() {
		int level = 0;
		while (level < cloudLevel && random.nextInt(denominator) == 0) {
			++level;
			// "fix-up" optimization: never grow up faster than one level at a time.
			if (absMinNode.nodes[level] == null) {
				break;
			}
		}
		return level;
	}
	
	@Override
	public boolean add(T element) {
		return addPrecomputedDistance(element, project(element));
	}

	private boolean addPrecomputedDistance(T element, int[] projection) {
		// insert sorted
		final Locator locator = new ValueLocator(element);
		final VectorMeter meter = new VectorMeter();

		Gap tracker = new Gap();
		final Navigator navigator = new Navigator(tracker, meter);
		if (navigator.descend(locator, !allowDuplicates)) {
			return false;
		}
		finishAddition(element, projection, meter, tracker);
		return true;
	}
	
	private void finishAddition(T element, int[] projection, VectorMeter meter, Gap tracker) {
		int level = 0;
		final int[] position = meter.getPosition();
		int[] ceiling = set(newDistance(), position, horizon);
		Distance.add(ceiling, projection, horizon);
		final int nodeLevel = randomLevel();
		final int[] temp = newDistance();
		Node inserted = new Node(element, nodeLevel);
		boolean split = true;
		do {
			Node prev = tracker.nodes[level];
			Distance.add(prev.distances[level], projection, horizon);
			if (split &= level <= nodeLevel) {
				// connections
				Node next = prev.nodes[level];
				inserted.setNextNode(level, next);
				prev.setNextNode(level, inserted);
				// edge lengths
				sub(set(temp, ceiling, horizon), tracker.distances[level], horizon);
				sub(set(inserted.distances[level], prev.distances[level], horizon), temp, horizon);
				set(prev.distances[level], temp, horizon);
			}
		} while (++level <= orbitLevel);
		onElementUpdated(element, position, projection, 1, projection);
	}
	
	private boolean removeNodeAtSection(Section section, Node container, int[] position) {
		if (container == absMinNode) {
			return false;
		}
		final int[] oldEdge = set(newDistance(), section.nodes[0].distances[0], horizon);
		adjustDistance(section, -1, oldEdge);
		finishRemoval(section, container);
		onElementUpdated(container.element, position, oldEdge, -1, oldEdge);
		return true;
	}
	
	private void finishRemoval(Section section, Node container) {
		for (int level = orbitLevel; level >= 0; --level) {
			Node prev = section.nodes[level];
			if (prev.nodes[level] == container) {
				// merge idiom
				Distance.add(prev.distances[level], container.distances[level], horizon);
				prev.setNextNode(level, container.nodes[level]);
			}
		}
	}
	
	@Override
	public boolean remove(T element) {
		Section tracker = new Section();
		Meter meter = allocateMeterForReporting();
		final Navigator navigator = new Navigator(tracker, meter);
		Node node = navigator.descendTo(element);
		return removeNodeAtSection(tracker, node, meter.getPosition());
	}
	
	@Override
	public boolean remove(int filterIndex, int elementIndex) {
		final Section section = new Section();
		final Meter meter = allocateMeterForReporting();
		final Navigator navigator = new Navigator(section, meter);
		final Node node = navigator.descendTo(filterIndex, elementIndex);
		return removeNodeAtSection(section, node, meter.getPosition());
	}

	@Override
	public void clear() {
		int[] size = absMinNode.distances[orbitLevel];
		comparator = pendingComparator;
		horizon = projector.getHorizon();
		absMinNode = new Node(null, orbitLevel);
		onElementUpdated(null, zero, size, -1, size);
	}

	@Override
	public T get(int filterIndex, int elementIndex) {
		final Navigator navigator = new Navigator(doNotTrack, doNotMeasure);
		Node node = navigator.descendTo(filterIndex, elementIndex);
		return node.element;
	}

	@Override
	public int indexOf(T item) {
		return indexOf(universeFilter, item);
	}

	@Override
	public int indexOf(int filterIndex, T element) {
		final ValueLocator locator = new ValueLocator(element);
		final ScalarMeter meter = new ScalarMeter(filterIndex);
		final Tracker tracker = doNotTrack;
		final Navigator navigator = new Navigator(tracker, meter);
		if (navigator.descend(locator, true)) {
			final Node found = locator.nextNode;
			navigator.descend(new ExactLocator(found), false);
			final Node predecessor = navigator.node;
			final int projection = predecessor.distances[0][filterIndex];
			return projection == 0 ? -1 : meter.position + projection;
		}
		return -1;
	}
	
	@Override
	public int convertIndex(int sourceElementIndex, int sourceFilterIndex, int targetFilterIndex) {
		return convertIndex(sourceElementIndex, sourceFilterIndex, targetFilterIndex, false);
	}

	@Override
	public int convertIndex(int sourceElementIndex, int sourceFilterIndex, int targetFilterIndex, boolean ceiling) {
		int targetSelectionSize = size(targetFilterIndex);
		int targetElementIndex;
		if (sourceFilterIndex == targetFilterIndex) {
			targetElementIndex = sourceElementIndex; // no validation
		} else {
			if (sourceElementIndex < 0) {
				targetElementIndex = -1;
			} else if (sourceElementIndex >= size(sourceFilterIndex)) {
				targetElementIndex = targetSelectionSize;
			} else {
				final IndexLocator locator = new IndexLocator(sourceFilterIndex, sourceElementIndex);
				final ScalarMeter meter = new ScalarMeter(targetFilterIndex);
				final Navigator navigator = new Navigator(doNotTrack, meter);
				navigator.descend(locator, false);
				final int projection = locator.currentNode.distances[0][targetFilterIndex];
				targetElementIndex = meter.position + (ceiling ? 1 : projection);
			}
		}
		return targetElementIndex;
	}
	
	@Override
	public T get(int elementIndex) {
		return get(universeFilter, elementIndex);
	}

	@Override
	public boolean updateInPlace(T oldValue, Update<? super T> modification) {
		Section tracker = autoAdd ? new Gap() : new Section();
		final Meter meter = allocateMeterForAutoAdd();
		final Navigator navigator = new Navigator(tracker, meter);
		Node container = navigator.descendTo(oldValue);
		if (container == absMinNode) {
			if (autoAdd) {
				modification.apply(oldValue);
				finishAddition(oldValue, project(oldValue), (VectorMeter) meter, (Gap) tracker);
				return true;
			}
			return false;
		}
		if (modification.apply(container.element)) {
			onElementUpdated(container.element, meter.getPosition(), tracker.nodes[0].distances[0], 0, zero);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateFilters(T oldValue, Update<? super T> modification) {
		Section tracker = autoAdd ? new Gap() : new Section();
		final Meter meter = allocateMeterForAutoAdd();
		final Navigator navigator = new Navigator(tracker, meter);
		Node container = navigator.descendTo(oldValue);
		if (container == absMinNode) {
			if (autoAdd) {
				modification.apply(oldValue);
				finishAddition(oldValue, project(oldValue), (VectorMeter) meter, (Gap) tracker);
				return true;
			}
			return false;
		}
		T element = container.element;
		int[] oldEdge = tracker.nodes[0].distances[0];
		boolean modified = modification.apply(element);
		if (modified) {
			int[] changeEstimate = project(element);
			int[] deltaCount = sub(set(newDistance(), changeEstimate, horizon), oldEdge, horizon);
			Distance.add(changeEstimate, oldEdge, horizon);
			adjustDistance(tracker, 1, deltaCount);
			onElementUpdated(element, meter.getPosition(), changeEstimate, 1, deltaCount);
		}
		return modified;
	}

	@Override
	public boolean updateReorder(T oldValue, Update<? super T> modification) {
		Section tracker = autoAdd ? new Gap() : new Section();
		Meter meter = allocateMeterForAutoAdd();
		final Navigator navigator = new Navigator(tracker, meter);
		Node container = navigator.descendTo(oldValue);
		if (container == absMinNode) {
			if (autoAdd) {
				boolean modified = modification.apply(oldValue);
				if (modified) {
					Node prevNode = tracker.nodes[0];
					if ((prevNode != absMinNode && compareWithNextNode(oldValue, prevNode) < 0)
							|| compareWithNextNode(oldValue, prevNode.nodes[0]) > 0) {
						addPrecomputedDistance(oldValue, project(oldValue));
						return true;
					}
				}
				finishAddition(oldValue, project(oldValue), (VectorMeter) meter, (Gap) tracker);
				return true;
			}
			return false;
		}
		// optimized position-aware remove
		int[] oldEdge = Distance.set(newDistance(), tracker.nodes[0].distances[0], horizon);
		adjustDistance(tracker, -1, oldEdge);
		T element = container.element;
		if (broadcastOldValue) {
			onElementUpdated(element, meter.getPosition(), oldEdge, -1, oldEdge);
		}
		boolean modified = modification.apply(element);
		boolean diffRank = modified
				&& (compareWithNextNode(tracker.nodes[0].element, container) > 0
				|| compareWithNextNode(element, container.nodes[0]) > 0);
		if (diffRank) {
			if (!broadcastOldValue) {
				onElementUpdated(element, meter.getPosition(), oldEdge, -1, oldEdge);
			}
			// finish removal
			finishRemoval(tracker, container);
			add(element);
		} else {
			// voila, order preserved!
			final int[] projection = project(element);
			adjustDistance(tracker, 1, projection);
			if (!broadcastOldValue) {
				sub(projection, oldEdge, horizon);
			}
			onElementUpdated(element, meter.getPosition(), projection, 1, projection);
		}
		return modified;
	}
	
	private Meter allocateMeterForReporting() {
		return positionUnaware || observers.isEmpty() ? doNotMeasure : new VectorMeter();
	}
	
	private Meter allocateMeterForAutoAdd() {
		return autoAdd ? new VectorMeter() : allocateMeterForReporting();
	}
	
	private int[] project(T oldValue) {
		return Distance.project(newDistance(), oldValue, horizon, this);
	}
	
	@Override
	public void setAutoAdd(boolean autoAdd) {
		this.autoAdd = autoAdd;
	}

	private void adjustDistance(Section previousNodes, int deltaSize, int[] deltaCount) {
		if (deltaSize != 0 && !Distance.isZero(deltaCount, horizon)) {
			for (int level = orbitLevel; level >= 0; --level) {
				Distance.add(previousNodes.nodes[level].distances[level], deltaSize, deltaCount, horizon);
			}
		}
	}

	@Override
	public BitSet refreshFilters(BitSet dirtyMask) {
		if (dirtyMask.cardinality() == 0) {
			return EMPTY;
		}
		horizon = projector.getHorizon();
		hintBulkOpBegin();
		final BitSet retVal;
		if (dirtyMask.cardinality() == 1) {
			int affectedElements = doRefreshFilters(dirtyMask.nextSetBit(0));
			retVal = affectedElements == 0 ? EMPTY : dirtyMask;
		} else {
			retVal = doRefreshFilters(dirtyMask);
		}
		hintBulkOpCompleted();
		return retVal;
	}
	
	@Override
	public void setComparator(Comparator<? super T> pendingComparator) {
		this.pendingComparator = pendingComparator;
	}
	
	@Override
	public Comparator<? super T> getComparator() {
		return comparator;
	}
	
	@Override
	public Iterator<Map.Entry<T, int[]>> iterator() {
		return new Iterator<Map.Entry<T, int[]>>() {
			Node node = absMinNode;

			@Override
			public boolean hasNext() {
				return node.nodes[0] != null;
			}

			@Override
			public Map.Entry<T, int[]> next() {
				node = node.nodes[0];
				return node;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException(); // no use case yet
			}
		};
	}

	@Override
	public Iterator<T> iterator(final int filterIndex) {
		int filteredSize = size(filterIndex);
		if (filteredSize < levelCount) {
			return cherryIterator(filterIndex);
		} else {
			int universeSize = size();
			// ladder beats walker starting from 1/d factor
			if (filteredSize < universeSize / denominator) {
				return ladderIterator(filterIndex);
			} else {
				return walkerIterator(filterIndex);
			}
		}
	}

	// factory methods exposed for unit testing / benchmarking
	protected Iterator<T> cherryIterator(int filterIndex) {
		return new CherryIterator(filterIndex);
	}

	protected Iterator<T> ladderIterator(int filterIndex) {
		return new LadderIterator(filterIndex);
	}

	protected Iterator<T> walkerIterator(int filterIndex) {
		return new WalkerIterator(filterIndex);
	}

	@Override
	public void setAll(Adaptable<T> source) {
		doAddAll(source, true);
	}
	
	@Override
	public void addAll(Adaptable<T> source) {
		doAddAll(source, source == this);
	}

	private void doAddAll(Adaptable<T> source, boolean dropExisting) {
		hintBulkOpBegin();
		if (source.getFilterCount() != getFilterCount()) {
			throw new IllegalArgumentException("Incompatible source!");
		}
		Iterator<Map.Entry<T, int[]>> iterator = source.iterator();
		if (dropExisting) {
			// clear silently, preserving iterator.
			clear();
		}
		while (iterator.hasNext()) {
			Map.Entry<T, int[]> node = iterator.next();
			addPrecomputedDistance(node.getKey(), node.getValue());
		}
		hintBulkOpCompleted();
	}

	private BitSet doRefreshFilters(BitSet mask) {
		if (size() == 0 || mask.cardinality() == 0) {
			// skip for empty containers
			return EMPTY;
		}
		Gap tracker = new Gap();
		Node node = absMinNode;
		Node nextNode;
		Node prevNode;
		int[] selectionIndex = newDistance();
		int[] projection = newDistance();
		for (int level = 0; level < levelCount; ++level) {
			tracker.setNextNode(level, node);
		}
		boolean moreData;
		final int[] indices = Distance.toArray(mask);
		final int[] deltaCount = newDistance();
		T element;
		do {
			set(projection, node.distances[0], horizon);
			nextNode = node.nodes[0];
			moreData = nextNode != null;
			if (moreData) {
				element = nextNode.element;
				Distance.set(deltaCount, projection, indices);
				Distance.project(projection, element, indices, this);
				Distance.sub(deltaCount, projection, indices);
				if (!isZero(deltaCount, horizon)) {
					onElementUpdated(element, selectionIndex, deltaCount, -1, deltaCount);
				}
				Distance.add(selectionIndex, projection, horizon);
			}
			for (int level = 0; level < levelCount; ++level) {
				prevNode = tracker.nodes[level];
				if (prevNode.nodes[level] == nextNode) {
					sub(set(projection, selectionIndex, indices), tracker.distances[level], horizon);
					set(prevNode.distances[level], projection, indices);
					set(tracker.distances[level], selectionIndex, indices);
					tracker.setNextNode(level, nextNode);
				}
			}
			node = nextNode;
		} while (moreData);
		return mask;
	}

	// optimized single-filter version
	// 
	// the traversal logic is similar to WalkerIterator, and can be further optimized
	// if both selections are narrow enough.
	// however, updateFilters(...) targeted by filterIndex should be considered first.
	// 
	private int doRefreshFilters(int filterIndex) {
		if (size() == 0) {
			return 0;
		}
		// we don't use a Gap because we only recompute indices of a single selection
		Section section = new Section();
		int[] lastIndex = new int[levelCount];
		Node node = absMinNode;
		Node nextNode;
		Node prevNode;
		int selectionIndex = 0;
		for (int level = 0; level < levelCount; ++level) {
			section.setNextNode(level, node);
		}
		boolean moreData;
		final int[] vPosition = newDistance();
		final int[] deltaCount = newDistance();
		T element;
		int[] distance;
		int delta;
		do {
			nextNode = node.nodes[0];
			moreData = nextNode != null;
			if (moreData) {
				element = nextNode.element;
				distance = node.distances[0];
				delta = -distance[filterIndex];
				if (accept(element, filterIndex, distance)) {
					++delta;
					++selectionIndex;
				}
				if (delta != 0) {
					deltaCount[filterIndex] = delta;
					vPosition[filterIndex] = selectionIndex;
					onElementUpdated(element, vPosition, deltaCount, 1, deltaCount);
				}
			}
			for (int level = 0; level < levelCount; ++level) {
				prevNode = section.nodes[level];
				if (prevNode.nodes[level] == nextNode) {
					prevNode.distances[level][filterIndex] = selectionIndex - lastIndex[level];
					lastIndex[level] = selectionIndex;
					section.setNextNode(level, nextNode);
				}
			}
			node = nextNode;
		} while (moreData);
		return selectionIndex;
	}

	@SuppressWarnings("UnusedDeclaration")
	void validateIntegrity() {
		Node node = absMinNode;
		Gap accumulated = new Gap();
		for (int level = 0; level < orbitLevel; ++level) {
			accumulated.setNextNode(level, absMinNode);
		}
		Node nextNode;
		do {
			int[] walkSlow = accumulated.distances[0];
			Distance.add(walkSlow, node.distances[0]);
			nextNode = node.nodes[0];
			int nextGoodLevel = 0;
			for (int level = 1; level < orbitLevel; ++level) {
				Node prev = accumulated.nodes[level];
				if (prev.nodes[level] == nextNode) {
					int[] flyDelta = accumulated.distances[level];
					Distance.add(flyDelta, prev.distances[level]);
					accumulated.setNextNode(level, prev.nodes[level]);
					if (!Arrays.equals(flyDelta, walkSlow)) {
						throw new IllegalStateException("Inconsistent distance to element "
								+ (nextNode == null ? null : nextNode.element)
								+ " at level " + level);
					}
					if (level - nextGoodLevel > 1) {
						throw new IllegalStateException("Connected at " + level + " but unconnected at " + (level - 1));
					}
					nextGoodLevel = level;
				}
			}
			node = nextNode;
		} while (node != null);
	}

	@Override
	public void hintBulkOpBegin() {
		// no-op
	}

	@Override
	public void hintBulkOpCompleted() {
		// no-op
	}

	@Override
	public int getUniverseFilterIndex() {
		return universeFilter;
	}

	@Override
	public ProjectorEditor<T> getFilterEditor() {
		return projector;
	}

	@Override
	public void addElementObserver(ElementObserver<? super T> observer) {
		observers.add(observer);
	}

	@Override
	public void removeElementObserver(ElementObserver<? super T> observer) {
		observers.remove(observer);
	}

	abstract class SimpleIterator implements Iterator<T> {
		final int filterIndex;
		final int returnedSize;
		Node node = absMinNode;
		int returnedCount = 0;

		public SimpleIterator(int filterIndex) {
			this.filterIndex = filterIndex;
			returnedSize = size(filterIndex);
		}

		@Override
		public boolean hasNext() {
			return returnedCount < returnedSize;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(); // no use case yet
		}
		
		/**
		 * Actually return the found element, preparing iterator state 
		 * for subsequent {@link #hasNext()} and {@link #next()} calls.
		 * The name is adopted from the Generator Function pattern.
		 * @return the element found.
		 */
		protected final T yield() {
			returnedCount ++;
			return node.element;
		}
	}
	
	class WalkerIterator extends SimpleIterator {
		public WalkerIterator(int filterIndex) {
			super(filterIndex);
		}

		@Override
		public T next() {
			int stepDistance;
			do {
				stepDistance = node.distances[0][filterIndex];
				node = node.nodes[0];
			} while (stepDistance == 0);
			return yield();
		}
	}
	
	class LadderIterator extends SimpleIterator {
		int level, bestLevel;
		int[][] distances;
		
		public LadderIterator(int filterIndex) {
			super(filterIndex);
			bestLevel = 0;
			final int totalSize = size();
			int selectionSize = returnedSize;
			while (bestLevel < node.level && (selectionSize *= denominator) < totalSize) {
				++bestLevel;
			}
			level = bestLevel;
			distances = node.distances;
		}

		@Override
		public T next() {
			// zero, may escalate -> escalate
			// zero, nowhere to escalate -> jump
			// nonzero, may descend -> descend
			// nonzero, nowhere to descend -> yield
			while (distances[0][filterIndex] == 0) {
				while (level > 0 && distances[level][filterIndex] != 0) {
					level--;
				}
				node = node.nodes[level];
				distances = node.distances;
			}
			node = node.nodes[0];
			distances = node.distances;
			level = Math.min(bestLevel, node.level);
			return yield();
		}
		
	}
	
	class CherryIterator extends SimpleIterator {
		public CherryIterator(int filterIndex) {
			super(filterIndex);
		}

		@Override
		public T next() {
			return get(filterIndex, returnedCount++);
		}
	}
}
