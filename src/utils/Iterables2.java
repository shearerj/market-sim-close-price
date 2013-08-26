package utils;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

public class Iterables2 {
	
	public static <E> Iterable<E> randomOrder(Iterable<E> elements) {
		List<E> list = Lists.newArrayList(elements);
		Collections.shuffle(list);
		return list;
	}

	public static <E> Iterable<E> randomOrder(Iterable<E> elements, Random rand) {
		List<E> list = Lists.newArrayList(elements);
		Collections.shuffle(list, rand);
		return list;
	}
}
