/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import com.skype.research.util.adaptable.mocks.DivisibleBy;
import com.skype.research.util.primitives.Filter;
import com.skype.research.util.primitives.Update;
import com.skype.research.util.projection.Derivative;
import com.skype.research.util.projection.ProjectorEditor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Tests of {@link com.skype.research.util.adaptable.AdaptableSkipList} structure.
 */
@RunWith(Parameterized.class)
public class TestAdaptable {

	@Parameterized.Parameters(name = "DuplicatesAllowed: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {{Boolean.FALSE}, {Boolean.TRUE}});
	}
	
	private final boolean allowDuplicates;

	public TestAdaptable(boolean allowDuplicates) {
		this.allowDuplicates = allowDuplicates;
	}
	
	private <T> AdaptableFactory<T> createAdaptableFactory() {
		AdaptableFactory<T> factory = new AdaptableFactory<T>();
		factory.setAllowDuplicates(allowDuplicates);
		return factory;
	}

	@Test
	public void testSequentialPlacement() throws Exception {
        AdaptableFactory<String> ab = createAdaptableFactory();
		FlexibleAdaptable<String> adaptable = ab.create();
		placeAlphabet(adaptable);
		dumpContents(adaptable);
		assertAlphabet(adaptable);
	}

	private void placeAlphabet(FlexibleAdaptable<String> adaptable) {
		for (char c = 'A'; c <= 'Z'; ++c) {
			adaptable.add(Character.toString(c));
			Validation.validateIntegrity(adaptable);
		}
	}

	private <T> void dumpContents(FlexibleAdaptable<T> adaptable) {
		new Dump(System.out).dump((AdaptableSkipList<T>) adaptable);
	}

	@Test
	public void testReorderedPlacement() throws Exception {
        AdaptableFactory<String> ab = createAdaptableFactory();
		FlexibleAdaptable<String> adaptable = ab.create();
		char[] chars = getAlphabet();
		shuffle(chars, 256);
		place(adaptable, chars);
		dumpContents(adaptable);
		assertAlphabet(adaptable);
	}

	@Test
	public void testFilteredPlacement() throws Exception {
		FlexibleAdaptable<String> adaptable = createFilterableAdaptable();
		char[] chars = getAlphabet();
		shuffle(chars, 256);
		place(adaptable, chars);
		dumpContents(adaptable);
		Assert.assertEquals("placement of A vowel", "A", adaptable.get(1, 0));
		Assert.assertEquals("placement of A chess", "A", adaptable.get(3, 0));
		Assert.assertEquals("count of lower cases", 0, adaptable.size(2));
		Assert.assertNull("first lowercase = null", adaptable.get(2, 0));
		Assert.assertEquals("placement of O vowel", "O", adaptable.get(1, 3));
		Assert.assertEquals("placement of H chess", "H", adaptable.get(3, 7));
		assertAlphabet(adaptable);
	}

	final Filter<String> isVowel = new Filter<String>() {
		@Override
		public boolean accept(String item) {
			return item.length() == 1 && "aeiouy".indexOf(item.toLowerCase().charAt(0)) >= 0;
		}
	};

	final Filter<String> isChess = new Filter<String>() {
		@Override
		public boolean accept(String item) {
			if (item.length() == 1) {
				char c = item.toLowerCase().charAt(0);
				if (c >= 'a' && c <= 'h') return true;
			}
			return false;
		}
	};

	final Filter<CharSequence> isAlphanumeric = new Filter<CharSequence>() {
		Pattern pattern = Pattern.compile("^\\w+$");
		@Override
		public boolean accept(CharSequence item) {
			return pattern.matcher(item).matches();
		}
	};

	final Filter<CharSequence> isLowercase = new Filter<CharSequence>() {
		@Override
		public boolean accept(CharSequence item) {
			String string = item.toString();
			return string.equals(string.toLowerCase());
		}
	};

	final Filter<CharSequence> isSpatial = new Filter<CharSequence>() {
		Pattern pattern = Pattern.compile("\\s");
		@Override
		public boolean accept(CharSequence item) {
			return pattern.matcher(item).find();
		}
	};

