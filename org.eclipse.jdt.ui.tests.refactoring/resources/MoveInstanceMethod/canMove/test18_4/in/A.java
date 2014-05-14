package p1;

import p2.B;

public interface A {
	public int fInt= 10;
	public B fB= new B();
	public String fString= "something";
	public boolean fBool= true;
	
	/**
	 * This is a comment
	 * @param j
	 * @param foo
	 * @param bar
	 */
	public default void mA1(float j, int foo, String bar) {
		System.out.println(bar + j + this);
		String z= fString + fBool;
	}
	
	public void mA2();
}