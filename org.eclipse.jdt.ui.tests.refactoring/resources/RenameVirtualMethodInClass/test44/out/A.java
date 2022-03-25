//can rename A.m(E) to k
package p;

class A1 extends C3 {
	class A2 extends A<Object> implements I1<Object> {
		public void k(Object o) {
		}
	}
}

class C3 {
	class A<E> {
		public void k(E e) {
		}
	}
}

interface I1<T> {
	void k(T t);
}