	private FlexibleAdaptable<String> createFilterableAdaptable() {
        AdaptableFactory<String> builder = createAdaptableFactory();
        builder.addFilter(isVowel);
        builder.addFilter(isLowercase);
        builder.addFilter(isChess);
        return builder.create();
	}

	@Test
	public void testRemovalByIndex() throws Exception {
		FlexibleAdaptable<String> adaptable = createFilterableAdaptable();
		placeAlphabet(adaptable);
		dumpContents(adaptable);
		// remove all vowels
		for(int i = adaptable.size(1) - 1; i >= 0; --i) {
			adaptable.remove(1, i);
			Validation.validateIntegrity(adaptable);
		}
		dumpContents(adaptable);
		assertConsonants(adaptable);
	}

	@Test
	public void testRemovalByValue() throws Exception {
		FlexibleAdaptable<String> adaptable = createFilterableAdaptable();
		placeAlphabet(adaptable);
		dumpContents(adaptable);
		// remove all vowels
		for(char vowel : "YAIOUEAI".toCharArray()) {
			adaptable.remove(Character.toString(vowel));
			Validation.validateIntegrity(adaptable);
		}
		dumpContents(adaptable);
		assertConsonants(adaptable);
	}

	final Comparator<CharSequence> contentsComparator = new Comparator<CharSequence>() {
		@Override
		public int compare(CharSequence lhs, CharSequence rhs) {
			return lhs.toString().compareTo(rhs.toString());
		}
	};

	static final int mutableAdaptableAlphaFilterId = 1;
	static final int mutableAdaptableSpaceFilterId = 3;

	private FlexibleAdaptable<StringBuilder> createMutableFilterableAdaptable() {
        AdaptableFactory<StringBuilder> builder = createAdaptableFactory();
        builder.setComparator(contentsComparator);
        builder.addFilter(isAlphanumeric);
        builder.addFilter(isLowercase);
        builder.addFilter(isSpatial);
        return builder.create();
	}

	@Test
	public void testUpdateInPlace() throws Exception {
		FlexibleAdaptable<StringBuilder> adaptable = createMutableFilterableAdaptable();
		SortedMap<String, StringBuilder> reference = new TreeMap<String, StringBuilder>();
		populatePseudoDictionary(adaptable, reference);
		// note that we will break container invariants in this test. never do likewise in production!
		final int size = adaptable.size();
		final Update<StringBuilder> distort = createDistortionEdit();
		final Random random = new Random(size);
		Map<Integer, Boolean> touched = new HashMap<Integer, Boolean>();
		int unmatched = 0;
		int distorted = 0;
		int index;
		for (int j = 0; j < 256; ++j) {
			do { index = random.nextInt(size); } while (touched.containsKey(index));
			touched.put(index, true);
			StringBuilder victimLocator = new StringBuilder(adaptable.get(index)); // copy
			if (adaptable.updateInPlace(victimLocator, distort)) {
				distorted++;
			}
		}
		index = 0;
		for (Map.Entry<String, StringBuilder> pair : reference.entrySet()) {
			if (!pair.getKey().equals(adaptable.get(index).toString())) {
				unmatched++;
			}
			index++;
		}
		Assert.assertEquals("distorted = unmatched", distorted, unmatched);
	}

	private void populatePseudoDictionary(FlexibleAdaptable<StringBuilder> adaptable, Map<String, StringBuilder> reference) throws IOException {
		final int count = 1024;
		Random random = new Random(count);
		for (int i = 0; i < count; ++i) {
            String string = UUID.randomUUID().toString();
			string = string.replace('S', ' ');
			string = string.replace('T', '\t');
			if (random.nextBoolean()) {
				string = string.toLowerCase();
			}
			StringBuilder element = new StringBuilder(string);
			adaptable.add(element);
			reference.put(string, element);
			Validation.validateIntegrity(adaptable);
		}
	}

    private Update<StringBuilder> createDistortionEdit() {
		return new Update<StringBuilder>() {
			final Random random = new Random(8);

			@Override
			public boolean apply(StringBuilder element) {
				int length = element.length();
				int subInt = Math.min(length, random.nextInt(8));
				if (subInt == 0) {
					return false; // the sequence is unchanged
				}
				int offset = random.nextInt(length - subInt);
				if (random.nextBoolean()) {
					// replace with garbage
					for (int i = 0; i < subInt; ++i) {
						element.setCharAt(offset++, (char) (random.nextInt(128) + 128));
					}
				} else {
					// punch
					element.replace(offset, offset + subInt, "");
				}
				return true;
			}
		};
	}

