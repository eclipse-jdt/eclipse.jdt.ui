package p1;

import p2.B;

public class A {
	public B fB;
	
	public void mA1(float j, int foo, String bar) {
		fB.mB1();
		System.out.println(bar + j);
	}
	
	public void mA2() {}
}