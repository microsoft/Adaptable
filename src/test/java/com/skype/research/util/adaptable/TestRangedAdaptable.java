/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.adaptable.mocks.DivisibleBy;
import com.skype.research.util.adaptable.mocks.Sample;
import com.skype.research.util.primitives.Filter;
import com.skype.research.util.projection.Derivative;
import com.skype.research.util.projection.ProjectorEditor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Test captioned list.
 */
@RunWith(Parameterized.class)
public class TestRangedAdaptable {

    @Parameterized.Parameters(name = "DuplicatesAllowed: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{Boolean.FALSE}, {Boolean.TRUE}});
    }
    
    private final boolean allowDuplicates;

    public TestRangedAdaptable(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    private <T> AdaptableFactory<T> createRangeAdaptableFactory() {
        AdaptableFactory<T> factory = new AdaptableFactory<T>(true);
        factory.setAllowDuplicates(allowDuplicates);
        return factory;
    }

    static final Comparator<Object> contentsComparator = new Comparator<Object>() {
        @Override
        public int compare(Object lhs, Object rhs) {
            return lhs.toString().compareTo(rhs.toString());
        }
    };

    static final Ranger<Character, Object> classifier = new Ranger<Character, Object>() {
        @Override
        public Character getRangeItem(Character rangeKey) {
            return rangeKey;
        }

        @Override
        public Character getRangeKey(Object rangeItem) {
            return (Character) rangeItem;
        }

        @Override
        public boolean accept(Object item) {
            return isCharacter.accept(item);
        }

        @Override
        public Iterable<Character> qualify(Object element) {
            String representation = String.valueOf(element);
            if (representation.length() == 0) {
                return Collections.emptyList();
            } else {
                return Collections.singleton(representation.charAt(0));
            }
        }
    };

    static class PatternMatcher implements Filter<Object> {
        final Pattern pattern;

        PatternMatcher(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        @Override
        public boolean accept(Object item) {
            return pattern.matcher((CharSequence) item).find();
        }
    }
    
    static final Filter<Object> isCharacter = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return item instanceof Character;
        }
    };
    
    static final Filter<Object> hasSpaces = new PatternMatcher("\\s+");
    static final Filter<Object> hasDigits = new PatternMatcher("\\d+");

    @Test
    public void testClassification() throws Exception {
        AdaptableFactory<Object> builder = createRangeAdaptableFactory();
        builder.setComparator(contentsComparator);
        builder.addFilter(hasSpaces);
        builder.addFilter(hasDigits);
        FlexibleAdaptable<Object> ras = builder.create(classifier);
        populateNormalizedAdaptable(ras, 2048);
        assertFilteredIndex(0, ras); // all
        assertFilteredIndex(2, ras); // spatial
        assertFilteredIndex(3, ras); // digital
    }

    private void populateNormalizedAdaptable(FlexibleAdaptable<Object> ras, int count) {
        Random random = new Random(count);
        for (int i = 0; i < count; ++i) {
            ras.add(Long.toHexString(random.nextLong()).replace("[0-8]", "q").replace('z', ' '));
        }
    }

    @Test
    public void testDependencies() throws Exception {
        AdaptableFactory<Object> builder = createRangeAdaptableFactory();
        builder.setComparator(contentsComparator);
        builder.addFilter(hasSpaces);
        builder.addFilter(hasDigits);
        // add un-grouping
        Derivative butNot = new Derivative() {
            @Override
            public boolean accept(int... args) {
                return args[0] != 0 && args[1] == 0;
            }
        };
        int derivativeOne = builder.addDerivativeOf(butNot,
                hasSpaces, 1); // 2, 1
        // computeForGroups==true => headers will disappear
        builder.setShouldComputeForGroup(derivativeOne, true);
        int derivativeTwo = builder.addDerivativeOf(butNot,
                hasDigits, 1); // 3, 1
        // computeForGroups==false => headers will stay
        builder.setShouldComputeForGroup(derivativeTwo, false);
        FlexibleAdaptable<Object> ras = builder.create(classifier);
        // spy
        final Map<Object, int[]> spy = new HashMap<Object, int[]>();
        ras.addElementObserver(new ElementObserver<Object>() {
            @Override
            public void onElementUpdated(Object element, int[] position, int[] changeEstimate, int deltaSign, int[] deltaCount) {
                int[] accumulated = spy.get(element);
                if (accumulated == null) {
                    accumulated = new int[deltaCount.length];
                    for (int i = 0; i < deltaCount.length; i++) {
                        accumulated[i] = deltaSign * deltaCount[i];
                    }
                    spy.put(element, accumulated);
                } else {
                    for (int i = 0; i < deltaCount.length; i++) {
                        accumulated[i] += deltaSign * deltaCount[i];
                    }
                }
            }
        });
        // fill
        populateNormalizedAdaptable(ras, 2048);
        // check evidence
        checkEvidence(ras, spy);
        ensureHeadersDrop(ras, derivativeOne);
        ensureHeadersStay(ras, 3, derivativeTwo);
        // can add extra proof: modify, validate modifications, check evidence once again...
        // for now, grouping just works
    }

    private void ensureHeadersDrop(FlexibleAdaptable<Object> ras, int compoundFilter) {
        for (int index = 0; index < ras.size(compoundFilter); ++index) {
            Object element = ras.get(compoundFilter, index);
            Assert.assertFalse(isCharacter.accept(element));
        }
    }

    private void ensureHeadersStay(FlexibleAdaptable<Object> ras, int componentFilter, int compoundFilter) {
        Set<Character> headSet = getHeaders(ras, componentFilter);
        Set<Character> handSet = getHeaders(ras, compoundFilter);
        Assert.assertEquals(headSet, handSet);
    }

