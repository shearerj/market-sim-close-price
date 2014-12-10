package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;


public class Iterators2 {
	
	private static final Rand rand = Rand.create();

	public static Iterator<Integer> counter() {
		return new AbstractSequentialIterator<Integer>(1) {
			@Override protected Integer computeNext(Integer previous) { return previous + 1; }
		};
	}

	public static Iterator<Double> exponentials(final double rate, final Rand rand) {
		if (rate == 0)
			return ImmutableList.<Double> of().iterator();
		return new AbstractIterator<Double>() {
			@Override protected Double computeNext() {
				return rand.nextExponential(rate);
			}
		};
	}
	
	public static Iterator<String> lineIterator(Reader reader) {
		final BufferedReader lineReader = new BufferedReader(reader);
		try {
			return new UnmodifiableIterator<String>() {
				String line = lineReader.readLine();

				@Override
				public boolean hasNext() {
					return line != null;
				}

				@Override
				public String next() {
					String lastLine = line;
					try {
						line = lineReader.readLine();
					} catch (IOException e) {
						line = null;
					}
					return lastLine;
				}
			};
		} catch (IOException e) {
			return ImmutableList.<String> of().iterator();
		}
	}
	
	public static <E> Iterator<E> shuffle(Iterator<E> iter, Random rand) {
		List<E> copy = Lists.newArrayList(iter);
		Collections.shuffle(copy, rand);
		return Collections.unmodifiableList(copy).iterator();
	}
	
	public static <E> Iterator<E> shuffle(Iterator<E> iter) {
		return shuffle(iter, rand);
	}
	
}
