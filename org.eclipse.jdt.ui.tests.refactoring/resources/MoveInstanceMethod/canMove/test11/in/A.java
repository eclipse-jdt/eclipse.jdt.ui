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
		fB.mB1();
		System.out.println(bar + j + this);
		String z= fString + fBool;
		fInt++;
	}
	
	public void mA2() {}
}