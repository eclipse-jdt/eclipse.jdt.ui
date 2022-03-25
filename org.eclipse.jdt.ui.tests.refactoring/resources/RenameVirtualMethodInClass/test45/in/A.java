//can rename A.m(T) to k
package p;

class A1 extends C3 {
	class A2 extends C<Object> implements A<Object> {
		public void m(Object o) {
		}
	}
}

class C3 {
	class C<E> {
		public void m(E e) {
		}
	}
}

interface A<T> {
	void m(T t);
}