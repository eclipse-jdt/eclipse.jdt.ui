package p;

import java.util.Stack;

public class A {
}

public class B<T extends Integer> extends Stack<T> {
	void foo(T t) {
	}
}
