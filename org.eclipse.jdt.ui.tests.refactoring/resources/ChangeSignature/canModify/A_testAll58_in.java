package p;

abstract class A {
	/**
	 * @param a an a
	 * @param b bee
	 * @return the number
	 * @see #m(int, String[][][])
	 */
	public abstract int m(int a, String[] b[][]);
}
class B extends A {
	public int m(int number, String[] b[][]) {
		return number + 0;
	}
}
class C extends B {
	/**
	 * @param a an a
	 * @param b bee
	 */
	public int m(int a, String[] strings[][]) {
		return a + 17;
	}
}
