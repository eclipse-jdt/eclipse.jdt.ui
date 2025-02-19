package p2;

import p2.b.T;

public class B {
	@T
	public void mB1() {}
	
	public void mB2() {}

	/**
	 * This is a comment
	 */
	public static p1.a.T m() {
		System.out.println("abc");
		return new p1.a.T();
	}
}