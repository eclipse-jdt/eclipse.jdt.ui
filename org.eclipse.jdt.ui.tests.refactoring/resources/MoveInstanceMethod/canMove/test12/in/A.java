package p1;

import p2.B;

public class A {
	public B fB;

	/**
	 * This is a comment
	 * @param j a float
	 * @param foo a foo
	 * @param bar a bar
	 */
	public void mA1(float j, int foo, String bar) {
		fB.mB1();
		System.out.println(bar + j);
	}
	
	public void mA2() {}
}