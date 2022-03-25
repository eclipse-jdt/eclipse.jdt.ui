//can rename A.m(T) to k
package p;
interface I {
	
}
class A<T, K extends I> {
	void m(T t) {
	}
	void m(K t) {
	}
}
class B extends A<I, I> {
	@Override
	void m(I i) {
	}
}