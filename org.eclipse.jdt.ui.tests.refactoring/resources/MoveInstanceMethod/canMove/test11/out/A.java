package p1;

import p2.B;

public class A {
	public int fInt;
	public B fB;
	public String fString;
	public boolean fBool;
	
	/**
	 * This is a comment
	 * @param j
	 * @param foo
	 * @param bar
	 */
	public void mA1(float j, int foo, String bar) {
		fB.mA1(this, j, foo, bar);
	}
	
	public void mA2() {}
}