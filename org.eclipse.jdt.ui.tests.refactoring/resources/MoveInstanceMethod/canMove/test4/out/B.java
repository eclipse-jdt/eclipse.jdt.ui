package p2;

import p1.A;

public class B {
	public void mB1() {}
	
	public void mB2() {}

	public void mA1(A a) {
		mB1();
		a.mA2();
		mB2();
		System.out.println(a);
	}
}