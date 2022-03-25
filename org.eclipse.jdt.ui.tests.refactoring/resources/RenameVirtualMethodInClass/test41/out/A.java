//can rename A.m to k
package p;
interface I {
	
}
class B<T, K extends I> {
	void k(T t) {
	}
	void k(K t) {
	}
}
class A extends B<I, I> {
	@Override
	void k(I i) {
	}
}