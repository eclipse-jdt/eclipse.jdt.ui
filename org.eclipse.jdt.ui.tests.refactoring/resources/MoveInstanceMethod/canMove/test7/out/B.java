package p2;

import p3.N1;
import p3.N1.N2;
import p3.N1.N2.N3;

public class B {

	public void m() {
		N3 anN3= new N1().new N2().new N3();
	}
}