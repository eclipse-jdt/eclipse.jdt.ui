package p;

import java.util.HashMap;
import java.util.List;

class A<E> {
	<T> void m(E e, T t, List<HashMap> maps) {}
}

class Sub<Q> extends A<Q> {
	void m(Q q, Integer i, List<HashMap> maps) {}
}
