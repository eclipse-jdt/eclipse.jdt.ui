//6, 16, 6, 32
package p;

class A<E> {
	private static final A<Integer> INT= new A<Integer>();

	static A<Integer> getInt() {
		return A.INT;
	}
}