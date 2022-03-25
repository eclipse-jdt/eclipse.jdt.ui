//can rename A.m to k
package p;
interface I {
	
}
class B<T, K extends I> {
	void m(T t) {
	}
	void m(K t) {
	}
}
class A extends B<I, I> {
	@Override
	void m(I i) {
	}
}