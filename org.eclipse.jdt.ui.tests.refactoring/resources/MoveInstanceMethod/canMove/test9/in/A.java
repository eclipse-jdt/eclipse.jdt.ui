package p1;

import p2.B;

public class A {
	public void mA1(float j, B b, int foo, String bar) {
		b.mB1();
		mA2();
		b.mB2();
		System.out.println(this);
		System.out.println(bar + j);
	}
	
	public void mA2() {}
}