package p3;

import p1.A;
import p2.B;

class C {
    C() {
    	A a = getA();
		getB().mA1(a);
	}

	A getA() {
		return null;
	}

	B getB() {
		return null;
	}
}