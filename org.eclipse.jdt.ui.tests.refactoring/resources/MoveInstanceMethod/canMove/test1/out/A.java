// Move mA1 to parameter b, inline delegator
package p1;

import p2.B;

public class A {

	/**
	 * @param b
	 */
	public void mA1(B b) {
		b.mA1(this);
	}
	
	public void mA2() {}
}