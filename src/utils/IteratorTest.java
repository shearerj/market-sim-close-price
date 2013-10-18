package utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

public class IteratorTest {
	
	
	@Test
	public void emptyConstructorTest() {
		Iterator<E> a = new Iterator<E>();
		assertTrue(a.isEmpty());
		bool testHasNext;
		testHasNext=Iterator.hasNext();
		Assert(testHasNext==0);

		//if you attempt to use next()
		//you should recieve a NoSuchElementException

		//if you use remove()
		//you should recieve an UnsupportedOperationException
	}
	
	}