	@Test
	public void testUpdateFilters() throws Exception {
		FlexibleAdaptable<StringBuilder> adaptable = createMutableFilterableAdaptable();
		SortedMap<String, StringBuilder> reference = new TreeMap<String, StringBuilder>();
		populatePseudoDictionary(adaptable, reference);
		final Random random = new Random(128);
		final Update<StringBuilder> appendGarbage = new Update<StringBuilder>() {
			@Override
			public boolean apply(StringBuilder element) {
				char c = (char) random.nextInt(128);
				if (c == 0) {
					return false;
				} else {
					element.append(c);
					return true;
				}
			}
		};
		for (int index = 0; index < 2048; ++index) {
			int randomIndex = random.nextInt(adaptable.size());
			StringBuilder randomElement = adaptable.get(randomIndex);
			StringBuilder victimLocator = new StringBuilder(randomElement); // copy
			adaptable.updateFilters(victimLocator, appendGarbage);
		}
		Validation.validateIntegrity(adaptable);
		int plainIndex = 0;
		StringBuilder prevElement = adaptable.get(plainIndex);
		for (plainIndex = 1; plainIndex < adaptable.size(); ++plainIndex) {
			StringBuilder nextElement = adaptable.get(plainIndex);
			Assert.assertTrue("Ordering holds", contentsComparator.compare(prevElement, nextElement) < 0);
			prevElement = nextElement;
		}
		int alphaIndex = 0, spaceIndex = 0;
		for (StringBuilder element : reference.values()) {
			if (isAlphanumeric.accept(element)) {
				Assert.assertSame(String.format("Filter 1 reference integrity [%d]", alphaIndex), element,
						adaptable.get(mutableAdaptableAlphaFilterId, alphaIndex));
				alphaIndex++;
			}
			if (isSpatial.accept(element)) {
				Assert.assertSame(String.format("Filter 3 reference integrity [%d]", spaceIndex), element,
						adaptable.get(mutableAdaptableSpaceFilterId, spaceIndex));
				spaceIndex++;
			}
		}
		Assert.assertEquals("Alpha element count", alphaIndex, adaptable.size(mutableAdaptableAlphaFilterId));
		Assert.assertEquals("Space element count", spaceIndex, adaptable.size(mutableAdaptableSpaceFilterId));
	}

	@Test
	public void testUpdateReorder() throws Exception {
		FlexibleAdaptable<StringBuilder> adaptable = createMutableFilterableAdaptable();
		SortedMap<String, StringBuilder> reference = new TreeMap<String, StringBuilder>();
		populatePseudoDictionary(adaptable, reference);
		// we remove and re-add elements into the reference structure to verify reordering
		Update<StringBuilder> invertOrder = new Update<StringBuilder>() {
			@Override
			public boolean apply(StringBuilder element) {
				String original = element.toString();
				int length = element.length();
				int middle = length / 2;
				for (int i = 0; i < middle; ++i) {
					int inverse = length - 1 - i;
					char tmp = element.charAt(i);
					element.setCharAt(i, element.charAt(inverse));
					element.setCharAt(inverse, tmp);
				}
				return !original.equals(element.toString());
			}
		};
		int edits = 512;
		Random random = new Random(edits);
		for (int i = 0; i < edits; ++i) {
			int randomIndex = random.nextInt(adaptable.size());
			StringBuilder victim = adaptable.get(randomIndex);
			StringBuilder victimLocator = new StringBuilder(victim);
			StringBuilder backup = new StringBuilder(victim);
			adaptable.updateReorder(victimLocator, invertOrder);
			//
			reference.remove(backup.toString());
			invertOrder.apply(backup);
			reference.put(backup.toString(), backup);
		}
		int index = 0;
		for (String element : reference.keySet()) {
			Assert.assertEquals("Container contents are identical", element, adaptable.get(index).toString());
			index++;
		}
		Assert.assertEquals("Container size is identical", reference.size(), adaptable.size());
	}

