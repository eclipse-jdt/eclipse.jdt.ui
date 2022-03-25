//can rename A.m(T) to k
package p;
interface I {
	
}
class A<T, K extends I> {
	void k(T t) {
	}
	void k(K t) {
	}
}
class B extends A<I, I> {
	@Override
	void k(I i) {
	}
}