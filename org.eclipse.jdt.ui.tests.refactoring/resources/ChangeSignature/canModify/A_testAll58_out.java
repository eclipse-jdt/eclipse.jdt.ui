package p;

abstract class A {
	/**
	 * @param bbb bee
	 * @param abb an a
	 * @return the number
	 * @see #m(String[][][], int)
	 */
	public abstract int m(String[] bbb[][], int abb);
}
class B extends A {
	public int m(String[] bbb[][], int number) {
		return number + 0;
	}
}
class C extends B {
	/**
	 * @param bbb bee
	 * @param abb an a
	 */
	public int m(String[] strings[][], int abb) {
		return abb + 17;
	}
}
