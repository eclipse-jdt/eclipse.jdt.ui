package p;

class A<E> {
	/**
	 * @deprecated Use {@link #m(E,Integer)} instead
	 */
	void m(Integer i, E e) {
		m(e, i);
	}

	void m(E e, Integer integer) {}
}

class Sub<Q> extends A<Q> {
	/**
	 * @deprecated Use {@link #m(Q,Integer)} instead
	 */
	void m(Integer i, Q q) {
		m(q, i);
	}

	void m(Q q, Integer integer) {
		super.m(q, integer);
	}
}
