// Move mA1 to field fB, do not inline delegator
package p1;

import p2.B;

public class A {
	public B fB;
	
	public void mA1() {
		fB.mA1(this);
	}
	
	public void mA2() {}
}