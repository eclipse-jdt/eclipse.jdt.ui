package p3;

import p1.A;
import p2.B;

class C {
    C() {
		getA().mA1(getB());
	}

	A getA() {
		return null;
	}

	B getB() {
		return null;
	}
}