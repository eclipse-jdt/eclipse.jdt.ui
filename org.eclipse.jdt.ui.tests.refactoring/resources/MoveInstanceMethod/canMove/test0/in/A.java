// Move mA1 to parameter b, do not inline delegator
package p1;

import p2.B;

public class A {

	/**
	 * @param b
	 */
	public void mA1(B b) {
		b.mB1();
		mA2(); //test comment
		b.mB2();
		System.out.println(this);
	}
	
	public void mA2() {}
}