package utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

public class RandomQueueTest {
	
	private static Random rand;
	
	@BeforeClass
	public static void setup() {
		rand = new Random();
	}
	
	@Test
	public void emptyConstructorTest() {
		RandomQueue<Integer> a = new RandomQueue<Integer>();
		assertTrue(a.isEmpty());
	}
	
	@Test
	public void collectionConstructorTest() {
		Collection<Integer> numbers = randomNumbers(10);
		RandomQueue<Integer> a = new RandomQueue<Integer>(numbers);
		for (Integer i : numbers)
			assertTrue(a.contains(i));
	}
	
	@Test
	public void randomSeedTest() {
		long seed = rand.nextLong();
		Collection<Integer> numbers = randomNumbers(10);
		RandomQueue<Integer> a = new RandomQueue<Integer>(numbers, new Random(seed));
		RandomQueue<Integer> b = new RandomQueue<Integer>(numbers, new Random(seed));
		
		Iterator<Integer> ita = a.iterator();
		Iterator<Integer> itb = b.iterator();
		while (ita.hasNext())
			assertTrue(ita.next().equals(itb.next()));
		
		RandomQueue<Integer> c = new RandomQueue<Integer>(new Random(seed));
		c.addAll(numbers);
		
		itb = b.iterator();
		Iterator<Integer> itc = c.iterator();
		while (itc.hasNext())
			assertTrue(itb.next().equals(itc.next()));
	}
	
	@Test
	public void clearTest() {
		RandomQueue<Integer> a = new RandomQueue<Integer>(randomNumbers(10));
		assertFalse(a.isEmpty());
		a.clear();
		assertTrue(a.isEmpty());
	}
	
	@Test
	public void removeTest() {
		RandomQueue<Integer> a = new RandomQueue<Integer>(Arrays.asList(new Integer[] {1, 2, 3, 4, 5}));
		assertTrue(a.contains(4));
		a.remove(4);
		assertFalse(a.contains(4));
		assertTrue(a.containsAll(Arrays.asList(new Integer[] {1, 2, 3, 5})));
	}
	
	@Test
	public void permutationTest() {
		// Note, this test could fail due to inconceivably small random chance ~1/1000! (that's factorial,
		// not an exclamation)
		Collection<Integer> numbers = randomNumbers(1000);
		RandomQueue<Integer> a = new RandomQueue<Integer>(numbers);
		
		Iterator<Integer> ita = a.iterator();
		Iterator<Integer> itb = numbers.iterator();
		while (ita.hasNext())
			if (!ita.next().equals(itb.next()))
				return;
		fail();
	}
	
	@Test
	public void offerPermutationTest() {
		// Note, this test could fail due to inconceivably small random chance as well. 1/1000^1000
		for (int i = 0; i < 1000; i++) {
			RandomQueue<Integer> a = new RandomQueue<Integer>(randomNumbers(1000));
			a.add(0);
			if (a.peek() != 0)
				return;
		}
		fail();
	}
	
	@Test
	public void offerNonpermutationTest() {
		// Note, this test could fail due to inconceivably small random chance as well. 1/2^10000 ~ 1/1000^1000
		for (int i = 0; i < 10000; i++) {
			RandomQueue<Integer> a = new RandomQueue<Integer>(randomNumbers(1));
			a.add(0);
			if (a.peek() == 0)
				return;
		}
		fail();
	}
	
	public static Collection<Integer> randomNumbers(int size) {
		Collection<Integer> numbers = new ArrayList<Integer>(size);
		for (int i = 0; i < size; i++)
			numbers.add(rand.nextInt());
		return numbers;
	}

}
