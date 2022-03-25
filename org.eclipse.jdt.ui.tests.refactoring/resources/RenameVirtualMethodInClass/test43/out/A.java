//can rename A.m(Object) to k
package p;

class A1 extends C3 {
	class A extends C<Object> implements I1<Object> {
		public void k(Object o) {
		}
	}
}

class C3 {
	class C<E> {
		public void k(E e) {
		}
	}
}

interface I1<T> {
	void k(T t);
}