package p;

class A<E> {
	void m(E e) {}
}

class Sub<E> extends A<E> {
	void m(E e) {}
}