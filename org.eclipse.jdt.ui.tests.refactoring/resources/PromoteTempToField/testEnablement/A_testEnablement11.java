//6, 21, 6, 27
package p;

class A {
	<U extends A> void k(final U u) {
		Callable<A> target= new Callable<A>() {
			public A call() throws Exception {
				return u;
			}
		};
	}
}

interface Callable<E> {
	E call() throws Exception;
}