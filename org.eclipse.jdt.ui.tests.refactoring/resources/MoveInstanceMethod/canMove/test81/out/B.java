package p2;

import p2.b.T;

public class B {
	@T
	public void mB1() {}
	
	public void mB2() {}

	/**
	 * This is a comment
	 * @param j a float
	 * @param foo a foo
	 * @param bar a bar
	 */
	public p1.a.T mA1(float j, int foo, String bar) {
		System.out.println(bar + j);
		return new p1.a.T();
	}
}