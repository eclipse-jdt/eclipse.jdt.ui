package p1;

import p2.B;
import p3.N1;
import p3.N1.N2.N3;

public class A {
	public void m(B b) {
		N3 anN3= new N1().new N2().new N3();
	}
}