package p;

class A<E> {
	<T> void m(T t, E e) {}
}

class Sub<Q> extends A<Q> {
	void m(Integer i, Q q) {}
}
