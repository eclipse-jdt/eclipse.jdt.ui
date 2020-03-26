package r;

import p1.A;

public class B {
	public void m() {
	}

	public void moveMe() {
		m();
		A.staticMethod();
	}
}
