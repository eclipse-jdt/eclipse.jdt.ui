// Move mA1 to field fB, unqualified static member references are qualified
package p1;

import p2.B;

public class A {
	public static String fgHello= "Hello from A!";
	
	public B fB;
	
	public static void talk(B b) {
		System.out.println("How are you?");
	}
	
	public void mA1() {
		fB.mA1(this);
	}
}