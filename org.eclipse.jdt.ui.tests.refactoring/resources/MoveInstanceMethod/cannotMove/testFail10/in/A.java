// Move mA1 to parameter b, do not inline delegator
package p1;

import p2.B;

public class A {
	String s= "";
	public void mA1() {
		B b= null;
		b.mB1();
		mA2(); //test comment
		b.mB2();
		System.out.println(this);
		System.out.println(s);
	}
	
	public void mA2() {}
}