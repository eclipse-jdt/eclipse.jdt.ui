package p;

import java.util.Collection;
import java.util.LinkedList;

public class Foo {
	public static <A extends Comparable<A>> A foo(Collection<A> xs) {
		return null;
	}

	{
		LinkedList<Long> list = new LinkedList<Long>();
		LinkedList<String> list2 = new LinkedList<String>();
		
		foo(list);
		foo(list2); // <-- invoke here
	}
}
