package p1;

import p2.B;

public class A {
	public void mA1(float j, B b, int foo, String bar) {
		b.mA1(j, this, foo, bar);
	}
	
	public void mA2() {}
}