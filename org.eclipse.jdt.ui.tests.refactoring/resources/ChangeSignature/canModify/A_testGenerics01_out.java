package p;

class A<E> {
	void m(E e, Integer integer) {}
}

class Sub<Q> extends A<Q> {
	void m(Q q, Integer integer) {
		super.m(q, integer);
	}
}
