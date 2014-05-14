package p2;

public interface B {

	/**
	 * This is a comment
	 * @param a TODO
	 * @param j
	 * @param foo
	 * @param bar
	 */
	public default void mA1(A a, float j, int foo, String bar) {
		System.out.println(bar + j + a);
		String z= A.fString + A.fBool;
	}
	
}