	private void assertConsonants(Adaptable<String> adaptable) {
		char[] charArray = "BCDFGH".toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			char c = charArray[i];
			String consonant = Character.toString(c);
			Assert.assertEquals(consonant, consonant, adaptable.get(3, i));
		}
	}

	private void place(FlexibleAdaptable<String> adaptable, char[] chars) {
		for (char c : chars) {
			adaptable.add(Character.toString(c));
			Validation.validateIntegrity(adaptable);
		}
	}

	private char[] getAlphabet() {
		StringBuilder stringBuilder = new StringBuilder();
		for (char c = 'A'; c <= 'Z'; ++c) {
			stringBuilder.append(c);
		}
		return stringBuilder.toString().toCharArray();
	}

	private void shuffle(char[] chars, int permutations) {
		Random random = new Random();
		char tmp;
		int p, q;
		for (int i = 0; i < permutations; ++i) {
			p = random.nextInt(chars.length);
			q = random.nextInt(chars.length);
			if (p != q) {
				tmp = chars[p];
				chars[p] = chars[q];
				chars[q] = tmp;
			}
		}
	}

	private void assertAlphabet(Adaptable<String> adaptable) {
		for (char c = 'A'; c < 'Z'; ++c) {
			Assert.assertEquals(String.format("adaptable[%d]", c - 'A'),
					Character.toString(c),
					adaptable.get(c - 'A'));
		}
	}

	final Filter<Integer> isEven = new Filter<Integer>() {
		@Override
		public boolean accept(Integer item) {
			return item % 2 == 0;
		}
	};
	final Filter<Integer> isPowerOf2 = new Filter<Integer>() {
		@Override
		public boolean accept(Integer item) {
			return Long.bitCount(item) == 1;
		}
	};
	final Filter<Integer> isAlpha = new Filter<Integer>() {
		@Override
		public boolean accept(Integer item) {
			return Character.isLetter(item);
		}
	};

	@Test
	public void testFilterChange() throws Exception {
		// create
		FlexibleAdaptable<Integer> adaptable = createIntegerSkipList();
		// select
		int[] evenNumbers = takeSelectionSnapshot(adaptable, 1);
		int[] alphaLetters = takeSelectionSnapshot(adaptable, 3);
		// modify and verify
        ProjectorEditor<Integer> filterEditor = adaptable.getFilterEditor();
        adaptable.refreshFilters(filterEditor.setFilter(1, isAlpha));
		assertSameSelection(alphaLetters, adaptable, 1);
        adaptable.refreshFilters(filterEditor.setFilter(2, isEven));
		assertSameSelection(evenNumbers, adaptable, 2);
		assertSameSelection(alphaLetters, adaptable, 1);
	}

	@Test
    public void testDependencies() throws Exception {
        AdaptableFactory<Integer> builder = createAdaptableFactory();
        builder.addFilter(isEven);
        builder.addFilter(isPowerOf2);
        builder.addFilter(isAlpha);
        Derivative inter = new Derivative() {
            @Override
            public boolean accept(int... args) {
                return args[0] * args[1] != 0;
            }
        };
        int indexOfInter = builder.addDerivativeOf(inter, 1, 3);
        Derivative union = new Derivative() {
            @Override
            public boolean accept(int... args) {
                return (args[0] | args[1]) != 0;
            }
        };
        int indexOfUnion = builder.addDerivativeOf(union, 1, 3);
        Derivative delta = new Derivative() {
            @Override
            public boolean accept(int... args) {
                return args[0] != args[1];
            }
        };
        int indexOfDelta = builder.addDerivativeOf(delta, 1, 3);
        int indexOfEmpty = builder.addDerivativeOf(inter, inter, delta);
        FlexibleAdaptable<Integer> adaptable = builder.create();
        // now fill
        fillWithIntegers(adaptable);
        // now check
        Assert.assertEquals(0, adaptable.size(indexOfEmpty));
        Assert.assertEquals(adaptable.size(indexOfUnion),
		        adaptable.size(indexOfInter) + adaptable.size(indexOfDelta));
        adaptable.refreshFilters(adaptable.getFilterEditor().setDerivative(indexOfDelta, inter));
        assertSameSelection(adaptable, indexOfInter, indexOfEmpty); // now empty is inter&inter
        // now play with inter itself
        // 1 was Even, 3 was Alpha
        int[] originalIntersection = takeSelectionSnapshot(adaptable, indexOfInter);
        adaptable.refreshFilters(adaptable.getFilterEditor().setFilter(3, isEven));
        assertSameSelection(adaptable, 1, indexOfInter);
        adaptable.refreshFilters(adaptable.getFilterEditor().setFilter(1, isAlpha));
        assertSameSelection(originalIntersection, adaptable, indexOfInter);
    }

    private static void assertSameSelection(FlexibleAdaptable<Integer> adaptable, int alice, int bobby) {
        final int size = adaptable.size(bobby);
        Assert.assertEquals(adaptable.size(alice), size);
        for (int index = 0; index < size; ++index) {
            Assert.assertEquals(adaptable.get(alice, index), adaptable.get(bobby, index));
        }
    }

    private void fillWithIntegers(FlexibleAdaptable<Integer> adaptable) {
        final Random random = new Random(2048);
        for (int i = 0; i < 2048; ++i) {
            adaptable.add(random.nextInt(1 << 17));
            if (random.nextInt(256) == 0) {
                Validation.validateIntegrity(adaptable);
            }
        }
    }

    private FlexibleAdaptable<Integer> createIntegerSkipList() {
        FlexibleAdaptable<Integer> adaptable = createEmptyIntegerSkipList();
        fillWithIntegers(adaptable);
		return adaptable;
	}

    private FlexibleAdaptable<Integer> createEmptyIntegerSkipList() {
        AdaptableFactory<Integer> builder = createAdaptableFactory();
        builder.addFilter(isEven);
        builder.addFilter(isPowerOf2);
        builder.addFilter(isAlpha);
        return builder.create();
    }

    private int[] takeSelectionSnapshot(FlexibleAdaptable<Integer> adaptable, int filterIndex) {
		int[] selection = new int[adaptable.size(filterIndex)];
		for (int elementIndex = 0; elementIndex < selection.length; elementIndex++) {
			selection[elementIndex] = adaptable.get(filterIndex, elementIndex);
		}
		return selection;
	}

	private void assertSameSelection(int[] selection,
	                                 FlexibleAdaptable<Integer> adaptable, int newFilterIndex) {
		Assert.assertEquals("Selection count", selection.length, adaptable.size(newFilterIndex));
		for (int elementIndex = 0; elementIndex < selection.length; elementIndex++) {
			Assert.assertEquals(String.format("selection[%d]", elementIndex), selection[elementIndex],
					adaptable.get(newFilterIndex, elementIndex).intValue());
		}
	}

	@Test
	public void testConvertIndex() throws Exception {
		// create
		FlexibleAdaptable<Integer> adaptable = createIntegerSkipList();

		/// 1. convert from universal to specific
		validateIndexConversion(adaptable, 0, 1, isEven);
		validateIndexConversion(adaptable, 0, 3, isAlpha);
		/// 2. convert from one specific to another specific
		validateIndexConversion(adaptable, 3, 1, isEven);
		validateIndexConversion(adaptable, 1, 3, isAlpha);
		/// 3. make sure conversions to universal exist and match
		validateIndexConversion(adaptable, 1, 0, Trivial.universeFilter());
		validateIndexConversion(adaptable, 3, 0, Trivial.universeFilter());
	}

	@Test
	public void testIndexOf() throws Exception {
		// create
		FlexibleAdaptable<Integer> adaptable = createIntegerSkipList();
		// test hits
		for (int filterIndex = 0; filterIndex < adaptable.getFilterCount(); ++filterIndex) {
			final int size = adaptable.size(filterIndex);
			for (int elementIndex = 0; elementIndex < size; ++elementIndex) {
				Integer element = adaptable.get(filterIndex, elementIndex);
				int reverseLookup = adaptable.indexOf(filterIndex, element);
				if (allowDuplicates) {
					// if duplicates are allowed
					Integer similar = adaptable.get(filterIndex, reverseLookup);
					Assert.assertEquals(String.format("Reverse lookup of %d", element),
							element, similar);
				} else {
					Assert.assertEquals(String.format("Reverse lookup of %d", element),
							elementIndex, reverseLookup);
				}
			}
		}
        final int size = adaptable.size(0);
        for (int elementIndex = 0; elementIndex < size; ++elementIndex) {
            Integer element = adaptable.get(0, elementIndex);
            // I agree it is inefficient to create the string every time
            String missing = String.format("Reverse lookup of missing %d", element);
            if (!isEven.accept(element)) {
                Assert.assertEquals(missing, -1, adaptable.indexOf(1));
            }
            if (!isAlpha.accept(element)) {
                Assert.assertEquals(missing, -1, adaptable.indexOf(3));
            }
        }
		// test misses
		final Random random = new Random(2048);
		for (int i = 0; i < 2048; ++i) {
			int element = random.nextInt(1 << 17);
			int filterIndex = random.nextInt(adaptable.getFilterCount());
			int reverseLookup = adaptable.indexOf(filterIndex, element);
			int unfilteredSize = adaptable.size();
			int filteredSize = adaptable.size(filterIndex);
			if (reverseLookup != -1) {
				Assert.assertTrue(String.format("Must be able to remove %d in %d", reverseLookup, filterIndex),
						adaptable.remove(filterIndex, reverseLookup));
				Assert.assertTrue("New selection size is 1 less", filteredSize - 1 == adaptable.size(filterIndex));
				Assert.assertTrue("New universe size is 1 less", unfilteredSize - 1 == adaptable.size());
			}
		}
	}

	private void validateIndexConversion(FlexibleAdaptable<Integer> adaptable,
	                                     int sourceFilterIndex, int targetFilterIndex, 
	                                     Filter<? super Integer> targetFilter) {
		boolean sourceIsUniversal = sourceFilterIndex == adaptable.getUniverseFilterIndex();
		int targetSelectionCount = adaptable.size(targetFilterIndex);
		BitSet bitSet = null;
		if (sourceIsUniversal) {
			bitSet = new BitSet(targetSelectionCount);
		}
		for (int index = 0; index < adaptable.size(sourceFilterIndex); ++index) {
			Integer element = adaptable.get(sourceFilterIndex, index);
			boolean fits = targetFilter.accept(element);
			// DO IT NOW
			int convertedIndex = adaptable.convertIndex(index, sourceFilterIndex, targetFilterIndex, false);
			// if the resulting index is out of bounds, trim it
			Integer elementAtConvertedIndex = convertedIndex < 0 ? Integer.MIN_VALUE :
					convertedIndex >= targetSelectionCount ? Integer.MAX_VALUE :
					adaptable.get(targetFilterIndex, convertedIndex);
			if (fits) {
				/// if specific passes, must match universal
				Assert.assertEquals("Lookup by converted index matches lookup by original index",
						element, elementAtConvertedIndex);
				if (sourceIsUniversal) {
					bitSet.set(convertedIndex);
				}
			} else {
				/// if specific does not pass, SHOULD fall in between
				Assert.assertTrue(String.format("Ceiling of [%d]=%d is [%d]=%d",
								index, element, convertedIndex, elementAtConvertedIndex),
						element > elementAtConvertedIndex);
				if (convertedIndex >= 0 && convertedIndex + 1 < targetSelectionCount) {
					Integer elementAtNextIndex = adaptable.get(targetFilterIndex, convertedIndex + 1);
					Assert.assertTrue(String.format("Floor of [%d]=%d is [%d]=%d",
									index, element, convertedIndex, elementAtNextIndex),
							element < elementAtNextIndex);
				}
				// and test ceiling too
			}
		}
		if (sourceIsUniversal) {
			bitSet.flip(0, targetSelectionCount);
			Assert.assertTrue("Universe includes selection fully",
					bitSet.isEmpty());
		}
	}
	
	@Test
	public void testIterators() throws Exception {
		AdaptableFactory<Integer> builder = createAdaptableFactory();
		for (int i = 1, divisor = 0; i <= 10; ++i) {
			divisor<<=1;
			divisor++;
			builder.addFilter(new DivisibleBy(divisor));
		}
		FlexibleAdaptable<Integer> adaptable = builder.create();
		fillWithIntegers(adaptable);
		// new Dump(System.err).dump((AdaptableSkipList<Integer>) adaptable);
		Validation.validateIntegrity(adaptable);
		Validation.validateIterators((AdaptableSkipList<Integer>) adaptable);
		Validation.validateIterators(adaptable); // factory policy
	}
}
