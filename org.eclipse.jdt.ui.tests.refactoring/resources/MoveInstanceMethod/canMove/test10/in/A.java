package p1;

import p2.B;

public class A {

	/**
	 * This is a comment
	 * @param j
	 * @param b
	 * @param foo
	 * @param bar
	 */
	public void mA1(float j, B b, int foo, String bar) {
		b.mB1();
		System.out.println(bar + j);
	}
	
	public void mA2() {}
}