    private Set<Character> getHeaders(FlexibleAdaptable<Object> ras, int componentFilter) {
        Set<Character> headSet = new TreeSet<Character>();
        for (int index = 0; index < ras.size(componentFilter); ++index) {
            Object element = ras.get(componentFilter, index);
            if (isCharacter.accept(element)) {
                Assert.assertTrue(ras.indexOf(1, element) >= 0);
                headSet.add((Character) element);
            }
        }
        ;
        return headSet;
    }

    private void checkEvidence(FlexibleAdaptable<Object> ras, Map<Object, int[]> spy) {
        int size = ras.size();
        for (int universalIndex = 0; universalIndex < size; ++universalIndex) {
            Object element = ras.get(universalIndex);
            int[] evidence = spy.get(element);
            Assert.assertEquals(1, evidence[0]);
            for (int filterIndex = 1; filterIndex < ras.getFilterCount(); ++filterIndex) {
                String location = String.format("Element %s[%d] in filter %d", element, universalIndex, filterIndex);
                int lookup = ras.indexOf(filterIndex, element);
                if (lookup >= 0) { // extra precondition check - make sure the element exists if its index is non-neg
                    Assert.assertSame(location, ras.get(filterIndex, lookup), element);
                }
                Assert.assertEquals(location, lookup < 0 ? 0 : 1, evidence[filterIndex]);
            }
        }
    }

    private void assertFilteredIndex(int filterIndex, FlexibleAdaptable<Object> ras) {
        int size = ras.size(filterIndex);
        char c = 0;
        for (int elementIndex = 0; elementIndex < size; ++elementIndex) {
            Object element = ras.get(filterIndex, elementIndex);
            if (element instanceof Character) {
                c = (Character) element;
            } else {
                Assert.assertEquals(String.format("(%d, %d) => ''%s'' begins with '%c'",
                                filterIndex, elementIndex, String.valueOf(element), c),
                        c, ((CharSequence) element).charAt(0));
            }
        }
    }
    
    @Test
    public void testRefreshFilters() throws Exception {
        AdaptableFactory<Sample> builder = createRangeAdaptableFactory();
        builder.setComparator(sampleComparator);
        int fa = builder.addFilter(new DivisibleBy(5));
        int fb = builder.addFilter(new DivisibleBy(7));
        int fac = builder.addNarrower(new DivisibleBy(3), fa);
        RangedAdaptable<Sample> ras = builder.create(ranger);
        for (int i = 0; i < 1981; ++i) {
            ras.add(new Sample());
        }
        verifyRanges(ras, 0);
        verifyRanges(ras, fa);
        verifyRanges(ras, fb);
        verifyRanges(ras, fac);
        ProjectorEditor<Sample> editor = ras.getFilterEditor();
        ras.refreshFilters(editor.setFilter(fb, new DivisibleBy(9)));
        verifyRanges(ras, fb);
        verifyRanges(ras, fac);
        ras.refreshFilters(editor.setFilter(fa, new DivisibleBy(7)));
        verifyRanges(ras, fa);
        verifyRanges(ras, fb);
        verifyRanges(ras, fac);
    }
    
    static final Comparator<Sample> sampleComparator = new Comparator<Sample>() {
        @Override
        public int compare(Sample lhs, Sample rhs) {
            return  lhs.getValue() > rhs.getValue() ? 1 : lhs.getValue() < rhs.getValue() ? -1 
                    : lhs.boundary == rhs.boundary ? 0 
                    : lhs.boundary ? -1 : 1;
        }
    };
    
    Ranger<Integer, Sample> ranger = new Ranger<Integer, Sample>() {
        @Override
        public Sample getRangeItem(Integer rangeKey) {
            return new Sample(rangeKey);
        }

        @Override
        public Integer getRangeKey(Sample rangeItem) {
            return rangeItem.value;
        }

        @Override
        public boolean accept(Sample item) {
            return item.boundary;
        }

        @Override
        public Iterable<Integer> qualify(Sample element) {
            if (element.boundary) {
                return Collections.emptySet();
            } else {
                return Collections.singleton(element.getRange());
            }
        }
    };
    
    @Test
    public void testReclassification() throws Exception {
        AdaptableFactory<Sample> builder = createRangeAdaptableFactory();
        builder.setComparator(sampleComparator);
        RangedAdaptable<Sample> ras = builder.create(ranger);
        for (int i = 0; i < 1981; ++i) {
            ras.add(new Sample());
        }
        verifyRanges(ras, 0);
        Sample.setBaseLine(9084);
        ras.updateRangeClassification();
        verifyRanges(ras, 0);
    }

    void verifyRanges(RangedAdaptable<Sample> ras, int filterIndex) {
        int lastRange = Integer.MIN_VALUE;
        int maxSample = Integer.MIN_VALUE;
        int size = ras.size(filterIndex);
        int lastCount = 0, lastRealCount = 0;
        for (int elementIndex = 0; elementIndex < size; ++elementIndex) {
            Sample sample = ras.get(filterIndex, elementIndex);
            int value = sample.getValue();
            if (sample.boundary) {
                Assert.assertEquals("Element count", lastCount, lastRealCount);
                Assert.assertTrue(String.format("Boundary %d exceeds maximum %d", value, maxSample),
                        value > maxSample);
                lastRange = value;
                lastCount = ras.getChildCount(sample, filterIndex);
                lastRealCount = 0;
            } else {
                Assert.assertTrue(String.format("Sample %d exceeds last boundary %d", value, lastRange),
                        value >= lastRange);
                maxSample = value;
                ++lastRealCount;
            }
        }
        Assert.assertEquals("Element count", lastCount, lastRealCount);
    }
}
