package bdc;

import java.util.ArrayList;
import java.util.List;

public class Chain<T> {

    public final T head;
    public final Chain<T> tail;

    public Chain(final T head, final Chain<T> tail) {
	this.head = head;
	this.tail = tail;
    }

    public static <T> Chain<T> append(final T head, final Chain<T> tail) {
	return new Chain<>(head, tail);
    }

    public static <T> List<T> toList(final Chain<T> stack) {
	final ArrayList<T> list = new ArrayList<>();
	Chain<T> iterator = stack;
	while (iterator != null) {
	    list.add(iterator.head);
	    iterator = iterator.tail;
	}
	return list;
    }

}
