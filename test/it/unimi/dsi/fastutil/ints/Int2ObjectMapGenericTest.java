package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public abstract class Int2ObjectMapGenericTest<M extends Int2ObjectMap<Integer>> {
	private static final Integer MINUS_ONE = Integer.valueOf(-1);
	private static final Integer ONE = Integer.valueOf(1);
	private static final Integer THREE = Integer.valueOf(3);
	private static final Integer TWO = Integer.valueOf(2);
	private static final Integer ZERO = Integer.valueOf(0);
	private static final Integer DEFAULT = Integer.valueOf(Integer.MIN_VALUE);
	private static final java.util.Random r = new java.util.Random(0);

	@Parameter(1)
	public EnumSet<Capability> capabilities;
	@Parameter(0)
	public Supplier<M> mapSupplier;
	protected M m;

	@SuppressWarnings("deprecation")
	protected void check(final Int2ObjectMap<Integer> m, final Map<Integer, Integer> t, final IntSupplier keyProvider, final IntSupplier valueProvider, final int size) {
		/* First of all, we fill t with random data. */
		for (int i = 0; i < size; i++) {
			t.put(Integer.valueOf(keyProvider.getAsInt()), Integer.valueOf(valueProvider.getAsInt()));
		}
		/* Now we add to m the same data */
		m.putAll(t);

		checkEquality(m, t, genTestKeys(m.keySet(), size), "after insertion");

		/* Now we put and remove random data in m and t, checking that the result is the same. */
		for (int i = 0; i < 20 * size; i++) {
			final int putKey = keyProvider.getAsInt();
			final int val = valueProvider.getAsInt();

			final Integer mPut = m.put(putKey, Integer.valueOf(val));
			final Integer tPut = t.put(Integer.valueOf(putKey), Integer.valueOf(val));
			assertEquals("Error: divergence in put() between t and m", mPut, tPut);

			final int remKey = keyProvider.getAsInt();
			assertEquals("Error: divergence in remove() between t and m", m.remove(Integer.valueOf(remKey)), t.remove(Integer.valueOf(remKey)));
		}

		checkEquality(m, t, genTestKeys(m.keySet(), size), "after removal");
	}

	@SuppressWarnings("deprecation")
	private void checkEquality(final Int2ObjectMap<Integer> m, final Map<Integer, Integer> t, final Iterable<Integer> keys, final String description) {
		final Integer drv = m.defaultReturnValue();

		assertTrue("Error: !m.equals(t) " + description, m.equals(t));
		assertTrue("Error: !t.equals(m) " + description, t.equals(m));

		/* Now we check that m actually holds that data. */
		for (final Map.Entry<Integer, Integer> o2 : t.entrySet()) {
			assertTrue("Error: m and t differ on an entry (" + o2 + ") " + description + " (iterating on t)", Objects.equals(o2.getValue(), m.get(o2.getKey())));
		}
		/* Now we check that m actually holds that data, but iterating on m. */
		for (final Map.Entry<Integer, Integer> entry : m.int2ObjectEntrySet()) {
			assertTrue("Error: m and t differ on an entry (" + entry + ") " + description + " (iterating on m)", Objects.equals(entry.getValue(), t.get(entry.getKey())));
		}

		/* Now we check that m actually holds the same keys. */
		for (final Integer objKey : t.keySet()) {
			assertTrue("Error: m and t differ on a key (" + objKey + ") " + description + " (iterating on t)", m.containsKey(objKey));
			assertTrue("Error: m and t differ on a key (" + objKey + ", in keySet()) " + description + " (iterating on t)", m.keySet().contains(objKey));
		}
		/* Now we check that m actually holds the same keys, but iterating on m. */
		for (final Integer objKey : m.keySet()) {
			assertTrue("Error: m and t differ on a key " + description + " (iterating on m)", t.containsKey(objKey));
			assertTrue("Error: m and t differ on a key (in keySet()) " + description + " (iterating on m)", t.keySet().contains(objKey));
		}

		/* Now we check that m actually hold the same values. */
		for (final Integer objVal : t.values()) {
			assertTrue("Error: m and t differ on a value " + description + " (iterating on t)", m.containsValue(objVal));
			assertTrue("Error: m and t differ on a value (in values()) " + description + " (iterating on t)", m.values().contains(objVal));
		}
		/* Now we check that m actually hold the same values, but iterating on m. */
		for (final Integer objVal : m.values()) {
			assertTrue("Error: m and t differ on a value " + description + " (iterating on m)", t.containsValue(objVal));
			assertTrue("Error: m and t differ on a value (in values()) " + description + " (iterating on m)", t.values().contains(objVal));
		}

		/* Now we check that inquiries about random data give the same answer in m and t. */
		for (final Integer objKey : keys) {
			assertTrue("Error: divergence in keys between t and m (polymorphic method)", m.containsKey(objKey) == t.containsKey(objKey));

			final Integer mVal = m.get(objKey.intValue());
			final Integer mValObj = m.get(objKey);
			final Integer tVal = t.get(objKey);
			assertEquals("Error: divergence between t and m " + description + " (polymorphic method)", tVal, mValObj);
			assertTrue("Error: divergence between polymorphic and standard method " + description, mVal == (mValObj == null ? drv : mValObj));
			assertEquals("Error: divergence between t and m " + description + " (standard method)", tVal == null ? drv : tVal, mVal);
		}
	}

	private IntCollection genTestKeys(final IntCollection initial, final int amount) {
		final IntCollection keys = new IntOpenHashSet(initial);
		for (int i = 0; i < amount; i++) {
			keys.add(r.nextInt());
		}
		return keys;
	}

	@Before
	public void setUp() {
		m = mapSupplier.get();
	}

	@Test
	public void test10() {
		check(m, new HashMap<>(), () -> r.nextInt(10), r::nextInt, 10);
	}

	@Test
	public void test100() {
		check(m, new HashMap<>(), () -> r.nextInt(100), r::nextInt, 100);
	}

	@Test
	public void test1000() {
		check(m, new HashMap<>(), () -> r.nextInt(1000), r::nextInt, 1000);
	}

	@Test
	public void test10000() {
		check(m, new HashMap<>(), () -> r.nextInt(10000), r::nextInt, 10000);
	}

	@Test
	public void testClone() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		assumeTrue(m instanceof Cloneable);
		final Method clone = m.getClass().getMethod("clone");

		assertEquals(m, clone.invoke(m));
		m.put(0, ONE);
		assertEquals(m, clone.invoke(m));
		m.put(0, TWO);
		assertEquals(m, clone.invoke(m));
		m.put(1, THREE);
		assertEquals(m, clone.invoke(m));
		m.remove(1);
		assertEquals(m, clone.invoke(m));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testComputeIfAbsentObject() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		assertEquals(ONE, m.computeIfAbsent(ONE, key -> Integer.valueOf(key.intValue() - 1)));

		assertEquals(ONE, m.computeIfAbsent(TWO, key -> Integer.valueOf(key.intValue() - 1)));
		assertEquals(ONE, m.computeIfAbsent(TWO, key -> Integer.valueOf(key.intValue() - 2)));
		assertEquals(ONE, m.get(TWO));

		assertEquals(2, m.size());
		m.clear();

		assertSame(DEFAULT, m.computeIfAbsent(ONE, key -> null));
		assertSame(DEFAULT, m.computeIfAbsent(TWO, key -> null));
		assertTrue(m.isEmpty());

		m.put(ONE, ONE);
		assertEquals(ONE, m.computeIfAbsent(ONE, key -> null));
		assertSame(DEFAULT, m.computeIfAbsent(TWO, key -> null));

		assertEquals(ONE, m.computeIfAbsent(ONE, key -> key));
		assertEquals(TWO, m.computeIfAbsent(TWO, key -> key));
		assertEquals(TWO, m.computeIfAbsent(TWO, key -> null));
		assertEquals(TWO, m.get(TWO));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeIfAbsentObjectNullFunction() {
		m.put(1, ONE);
		m.computeIfAbsent(ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeIfAbsentObjectNullFunctionMissingKey() {
		m.computeIfAbsent(ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeIfAbsentObjectNullKey() {
		m.computeIfAbsent(null, key -> key);
	}

	@Test
	public void testComputeIfAbsentPartialPrimitive() {
		m.defaultReturnValue(DEFAULT);

		final Int2ObjectFunction<Integer> f = new Int2ObjectArrayMap<>();
		f.put(1, ONE);

		assertEquals(ONE, m.computeIfAbsentPartial(1, f));
		assertEquals(ONE, m.get(1));

		assertSame(DEFAULT, m.computeIfAbsentPartial(2, f));
		assertFalse(m.containsKey(2));

		f.put(2, TWO);
		assertEquals(TWO, m.computeIfAbsentPartial(2, f));
		assertTrue(m.containsKey(2));
	}

	@Test(expected = NullPointerException.class)
	public void testComputeIfAbsentPartialPrimitiveNullFunction() {
		m.put(1, ONE);
		m.computeIfAbsentPartial(1, null);
	}

	@Test(expected = NullPointerException.class)
	public void testComputeIfAbsentPartialPrimitiveNullFunctionMissingKey() {
		m.computeIfAbsentPartial(1, null);
	}

	@Test
	public void testComputeIfAbsentPrimitive() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		assertEquals(ONE, m.computeIfAbsent(1, key -> Integer.valueOf(key - 1)));

		assertEquals(ONE, m.computeIfAbsent(2, key -> Integer.valueOf(key - 1)));
		assertEquals(ONE, m.computeIfAbsent(2, key -> Integer.valueOf(key - 2)));
		assertEquals(ONE, m.get(2));

		assertEquals(2, m.size());
	}

	@Test(expected = NullPointerException.class)
	public void testComputeIfAbsentPrimitiveNullFunction() {
		m.put(1, ONE);
		m.computeIfAbsent(1, null);
	}

	@Test(expected = NullPointerException.class)
	public void testComputeIfAbsentPrimitiveNullFunctionMissingKey() {
		m.computeIfAbsent(1, null);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testComputeIfPresentObject() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		final BiFunction<Integer, Integer, Integer> add = (key, value) -> Integer.valueOf(key.intValue() + value.intValue());

		assertSame(DEFAULT, m.computeIfPresent(TWO, add));
		assertSame(DEFAULT, m.computeIfPresent(TWO, (key, value) -> null));
		assertFalse(m.containsKey(TWO));

		assertEquals(TWO, m.computeIfPresent(ONE, add));
		assertEquals(TWO, m.get(ONE));
		assertEquals(THREE, m.computeIfPresent(ONE, add));
		assertEquals(THREE, m.get(ONE));

		assertEquals(MINUS_ONE, m.computeIfPresent(ONE, (key, value) -> MINUS_ONE));
		assertTrue(m.containsKey(ONE));

		assertSame(DEFAULT, m.computeIfPresent(ONE, (key, value) -> null));
		assertFalse(m.containsKey(ONE));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeIfPresentObjectNullFunction() {
		m.put(1, ONE);
		m.computeIfPresent(ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeIfPresentObjectNullFunctionMissingKey() {
		m.computeIfPresent(ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeIfPresentObjectNullKey() {
		m.computeIfPresent(null, (key, value) -> key);
	}

	@Test
	public void testComputeIfPresentPrimitive() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		final BiFunction<Integer, Integer, Integer> add = (key, value) -> Integer.valueOf(key.intValue() + value.intValue());

		assertSame(DEFAULT, m.computeIfPresent(2, add));
		assertSame(DEFAULT, m.computeIfPresent(2, (key, value) -> null));
		assertFalse(m.containsKey(2));

		assertEquals(TWO, m.computeIfPresent(1, add));
		assertEquals(TWO, m.get(1));
		assertEquals(THREE, m.computeIfPresent(1, add));
		assertEquals(THREE, m.get(1));

		assertSame(DEFAULT, m.computeIfPresent(1, (key, value) -> null));
		assertFalse(m.containsKey(1));
	}

	@Test(expected = NullPointerException.class)
	public void testComputeIfPresentPrimitiveNullFunction() {
		m.put(1, ONE);
		m.computeIfPresent(1, null);
	}

	@Test(expected = NullPointerException.class)
	public void testComputeIfPresentPrimitiveNullFunctionMissingKey() {
		m.computeIfPresent(1, null);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testComputeObject() {
		m.defaultReturnValue(DEFAULT);

		// Test parameters of function
		assertEquals(ONE, m.compute(ONE, (key, value) -> {
			assertEquals(ONE, key);
			assertNull(value);
			return ONE;
		}));
		assertEquals(ONE, m.get(ONE));

		assertEquals(TWO, m.compute(ONE, (key, value) -> {
			assertEquals(ONE, key);
			assertEquals(ONE, value);
			return TWO;
		}));
		assertEquals(TWO, m.get(ONE));

		assertSame(DEFAULT, m.compute(ONE, (key, value) -> {
			assertEquals(ONE, key);
			assertEquals(TWO, value);
			return null;
		}));
		assertFalse(m.containsKey(ONE));

		// Test functionality
		assertEquals(Integer.valueOf(1000), m.compute(ZERO, (x, y) -> Integer.valueOf(x.intValue() + (y != null ? y.intValue() : 1000))));
		assertEquals(Integer.valueOf(1000), m.get(ZERO));
		assertEquals(Integer.valueOf(2000), m.compute(ZERO, (x, y) -> Integer.valueOf(x.intValue() + y.intValue() * 2)));
		assertEquals(Integer.valueOf(2000), m.get(ZERO));
		assertSame(DEFAULT, m.compute(ZERO, (x, y) -> null));
		assertFalse(m.containsKey(0));

		assertEquals(Integer.valueOf(1001), m.compute(ONE, (x, y) -> Integer.valueOf(x.intValue() + (y != null ? y.intValue() : 1000))));
		assertEquals(Integer.valueOf(1001), m.get(ONE));
		assertEquals(Integer.valueOf(2003), m.compute(ONE, (x, y) -> Integer.valueOf(x.intValue() + y.intValue() * 2)));
		assertEquals(Integer.valueOf(2003), m.get(ONE));
		assertSame(DEFAULT, m.compute(ONE, (x, y) -> null));
		assertFalse(m.containsKey(1));

		assertSame(DEFAULT, m.compute(TWO, (x, y) -> null));
		assertFalse(m.containsKey(2));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeObjectNullFunction() {
		m.put(1, ONE);
		m.compute(ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testComputeObjectNullFunctionMissingKey() {
		m.compute(ONE, null);
	}

	@Test
	public void testComputePrimitive() {
		m.defaultReturnValue(DEFAULT);

		// Test parameters of function
		assertEquals(ONE, m.compute(1, (key, value) -> {
			assertEquals(1, key.intValue());
			assertNull(value);
			return Integer.valueOf(1);
		}));
		assertEquals(ONE, m.get(1));

		assertEquals(TWO, m.compute(1, (key, value) -> {
			assertEquals(1, key.intValue());
			assertEquals(1, value.intValue());
			return Integer.valueOf(2);
		}));
		assertEquals(TWO, m.get(1));

		assertSame(DEFAULT, m.compute(1, (key, value) -> {
			assertEquals(1, key.intValue());
			assertEquals(2, value.intValue());
			return null;
		}));
		assertFalse(m.containsKey(1));

		// Test functionality
		assertEquals(1000, m.compute(0, (x, y) -> Integer.valueOf(x.intValue() + (y != null ? y.intValue() : 1000))).intValue());
		assertEquals(1000, m.get(0).intValue());
		assertEquals(2000, m.compute(0, (x, y) -> Integer.valueOf(x.intValue() + y.intValue() * 2)).intValue());
		assertEquals(2000, m.get(0).intValue());
		assertSame(DEFAULT, m.compute(0, (x, y) -> null));
		assertSame(DEFAULT, m.get(0));

		assertEquals(1001, m.compute(1, (x, y) -> Integer.valueOf(x.intValue() + (y != null ? y.intValue() : 1000))).intValue());
		assertEquals(1001, m.get(1).intValue());
		assertEquals(2003, m.compute(1, (x, y) -> Integer.valueOf(x.intValue() + y.intValue() * 2)).intValue());
		assertEquals(2003, m.get(1).intValue());
		assertSame(DEFAULT, m.compute(1, (x, y) -> null));
		assertSame(DEFAULT, m.get(1));

		assertSame(DEFAULT, m.compute(2, (x, y) -> null));
		assertSame(DEFAULT, m.get(2));
	}

	@Test(expected = NullPointerException.class)
	public void testComputePrimitiveNullFunction() {
		m.put(1, ONE);
		m.compute(1, null);
	}

	@Test(expected = NullPointerException.class)
	public void testComputePrimitiveNullFunctionMissingKey() {
		m.compute(1, null);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testContainsNull() {
		assertFalse(m.containsKey(null));
		assertFalse(m.containsValue(null));

		m.put(0, ZERO);
		assertFalse(m.containsKey(null));
		assertFalse(m.containsValue(null));

		assertNull(m.get(null));
	}

	@Test
	public void testEntrySet() {
		m.defaultReturnValue(DEFAULT);
		for (int i = 0; i < 100; i++) {
			assertSame(DEFAULT, m.put(i, Integer.valueOf(i)));
		}
		for (int i = 0; i < 100; i++) {
			assertTrue(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(0, Integer.valueOf(0))));
		}
		for (int i = 0; i < 100; i++) {
			assertFalse(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(i, DEFAULT)));
		}
		for (int i = 0; i < 100; i++) {
			assertTrue(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(i, Integer.valueOf(i))));
		}
		for (int i = 0; i < 100; i++) {
			assertFalse(m.int2ObjectEntrySet().remove(new AbstractInt2ObjectMap.BasicEntry<>(i, DEFAULT)));
		}
		for (int i = 0; i < 100; i++) {
			assertTrue(m.int2ObjectEntrySet().remove(new AbstractInt2ObjectMap.BasicEntry<>(i, Integer.valueOf(i))));
		}
		assertTrue(m.int2ObjectEntrySet().isEmpty());
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Test
	public void testEntrySetContains() {
		m.put(0, ZERO);
		assertFalse(m.int2ObjectEntrySet().contains(new AbstractMap.SimpleEntry<>(new Object(), null)));
		assertFalse(m.int2ObjectEntrySet().contains(new AbstractMap.SimpleEntry<>(null, new Object())));
		assertFalse(m.int2ObjectEntrySet().contains(new AbstractMap.SimpleEntry<>(null, null)));
		assertFalse(m.int2ObjectEntrySet().contains(new AbstractMap.SimpleEntry<>(new Object(), new Object())));
	}

	@Test(expected = IllegalStateException.class)
	public void testEntrySetEmptyIteratorRemove() {
		final ObjectIterator<Entry<Integer>> iterator = m.int2ObjectEntrySet().iterator();
		assertFalse(iterator.hasNext());
		iterator.remove();
	}

	@Test
	public void testEntrySetIteratorFastForEach() {
		for (int i = 0; i < 100; i++) {
			m.put(i, Integer.valueOf(i));
		}
		final Set<Entry<Integer>> s = new HashSet<>();
		Int2ObjectMaps.fastForEach(m, x -> s.add(new AbstractInt2ObjectMap.BasicEntry<>(x.getIntKey(), x.getValue())));
		assertEquals(m.int2ObjectEntrySet(), s);
	}

	@Test
	public void testEntrySetIteratorForEach() {
		for (int i = 0; i < 100; i++) {
			m.put(i, Integer.valueOf(i));
		}
		final Set<Entry<Integer>> s = new HashSet<>();
		//noinspection UseBulkOperation
		m.int2ObjectEntrySet().forEach(s::add);
		assertEquals(m.int2ObjectEntrySet(), s);
	}

	@Test(expected = IllegalStateException.class)
	public void testEntrySetIteratorTwoRemoves() {
		m.put(0, ZERO);
		m.put(1, ONE);
		final ObjectIterator<Entry<Integer>> iterator = m.int2ObjectEntrySet().iterator();
		iterator.next();
		iterator.remove();
		iterator.remove();
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Test
	public void testEntrySetRemove() {
		m.put(0, ZERO);
		assertFalse(m.int2ObjectEntrySet().remove(new AbstractMap.SimpleEntry<>(new Object(), null)));
		assertFalse(m.int2ObjectEntrySet().remove(new AbstractMap.SimpleEntry<>(null, new Object())));
		assertFalse(m.int2ObjectEntrySet().remove(new AbstractMap.SimpleEntry<>(null, null)));
		assertFalse(m.int2ObjectEntrySet().remove(new AbstractMap.SimpleEntry<>(new Object(), new Object())));
	}

	@Test
	public void testEquals() {
		m.put(1, ONE);
		assertFalse(m.equals(new Object2ObjectOpenHashMap<>(new Integer[] {ONE, null}, new Integer[] {ONE, null})));
		assertTrue(m.equals(new Object2ObjectOpenHashMap<>(new Integer[] {ONE}, new Integer[] {ONE})));
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalStateException.class)
	public void testFastEntrySetEmptyIteratorRemove() {
		final ObjectSet<Entry<Integer>> entries = m.int2ObjectEntrySet();
		assumeTrue(entries instanceof Int2ObjectMap.FastEntrySet);
		final ObjectIterator<Entry<Integer>> iterator = ((Int2ObjectMap.FastEntrySet) entries).fastIterator();
		assertFalse(iterator.hasNext());
		iterator.remove();
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalStateException.class)
	public void testFastEntrySetIteratorTwoRemoves() {
		m.put(0, ZERO);
		m.put(1, ONE);
		final ObjectSet<Entry<Integer>> entries = m.int2ObjectEntrySet();
		assumeTrue(entries instanceof Int2ObjectMap.FastEntrySet);
		final ObjectIterator<Entry<Integer>> iterator = ((Int2ObjectMap.FastEntrySet) entries).fastIterator();
		iterator.next();
		iterator.remove();
		iterator.remove();
	}

	@Test
	public void testFastIterator() {
		assumeTrue(m.int2ObjectEntrySet() instanceof Int2ObjectMap.FastEntrySet);
		assumeTrue(capabilities.contains(Capability.ITERATOR_MODIFY));

		m.defaultReturnValue(DEFAULT);
		for (int i = 0; i < 100; i++) {
			assertSame(DEFAULT, m.put(i, Integer.valueOf(i)));
		}
		final ObjectIterator<Entry<Integer>> fastIterator = Int2ObjectMaps.fastIterator(m);
		final Entry entry = fastIterator.next();
		final int key = entry.getIntKey();
		entry.setValue(Integer.valueOf(1000));
		assertEquals(m.get(key), Integer.valueOf(1000));
		fastIterator.remove();
		assertEquals(m.get(key), DEFAULT);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testGetOrDefaultObject() {
		m.put(1, ONE);

		assertEquals(ZERO, m.getOrDefault(ZERO, ZERO));
		assertEquals(ONE, m.getOrDefault(ZERO, ONE));
		assertEquals(ONE, m.getOrDefault(ONE, TWO));

		m.put(0, ONE);
		assertEquals(ONE, m.getOrDefault(ZERO, ZERO));
		assertEquals(ONE, m.getOrDefault(ONE, ZERO));

		m.put(1, ONE);
		assertEquals(ONE, m.getOrDefault(ONE, TWO));

		assertEquals(THREE, m.getOrDefault(null, THREE));
		assertEquals(THREE, m.getOrDefault(THREE, THREE));
		assertNull(m.getOrDefault(THREE, null));
		assertNull(m.getOrDefault(null, null));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = ClassCastException.class)
	public void testGetOrDefaultObjectInvalidKey() {
		m.getOrDefault(Long.valueOf(1), ONE);
	}

	@Test
	public void testGetOrDefaultPrimitive() {
		m.put(1, ONE);

		assertEquals(ZERO, m.getOrDefault(0, ZERO));
		assertEquals(ONE, m.getOrDefault(0, ONE));
		assertEquals(ONE, m.getOrDefault(1, TWO));

		m.put(0, ONE);
		assertEquals(ONE, m.getOrDefault(0, ZERO));
		assertEquals(ONE, m.getOrDefault(1, ZERO));

		m.put(1, ONE);
		assertEquals(ONE, m.getOrDefault(1, TWO));
	}

	@Test
	public void testKeySetIteratorForEach() {
		for (int i = 0; i < 100; i++) {
			m.put(i, Integer.valueOf(i));
		}
		final IntOpenHashSet s = new IntOpenHashSet();
		m.keySet().forEach((java.util.function.IntConsumer) s::add);
		assertEquals(s, m.keySet());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testKeySetIteratorForEachObject() {
		for (int i = 0; i < 100; i++) {
			m.put(i, Integer.valueOf(i));
		}
		final IntOpenHashSet s = new IntOpenHashSet();
		m.keySet().forEach((Consumer<Integer>) s::add);
		assertEquals(s, m.keySet());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testMap() {
		assertNull(m.put(1, ONE));
		assertEquals(1, m.size());
		assertTrue(m.containsKey(1));
		assertTrue(m.containsValue(ONE));

		assertNull(m.put(2, TWO));
		assertTrue(m.containsKey(2));
		assertTrue(m.containsValue(TWO));
		assertEquals(2, m.size());

		m.defaultReturnValue(DEFAULT);

		assertEquals(ONE, m.put(1, THREE));
		assertTrue(m.containsValue(THREE));
		assertEquals(DEFAULT, m.remove(THREE));
		assertEquals(DEFAULT, m.put(3, THREE));
		assertTrue(m.containsKey(THREE));
		assertTrue(m.containsValue(THREE));
		assertEquals(3, m.size());

		assertEquals(THREE, m.get(1));
		assertEquals(TWO, m.get(2));
		assertEquals(THREE, m.get(3));

		assertEquals(new IntOpenHashSet(new int[] {1, 2, 3}), new IntOpenHashSet(m.keySet().iterator()));
		assertEquals(new ObjectOpenHashSet<>(new Integer[] {THREE, TWO, THREE}), new ObjectOpenHashSet<>(m.values().iterator()));

		for (final Map.Entry<Integer, Integer> entry : m.int2ObjectEntrySet()) {
			assertEquals(entry.getValue(), m.get(entry.getKey()));
		}

		assertTrue(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(1, THREE)));
		assertTrue(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(2, TWO)));
		assertTrue(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(3, THREE)));
		assertFalse(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(1, TWO)));
		assertFalse(m.int2ObjectEntrySet().contains(new AbstractInt2ObjectMap.BasicEntry<>(2, ONE)));

		assertEquals(THREE, m.remove(3));
		assertEquals(2, m.size());
		assertEquals(THREE, m.remove(1));
		assertEquals(1, m.size());
		assertFalse(m.containsKey(1));
		assertEquals(TWO, m.remove(2));
		assertEquals(0, m.size());
		assertFalse(m.containsKey(1));
	}

	@Test
	public void testMerge() {
		m.defaultReturnValue(DEFAULT);
		assertEquals(ZERO, m.merge(0, ZERO, (x, y) -> Integer.valueOf(1000)));
		assertEquals(ZERO, m.get(0));
		assertEquals(Integer.valueOf(1000), m.merge(0, ZERO, (x, y) -> Integer.valueOf(1000)));
		assertEquals(Integer.valueOf(1000), m.get(0));
		assertEquals(Integer.valueOf(2000), m.merge(0, Integer.valueOf(500), (x, y) -> Integer.valueOf(x.intValue() + y.intValue() * 2)));
		assertEquals(Integer.valueOf(2000), m.get(0));
		assertSame(DEFAULT, m.merge(0, ZERO, (x, y) -> null));
		assertSame(DEFAULT, m.get(0));

		assertEquals(ZERO, m.merge(1, ZERO, (x, y) -> Integer.valueOf(1000)));
		assertEquals(ZERO, m.get(1));
		assertEquals(Integer.valueOf(1000), m.merge(1, ZERO, (x, y) -> Integer.valueOf(1000)));
		assertEquals(Integer.valueOf(1000), m.get(1));
		assertEquals(Integer.valueOf(2000), m.merge(1, Integer.valueOf(500), (x, y) -> Integer.valueOf(x.intValue() + y.intValue() * 2)));
		assertEquals(Integer.valueOf(2000), m.get(1));
		assertSame(DEFAULT, m.merge(1, ZERO, (x, y) -> null));
		assertSame(DEFAULT, m.get(1));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testMergeObject() {
		m.defaultReturnValue(DEFAULT);

		assertEquals(ZERO, m.merge(ONE, ZERO, (oldVal, newVal) -> {
			assertSame(DEFAULT, oldVal);
			assertEquals(ZERO, newVal);
			return ZERO;
		}));
		assertEquals(ZERO, m.get(ONE));
		m.clear();

		final BiFunction<Integer, Integer, Integer> add = (oldVal, newVal) -> Integer.valueOf(oldVal.intValue() + newVal.intValue());

		assertEquals(ZERO, m.merge(ONE, ZERO, add));
		assertEquals(ONE, m.merge(ONE, ONE, add));
		assertEquals(THREE, m.merge(ONE, TWO, add));
		assertEquals(ZERO, m.merge(TWO, ZERO, add));
		assertTrue(m.containsKey(ONE));

		assertSame(DEFAULT, m.merge(ONE, TWO, (key, value) -> null));
		assertSame(DEFAULT, m.merge(TWO, TWO, (key, value) -> null));

		assertTrue(m.isEmpty());
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testMergeObjectNullFunction() {
		m.put(1, ONE);
		m.merge(ONE, ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testMergeObjectNullFunctionMissingKey() {
		m.merge(ONE, ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testMergeObjectNullKey() {
		m.merge(null, ONE, (key, vale) -> ONE);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testMergeObjectNullValue() {
		m.put(1, ONE);
		m.merge(ONE, null, (key, vale) -> ONE);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testMergeObjectNullValueMissingKey() {
		m.merge(ONE, null, (key, vale) -> ONE);
	}

	@Test
	public void testMergePrimitive() {
		m.defaultReturnValue(DEFAULT);

		assertEquals(ZERO, m.merge(1, ZERO, (oldVal, newVal) -> {
			assertSame(DEFAULT, oldVal);
			assertEquals(ZERO, newVal);
			return Integer.valueOf(0);
		}));
		assertEquals(ZERO, m.get(1));
		m.clear();

		final BiFunction<Integer, Integer, Integer> add = (oldVal, newVal) -> Integer.valueOf(oldVal.intValue() + newVal.intValue());

		assertEquals(ZERO, m.merge(1, ZERO, add));
		assertEquals(ONE, m.merge(1, ONE, add));
		assertEquals(THREE, m.merge(1, TWO, add));
		assertEquals(ZERO, m.merge(2, ZERO, add));
		assertTrue(m.containsKey(1));

		assertSame(DEFAULT, m.merge(1, TWO, (key, value) -> null));
		assertSame(DEFAULT, m.merge(2, TWO, (key, value) -> null));

		assertTrue(m.isEmpty());
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testMergePrimitiveNullFunction() {
		m.put(1, ONE);
		m.merge(1, ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testMergePrimitiveNullFunctionMissingKey() {
		m.merge(1, ONE, null);
	}

	@Test
	public void testPutAndGetPrimitive() {
		m.defaultReturnValue(DEFAULT);
		assertSame(DEFAULT, m.get(1));
		m.put(1, ONE);
		assertEquals(ONE, m.get(1));
		m.defaultReturnValue(ONE);
		assertTrue(m.containsKey(1));
		assertEquals(ONE, m.get(1));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testPutIfAbsentObject() {
		m.defaultReturnValue(DEFAULT);

		m.put(1, ONE);
		assertEquals(ONE, m.putIfAbsent(ONE, TWO));
		assertEquals(ONE, m.putIfAbsent(ONE, null));

		assertSame(DEFAULT, m.putIfAbsent(TWO, TWO));
		assertEquals(TWO, m.get(TWO));

		assertEquals(TWO, m.putIfAbsent(TWO, THREE));
		assertEquals(TWO, m.get(TWO));

		m.remove(1);
		assertSame(DEFAULT, m.putIfAbsent(ONE, null));
		assertNull(m.get(ONE));
	}

	@Test
	public void testPutIfAbsentPrimitive() {
		m.defaultReturnValue(DEFAULT);

		m.put(1, ONE);
		assertEquals(ONE, m.putIfAbsent(1, TWO));

		assertSame(DEFAULT, m.putIfAbsent(2, TWO));
		assertEquals(TWO, m.get(2));

		assertEquals(TWO, m.putIfAbsent(2, THREE));
		assertEquals(TWO, m.get(2));

		m.remove(1);
		assertSame(DEFAULT, m.putIfAbsent(1, null));
		assertNull(m.get(1));
	}

	@Test
	public void testRemove() {
		m.defaultReturnValue(DEFAULT);
		for (int i = 0; i < 100; i++) {
			assertSame(DEFAULT, m.put(i, Integer.valueOf(i)));
		}
		for (int i = 0; i < 100; i++) {
			assertSame(DEFAULT, m.remove(100 + i));
		}
		for (int i = 50; i < 150; i++) {
			assertEquals(Integer.valueOf(i % 100), m.remove(i % 100));
			assertEquals(m.size(), m.int2ObjectEntrySet().size());
		}
		assertTrue(m.isEmpty());
		for (int i = 0; i < 100; i++) {
			assertSame(DEFAULT, m.put(i, Integer.valueOf(i)));
		}
		for (int i = 0; i < 100; i++) {
			assertFalse(m.int2ObjectEntrySet().remove(new AbstractInt2ObjectMap.BasicEntry<>(i + 1, Integer.valueOf(i))));
			assertFalse(m.int2ObjectEntrySet().remove(new AbstractInt2ObjectMap.BasicEntry<>(i, Integer.valueOf(i + 1))));
			assertTrue(m.containsKey(i));
			assertTrue(m.int2ObjectEntrySet().remove(new AbstractInt2ObjectMap.BasicEntry<>(i, Integer.valueOf(i))));
			assertFalse(m.containsKey(i));
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRemoveObject() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);
		assertTrue(m.containsKey(ONE));

		assertFalse(m.remove(TWO, ONE));
		assertFalse(m.remove(TWO, TWO));
		assertFalse(m.remove(ONE, TWO));

		assertTrue(m.containsKey(ONE));

		assertTrue(m.remove(ONE, ONE));
		assertFalse(m.containsKey(ONE));

		assertFalse(m.remove(ONE, ONE));
		assertFalse(m.remove(ONE, MINUS_ONE));

		assertSame(DEFAULT, m.get(ONE));
		assertFalse(m.containsKey(ONE));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = ClassCastException.class)
	public void testRemoveObjectInvalidKey() {
		m.put(1, ONE);
		m.remove(Long.valueOf(1), ONE);
	}

	@Test
	public void testRemovePrimitive() {
		m.defaultReturnValue(DEFAULT);

		m.put(1, ONE);
		assertTrue(m.containsKey(1));

		assertFalse(m.remove(2, ONE));
		assertFalse(m.remove(2, TWO));
		assertFalse(m.remove(1, TWO));

		assertTrue(m.containsKey(1));

		assertTrue(m.remove(1, ONE));
		assertFalse(m.containsKey(1));

		assertFalse(m.remove(1, ONE));
		assertFalse(m.remove(1, DEFAULT));

		assertSame(DEFAULT, m.get(1));
		assertFalse(m.containsKey(1));
	}

	@Test
	public void testRemoveWithValue() {
		m.defaultReturnValue(DEFAULT);

		assertFalse(m.remove(0, ZERO));
		m.put(0, ONE);
		assertFalse(m.remove(0, ZERO));
		assertTrue(m.containsKey(0));
		m.put(0, ZERO);
		assertTrue(m.remove(0, ZERO));
		assertFalse(m.containsKey(0));

		assertFalse(m.remove(1, ZERO));
		m.put(1, ONE);
		assertFalse(m.remove(1, ZERO));
		assertTrue(m.containsKey(1));
		m.put(1, ZERO);
		assertTrue(m.remove(1, ZERO));
		assertFalse(m.containsKey(1));
	}

	@Test
	public void testRemoveZero() {
		m.defaultReturnValue(DEFAULT);
		for (int i = -1; i <= 1; i++) {
			assertSame(DEFAULT, m.put(i, Integer.valueOf(i)));
		}
		assertEquals(ZERO, m.remove(0));
		final IntIterator iterator = m.keySet().iterator();
		final IntOpenHashSet z = new IntOpenHashSet();
		z.add(iterator.nextInt());
		z.add(iterator.nextInt());
		assertFalse(iterator.hasNext());
		assertEquals(new IntOpenHashSet(new int[] {-1, 1}), z);
	}

	@Test
	public void testRemoveZeroKeySet() {
		assumeTrue(capabilities.contains(Capability.KEY_SET_MODIFY));

		m.defaultReturnValue(DEFAULT);
		for (int i = -1; i <= 1; i++) {
			assertSame(DEFAULT, m.put(i, Integer.valueOf(i)));
		}
		IntIterator iterator = m.keySet().iterator();
		boolean removed = false;
		while (iterator.hasNext()) {
			if (iterator.nextInt() == 0) {
				assertFalse(removed);
				iterator.remove();
				removed = true;
			}
		}
		assertTrue(removed);

		assertFalse(m.containsKey(0));
		assertSame(DEFAULT, m.get(0));

		assertEquals(2, m.size());
		assertEquals(2, m.keySet().size());
		iterator = m.keySet().iterator();
		final int[] content = new int[2];
		content[0] = iterator.nextInt();
		content[1] = iterator.nextInt();
		assertFalse(iterator.hasNext());
		Arrays.sort(content);
		assertArrayEquals(new int[] {-1, 1}, content);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testReplaceBinaryObject() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		assertSame(DEFAULT, m.replace(TWO, ONE));
		assertSame(DEFAULT, m.replace(TWO, TWO));
		assertFalse(m.containsKey(TWO));

		assertEquals(ONE, m.replace(ONE, TWO));
		assertEquals(TWO, m.replace(ONE, TWO));
		assertEquals(TWO, m.replace(ONE, ONE));
		assertEquals(ONE, m.get(ONE));
		assertTrue(m.containsKey(ONE));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testReplaceBinaryObjectNullKey() {
		m.replace(null, ONE);
	}

	@SuppressWarnings("deprecation")
	public void testReplaceBinaryObjectNullValue() {
		m.put(1, ONE);
		m.replace(ONE, null);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testReplaceBinaryObjectNullValueMissingKey() {
		m.defaultReturnValue(DEFAULT);
		assertSame(DEFAULT, m.replace(ONE, null));
	}

	@Test
	public void testReplaceBinaryPrimitive() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		assertSame(DEFAULT, m.replace(2, ONE));
		assertSame(DEFAULT, m.replace(2, TWO));
		assertFalse(m.containsKey(2));

		assertEquals(ONE, m.replace(1, TWO));
		assertEquals(TWO, m.replace(1, TWO));
		assertEquals(TWO, m.replace(1, ONE));
		assertEquals(ONE, m.get(1));
		assertTrue(m.containsKey(1));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testReplaceTernaryObject() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		assertFalse(m.replace(TWO, ONE, ONE));
		assertFalse(m.replace(TWO, TWO, ONE));
		assertFalse(m.replace(ONE, TWO, ONE));
		assertEquals(ONE, m.get(ONE));

		assertTrue(m.replace(ONE, ONE, ONE));
		assertTrue(m.replace(ONE, ONE, TWO));
		assertFalse(m.replace(ONE, ONE, TWO));

		assertTrue(m.replace(ONE, TWO, MINUS_ONE));
		assertEquals(MINUS_ONE, m.get(ONE));
		assertTrue(m.containsKey(ONE));
	}

	@SuppressWarnings("deprecation")
	@Test(expected = NullPointerException.class)
	public void testReplaceTernaryObjectNullKey() {
		m.replace(null, ONE, ONE);
	}

	@SuppressWarnings("deprecation")
	public void testReplaceTernaryObjectNullNewValue() {
		m.put(1, ONE);
		m.replace(ONE, ONE, null);
	}

	@SuppressWarnings("deprecation")
	public void testReplaceTernaryObjectNullNewValueMissingKey() {
		m.replace(ONE, ONE, null);
	}

	@SuppressWarnings("deprecation")
	public void testReplaceTernaryObjectNullOldValueMissingKey() {
		m.replace(ONE, null, ONE);
	}

	@Test
	public void testReplaceTernaryPrimitive() {
		m.defaultReturnValue(DEFAULT);
		m.put(1, ONE);

		assertFalse(m.replace(2, ONE, ONE));
		assertFalse(m.replace(2, TWO, ONE));
		assertFalse(m.replace(1, TWO, ONE));
		assertEquals(ONE, m.get(1));

		assertTrue(m.replace(1, ONE, ONE));
		assertTrue(m.replace(1, ONE, TWO));
		assertFalse(m.replace(1, ONE, TWO));

		assertTrue(m.replace(1, TWO, DEFAULT));
		assertSame(DEFAULT, m.get(1));
		assertTrue(m.containsKey(1));
	}

	@Test
	public void testSerialisation() throws IOException, ClassNotFoundException {
		assumeTrue(m instanceof Serializable);

		final ByteArrayOutputStream store = new ByteArrayOutputStream();
		try (ObjectOutput oos = new ObjectOutputStream(store)) {
			oos.writeObject(m);
		}
		assertEquals(m, BinIO.loadObject(new ByteArrayInputStream(store.toByteArray())));

		m.put(0, ONE);
		m.put(1, TWO);

		store.reset();
		try (ObjectOutput oos = new ObjectOutputStream(store)) {
			oos.writeObject(m);
		}
		assertEquals(m, BinIO.loadObject(new ByteArrayInputStream(store.toByteArray())));
	}

	@Test
	public void testSizeAndIsEmpty() {
		assertEquals(0, m.size());
		assertTrue(m.isEmpty());
		for (int i = 0; i < 100; i++) {
			assertEquals(i, m.size());
			assertEquals(m.defaultReturnValue(), m.put(i, Integer.valueOf(i)));
			assertFalse(m.isEmpty());
		}
		for (int i = 0; i < 100; i++) {
			assertEquals(100, m.size());
			assertEquals(Integer.valueOf(i), m.put(i, Integer.valueOf(i)));
		}
		for (int i = 99; i >= 0; i--) {
			assertEquals(Integer.valueOf(i), m.remove(i));
			assertEquals(i, m.size());
		}
		assertEquals(0, m.size());
		assertTrue(m.isEmpty());
		for (int i = 0; i < 100; i++) {
			m.put(i, Integer.valueOf(i));
		}
		m.clear();
		assertEquals(0, m.size());
		assertTrue(m.isEmpty());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testValueSetIteratorForEach() {
		for (int i = 0; i < 100; i++) {
			m.put(i, Integer.valueOf(i));
		}
		final IntOpenHashSet s = new IntOpenHashSet();
		m.values().forEach(s::add);
		assertEquals(s, new IntOpenHashSet(m.values()));
	}

	public enum Capability {
		KEY_SET_MODIFY, ITERATOR_MODIFY
	}
}