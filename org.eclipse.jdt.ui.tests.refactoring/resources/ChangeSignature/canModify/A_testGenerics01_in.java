package p;

class A<E> {
	void m(Integer i, E e) {}
}

class Sub<Q> extends A<Q> {
	void m(Integer i, Q q) {
		super.m(i, q);
	}
}
