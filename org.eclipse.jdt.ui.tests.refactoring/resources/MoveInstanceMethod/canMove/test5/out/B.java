package p2;

import p1.A;

public class B {

	public void mA1(A a) {
		System.out.println(A.fgHello);
		A.talk(this);
		System.out.println(a);
	}
}