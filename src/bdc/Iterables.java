package bdc;

import java.util.Iterator;

public class Iterables {

    public static <T> T getOnlyElement(final Iterable<T> iterable) {
	final Iterator<T> iterator = iterable.iterator();
	if (!iterator.hasNext()) {
	    throw new IllegalStateException();
	}
	final T t = iterator.next();
	if (iterator.hasNext()) {
	    throw new IllegalStateException();
	}
	return t;
    }

    public static void assertEmpty(final Iterable<?> iterable) {
	if (iterable.iterator().hasNext()) {
	    throw new IllegalStateException("Expected empty: " + iterable);
	}
    }

    public static boolean isEmpty(final Iterable<?> iterable) {
	return iterable.iterator().hasNext();
    }
}
