package p3;

import p1.A;
import p2.B;

class C {
    C() {
		getB().mA1(getA());
	}

	A getA() {
		return null;
	}

	B getB() {
		return null;
	}
}